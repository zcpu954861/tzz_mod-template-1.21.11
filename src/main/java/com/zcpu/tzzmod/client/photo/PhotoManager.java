package com.zcpu.tzzmod.client.photo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.client.phone.ui.state.PhoneSettingsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import javax.imageio.ImageIO;
import javax.imageio.IIOImage;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages photo capture, storage, loading, and metadata for the camera/gallery system.
 * Photos are stored in .minecraft/tzzphotos/local with embedded metadata.
 */
public final class PhotoManager {
    private static final String PHOTOS_DIR = "tzzphotos";
    private static final String LOCAL_PHOTOS_DIR = "local";
    private static final String SERVER_PHOTOS_DIR = "serverphotos";
    private static final String ONLINE_TEMP_DIR = "onlinetemp";
    private static final String TEMP_PHOTOS_DIR = "temp";
    private static final String METADATA_KEY = "tzzModPhotoMeta";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int CACHE_STEP = 64;
    private static final int LOW_MIN_THUMB_EDGE = 128;
    private static final int LOW_MAX_THUMB_EDGE = 320;
    private static final int HIGH_MIN_THUMB_EDGE = 192;
    private static final int HIGH_MAX_THUMB_EDGE = 640;
    private static final int LOW_MIN_VIEWER_EDGE = 256;
    private static final int LOW_MAX_VIEWER_EDGE = 1600;
    private static final int HIGH_MIN_VIEWER_EDGE = 512;
    private static final int HIGH_MAX_VIEWER_EDGE = 1920;
    private static final long TARGET_UPLOAD_PIXELS = 1920L * 1080L;
    private static final long HIGH_QUALITY_VIEWER_PIXELS = 1920L * 1080L;

    // Texture cache: path -> (Identifier, NativeImageBackedTexture)
    private static final Map<String, Identifier> textureCache = new ConcurrentHashMap<>();
    private static final Map<String, NativeImageBackedTexture> nativeTextureCache = new ConcurrentHashMap<>();
    private static final Map<String, int[]> dimensionCache = new ConcurrentHashMap<>();
    private static final Map<String, String> fileHashCache = new ConcurrentHashMap<>();

    // Cached photo list for the local gallery directory
    private static volatile List<PhotoEntry> cachedPhotos = null;
    private static volatile Map<String, Path> cachedPhotoHashIndex = null;
    private static volatile long localPhotoRevision = 0L;

    private PhotoManager() {}

    /**
     * Get the unique identifier for the current world/server.
     */
    public static String getWorldId() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return "unknown";

        // Multiplayer: use server address
        var currentServerEntry = client.getCurrentServerEntry();
        if (currentServerEntry != null) {
            String addr = currentServerEntry.address;
            return sanitizeFileName(addr);
        }

        // Singleplayer: use world folder name
        var server = client.getServer();
        if (server != null) {
            Path worldRoot = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT);
            Path worldName = worldRoot.getFileName();
            if (worldName != null) {
                return sanitizeFileName(worldName.toString());
            }
        }

        return LOCAL_PHOTOS_DIR;
    }

    /**
     * Get the server IP for online temp cache directory.
     */
    public static String getServerIp() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            var currentServerEntry = client.getCurrentServerEntry();
            if (currentServerEntry != null) {
                return sanitizeFileName(currentServerEntry.address);
            }
        }
        return "local";
    }

    /**
     * Get the local photos directory used by the camera app.
     */
    public static Path getPhotosDir() {
        return getGameDir().resolve(PHOTOS_DIR).resolve(LOCAL_PHOTOS_DIR);
    }

    public static Path ensurePhotosDir() {
        Path photosDir = getPhotosDir();
        try {
            Files.createDirectories(photosDir);
            return photosDir;
        } catch (IOException e) {
            Tzz_mod.LOGGER.error("Failed to create photo directory {}", photosDir, e);
            return null;
        }
    }

    /**
     * Get the server photos directory (for online gallery storage on the server side).
     */
    public static Path getServerPhotosDir() {
        return getGameDir().resolve(PHOTOS_DIR).resolve(SERVER_PHOTOS_DIR);
    }

    /**
     * Get the online temp cache directory for a specific server.
     */
    public static Path getOnlineTempDir() {
        return getGameDir().resolve(PHOTOS_DIR).resolve(ONLINE_TEMP_DIR).resolve(getServerIp());
    }

    public static Path getTempPhotosDir() {
        return getGameDir().resolve(PHOTOS_DIR).resolve(TEMP_PHOTOS_DIR);
    }

    public static long getLocalPhotoRevision() {
        return localPhotoRevision;
    }

    public static Path findLocalPhotoByHash(String photoHash) {
        if (photoHash == null || photoHash.isBlank()) {
            return null;
        }

        if (cachedPhotoHashIndex == null) {
            loadPhotos();
            if (cachedPhotoHashIndex == null && cachedPhotos != null) {
                cachedPhotoHashIndex = buildPhotoHashIndex(cachedPhotos);
            }
        }

        Path matchedPath = cachedPhotoHashIndex != null ? cachedPhotoHashIndex.get(photoHash) : null;
        return matchedPath != null && Files.exists(matchedPath) ? matchedPath : null;
    }

    public static CachedImage getThumbnailImage(Path photoPath, int displaySize) {
        boolean performanceMode = isGalleryPerformanceModeEnabled();
        int minEdge = performanceMode ? LOW_MIN_THUMB_EDGE : HIGH_MIN_THUMB_EDGE;
        int maxEdge = performanceMode ? LOW_MAX_THUMB_EDGE : HIGH_MAX_THUMB_EDGE;
        int scaleFactor = performanceMode ? 2 : 4;
        int targetEdge = normalizeCacheDimension(Math.max(minEdge, displaySize * scaleFactor), minEdge, maxEdge);
        return getOrCreateCachedImage(photoPath, targetEdge, targetEdge, true);
    }

    public static CachedImage getViewerImage(Path photoPath, int maxWidth, int maxHeight) {
        if (photoPath == null || !Files.exists(photoPath)) {
            return new CachedImage(photoPath, 1, 1);
        }

        boolean performanceMode = isGalleryPerformanceModeEnabled();
        int targetWidth;
        int targetHeight;
        if (performanceMode) {
            targetWidth = normalizeCacheDimension(maxWidth, LOW_MIN_VIEWER_EDGE, LOW_MAX_VIEWER_EDGE);
            targetHeight = normalizeCacheDimension(maxHeight, LOW_MIN_VIEWER_EDGE, LOW_MAX_VIEWER_EDGE);
        } else {
            int requestedWidth = Math.max(HIGH_MIN_VIEWER_EDGE, maxWidth * 4);
            int requestedHeight = Math.max(HIGH_MIN_VIEWER_EDGE, maxHeight * 4);
            targetWidth = normalizeCacheDimension(requestedWidth, HIGH_MIN_VIEWER_EDGE, HIGH_MAX_VIEWER_EDGE);
            targetHeight = normalizeCacheDimension(requestedHeight, HIGH_MIN_VIEWER_EDGE, HIGH_MAX_VIEWER_EDGE);

            int[] sourceDims = getImageDimensions(photoPath);
            int[] desiredDims = computeScaledDimensions(sourceDims[0], sourceDims[1], targetWidth, targetHeight);
            if ((long) desiredDims[0] * desiredDims[1] > HIGH_QUALITY_VIEWER_PIXELS) {
                desiredDims = computeScaledDimensionsForMaxPixels(desiredDims[0], desiredDims[1], HIGH_QUALITY_VIEWER_PIXELS);
            }
            targetWidth = desiredDims[0];
            targetHeight = desiredDims[1];
        }

        return getOrCreateCachedImage(photoPath, targetWidth, targetHeight, true);
    }

    public static PreparedUploadPhoto preparePhotoForUpload(Path photoPath, PhotoMetadata metadata) throws IOException {
        int[] sourceDims = getImageDimensions(photoPath);
        Path preparedPath = photoPath;
        int preparedWidth = sourceDims[0];
        int preparedHeight = sourceDims[1];

        if ((long) preparedWidth * preparedHeight > TARGET_UPLOAD_PIXELS) {
            int[] uploadDims = computeScaledDimensionsForMaxPixels(preparedWidth, preparedHeight, TARGET_UPLOAD_PIXELS);
            CachedImage cachedImage = getOrCreateCachedImage(photoPath, uploadDims[0], uploadDims[1], true);
            preparedPath = cachedImage.path();
            preparedWidth = cachedImage.width();
            preparedHeight = cachedImage.height();
        }

        byte[] data = Files.readAllBytes(preparedPath);
        String photoId = computeFileHash(preparedPath);
        PhotoMetadata preparedMetadata = metadata == null
                ? new PhotoMetadata(PhotoMetadata.MOD_TAG, System.currentTimeMillis(), "", "", "", preparedWidth, preparedHeight)
                : new PhotoMetadata(
                        metadata.modTag(),
                        metadata.captureTimeMs(),
                        metadata.worldId(),
                        metadata.playerName(),
                        metadata.playerUuid(),
                        preparedWidth,
                        preparedHeight
                );

        return new PreparedUploadPhoto(preparedPath, data, photoId, preparedMetadata);
    }

    /**
     * Capture a screenshot without HUD and save it as a mod photo.
     * This must be called from the render thread AFTER the frame has been rendered without HUD.
     *
     * @param drawCameraFrame whether to draw the camera frame overlay on the saved image
     * @param frameColor the color to use for the camera frame lines
     * @return the saved file path, or null on failure
     */
    public static Path capturePhoto(boolean drawCameraFrame, int frameColor) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getFramebuffer() == null) return null;

        try {
            Path photosDir = ensurePhotosDir();
            if (photosDir == null) {
                return null;
            }

            // Generate filename with timestamp
            String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.now());
            String filename = "photo_" + timestamp + ".png";
            Path filePath = photosDir.resolve(filename);

            // Ensure unique filename
            int counter = 1;
            while (Files.exists(filePath)) {
                filename = "photo_" + timestamp + "_" + counter + ".png";
                filePath = photosDir.resolve(filename);
                counter++;
            }
                Path targetFilePath = filePath;

            String playerName = client.player != null ? client.player.getName().getString() : "Unknown";
            String playerUuid = client.player != null ? client.player.getUuidAsString() : "";
            String worldId = getWorldId();

            com.zcpu.tzzmod.client.photo.ScreenshotHelper.takeScreenshot(client.getFramebuffer(), screenshot ->
                    Util.getIoWorkerExecutor().execute(() -> saveCapturedPhoto(
                            screenshot,
                        targetFilePath,
                            drawCameraFrame,
                            frameColor,
                            worldId,
                            playerName,
                            playerUuid
                    ))
            );

                return targetFilePath;
        } catch (Exception e) {
            Tzz_mod.LOGGER.error("Failed to capture photo", e);
            return null;
        }
    }

    private static void saveCapturedPhoto(NativeImage screenshot, Path filePath, boolean drawCameraFrame,
                                          int frameColor, String worldId, String playerName, String playerUuid) {
        if (screenshot == null) {
            Tzz_mod.LOGGER.error("Failed to capture photo: screenshot callback returned null image");
            return;
        }

        try (screenshot) {
            int width = screenshot.getWidth();
            int height = screenshot.getHeight();

            if (drawCameraFrame) {
                CameraFrameRenderer.drawOnImage(screenshot, width, height, frameColor);
            }

            writePhotoWithMetadata(screenshot, filePath, width, height, worldId, playerName, playerUuid);
            markLocalPhotosChanged();
            Tzz_mod.LOGGER.info("Photo captured: {}", filePath);
        } catch (Exception e) {
            Tzz_mod.LOGGER.error("Failed to save captured photo to {}", filePath, e);
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Write a NativeImage as PNG with embedded mod metadata in a tEXt chunk.
     */
    private static void writePhotoWithMetadata(NativeImage image, Path filePath, int w, int h,
                                               String worldId, String playerName, String playerUuid) throws IOException {
        // First, write the NativeImage to a temp file, then read bytes
        byte[] pngBytes;
        Path tempFile = filePath.getParent().resolve(".tmp_" + System.nanoTime() + ".png");
        try {
            image.writeTo(tempFile);
            pngBytes = Files.readAllBytes(tempFile);
        } finally {
            Files.deleteIfExists(tempFile);
        }

        // Now read it as a BufferedImage to re-write with metadata
        BufferedImage buffered;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(pngBytes)) {
            buffered = ImageIO.read(bais);
        }

        if (buffered == null) {
            // Fallback: just write raw PNG without metadata
            Files.write(filePath, pngBytes);
            return;
        }

        // Build metadata JSON
        JsonObject meta = new JsonObject();
        meta.addProperty("modTag", PhotoMetadata.MOD_TAG);
        meta.addProperty("captureTimeMs", System.currentTimeMillis());
        meta.addProperty("worldId", worldId);
        meta.addProperty("playerName", playerName);
        meta.addProperty("playerUuid", playerUuid);
        meta.addProperty("imageWidth", w);
        meta.addProperty("imageHeight", h);

        // Write PNG with tEXt metadata
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
        if (!writers.hasNext()) {
            Files.write(filePath, pngBytes);
            return;
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(filePath.toFile())) {
            writer.setOutput(ios);

            IIOMetadata writeMeta = writer.getDefaultImageMetadata(
                    new javax.imageio.ImageTypeSpecifier(buffered), writer.getDefaultWriteParam());

            // Add tEXt entry
            String metaFormat = writeMeta.getNativeMetadataFormatName();
            IIOMetadataNode root = (IIOMetadataNode) writeMeta.getAsTree(metaFormat);
            IIOMetadataNode textEntry = new IIOMetadataNode("tEXtEntry");
            textEntry.setAttribute("keyword", METADATA_KEY);
            textEntry.setAttribute("value", meta.toString());
            IIOMetadataNode textNode = new IIOMetadataNode("tEXt");
            textNode.appendChild(textEntry);
            root.appendChild(textNode);
            writeMeta.mergeTree(metaFormat, root);

            writer.write(new IIOImage(buffered, null, writeMeta));
        } finally {
            writer.dispose();
        }
    }

    /**
     * Read mod metadata from a PNG file's tEXt chunk.
     * Returns null if the file doesn't have valid mod metadata (not a mod photo).
     */
    public static PhotoMetadata readMetadata(Path filePath) {
        try {
            Iterator<javax.imageio.ImageReader> readers = ImageIO.getImageReadersByFormatName("png");
            if (!readers.hasNext()) return null;

            javax.imageio.ImageReader reader = readers.next();
            try (javax.imageio.stream.ImageInputStream iis = ImageIO.createImageInputStream(filePath.toFile())) {
                reader.setInput(iis);
                IIOMetadata meta = reader.getImageMetadata(0);
                if (meta == null) return null;

                String[] formatNames = meta.getMetadataFormatNames();
                for (String format : formatNames) {
                    IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(format);
                    String json = findTextEntry(root, METADATA_KEY);
                    if (json != null) {
                        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                        String modTag = obj.has("modTag") ? obj.get("modTag").getAsString() : "";
                        if (!PhotoMetadata.MOD_TAG.equals(modTag)) return null;

                        return new PhotoMetadata(
                                modTag,
                                obj.has("captureTimeMs") ? obj.get("captureTimeMs").getAsLong() : 0,
                                obj.has("worldId") ? obj.get("worldId").getAsString() : "",
                                obj.has("playerName") ? obj.get("playerName").getAsString() : "",
                                obj.has("playerUuid") ? obj.get("playerUuid").getAsString() : "",
                                obj.has("imageWidth") ? obj.get("imageWidth").getAsInt() : 0,
                                obj.has("imageHeight") ? obj.get("imageHeight").getAsInt() : 0
                        );
                    }
                }
            } finally {
                reader.dispose();
            }
        } catch (Exception e) {
            Tzz_mod.LOGGER.debug("Failed to read photo metadata from {}: {}", filePath, e.getMessage());
        }
        return null;
    }

    private static String findTextEntry(IIOMetadataNode node, String keyword) {
        if ("tEXtEntry".equals(node.getNodeName())) {
            String kw = node.getAttribute("keyword");
            if (keyword.equals(kw)) {
                return node.getAttribute("value");
            }
        }
        org.w3c.dom.NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof IIOMetadataNode child) {
                String result = findTextEntry(child, keyword);
                if (result != null) return result;
            }
        }
        return null;
    }

    /**
     * Load all mod photos from the current world's photo directory.
     * Only returns photos with valid mod metadata.
     * Results are cached until invalidated.
     */
    public static List<PhotoEntry> loadPhotos() {
        if (cachedPhotos != null) {
            return cachedPhotos;
        }

        List<PhotoEntry> photos = new ArrayList<>();
        Path photosDir = ensurePhotosDir();
        if (photosDir == null) {
            cachedPhotos = photos;
            return photos;
        }
        if (!Files.isDirectory(photosDir)) {
            cachedPhotos = photos;
            return photos;
        }

        try (var stream = Files.list(photosDir)) {
            stream.filter(p -> p.toString().toLowerCase().endsWith(".png"))
                    .sorted(Comparator.<Path>comparingLong(p -> {
                        try { return Files.getLastModifiedTime(p).toMillis(); }
                        catch (IOException e) { return 0L; }
                    }).reversed())
                    .forEach(path -> {
                        PhotoMetadata meta = readMetadata(path);
                        if (meta != null) {
                            photos.add(new PhotoEntry(path, meta));
                        }
                    });
        } catch (IOException e) {
            Tzz_mod.LOGGER.error("Failed to list photos in {}", photosDir, e);
        }

        cachedPhotos = photos;
        cachedPhotoHashIndex = buildPhotoHashIndex(photos);
        return photos;
    }

    /**
     * Invalidate the photo cache, forcing a reload on next access.
     */
    public static void invalidateCache() {
        cachedPhotos = null;
        cachedPhotoHashIndex = null;
    }

    /**
     * Get or create a texture Identifier for a photo file.
     * Loads the image as a NativeImageBackedTexture and registers it.
     */
    public static Identifier getOrLoadTexture(Path photoPath) {
        String key = photoPath.toAbsolutePath().toString();
        Identifier cached = textureCache.get(key);
        if (cached != null) return cached;

        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) return null;

            byte[] bytes = Files.readAllBytes(photoPath);
            NativeImage nativeImage = NativeImage.read(bytes);
            cacheDimensions(photoPath, nativeImage.getWidth(), nativeImage.getHeight());
            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "tzz_mod_photo_" + System.nanoTime(), nativeImage);

            String safeName = sanitizeFileName(photoPath.getFileName().toString().replace(".png", ""));
            Identifier id = Identifier.of(Tzz_mod.MOD_ID, "dynamic/photo_" + safeName + "_" + System.nanoTime());
            client.getTextureManager().registerTexture(id, texture);

            textureCache.put(key, id);
            nativeTextureCache.put(key, texture);
            return id;
        } catch (Exception e) {
            Tzz_mod.LOGGER.error("Failed to load photo texture: {}", photoPath, e);
            return null;
        }
    }

    /**
     * Get the dimensions (width, height) of a photo file.
     */
    public static int[] getImageDimensions(Path photoPath) {
        String key = photoPath.toAbsolutePath().toString();
        int[] cached = dimensionCache.get(key);
        if (cached != null) {
            return new int[]{cached[0], cached[1]};
        }

        try {
            byte[] bytes = Files.readAllBytes(photoPath);
            NativeImage img = NativeImage.read(bytes);
            int[] dims = {img.getWidth(), img.getHeight()};
            dimensionCache.put(key, dims);
            img.close();
            return dims;
        } catch (Exception e) {
            return new int[]{1, 1};
        }
    }

    /**
     * Compute SHA-256 hash of a file for integrity checking.
     */
    public static String computeFileHash(Path filePath) {
        String key = filePath.toAbsolutePath().toString();
        String cached = fileHashCache.get(key);
        if (cached != null) {
            return cached;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(filePath);
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            String value = sb.toString();
            fileHashCache.put(key, value);
            return value;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Format a capture timestamp for display.
     */
    public static String formatCaptureTime(long captureTimeMs) {
        return TIME_FORMAT.format(Instant.ofEpochMilli(captureTimeMs).atZone(ZoneId.systemDefault()));
    }

    /**
     * Clean up all loaded dynamic textures.
     */
    public static void cleanup() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            for (Map.Entry<String, Identifier> entry : textureCache.entrySet()) {
                client.getTextureManager().destroyTexture(entry.getValue());
            }
        }
        for (NativeImageBackedTexture tex : nativeTextureCache.values()) {
            tex.close();
        }
        textureCache.clear();
        nativeTextureCache.clear();
        dimensionCache.clear();
        fileHashCache.clear();
        cachedPhotos = null;
        cachedPhotoHashIndex = null;
    }

    /**
     * Delete a local photo file.
     */
    public static boolean deletePhoto(Path photoPath) {
        try {
            String pathKey = photoPath.toAbsolutePath().toString();
            String photoHash = fileHashCache.remove(pathKey);
            Files.deleteIfExists(photoPath);
            // Remove from texture cache
            Identifier id = textureCache.remove(pathKey);
            NativeImageBackedTexture tex = nativeTextureCache.remove(pathKey);
            dimensionCache.remove(pathKey);
            if (id != null && MinecraftClient.getInstance() != null) {
                MinecraftClient.getInstance().getTextureManager().destroyTexture(id);
            }
            if (tex != null) tex.close();
            if (photoHash != null && !photoHash.isEmpty()) {
                deleteDirectoryQuietly(getTempPhotosDir().resolve(photoHash));
            }
            markLocalPhotosChanged();
            return true;
        } catch (IOException e) {
            Tzz_mod.LOGGER.error("Failed to delete photo: {}", photoPath, e);
            return false;
        }
    }

    private static Path getGameDir() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            return client.runDirectory.toPath();
        }
        return Path.of(".");
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static void markLocalPhotosChanged() {
        cachedPhotos = null;
        cachedPhotoHashIndex = null;
        localPhotoRevision++;
    }

    private static Map<String, Path> buildPhotoHashIndex(List<PhotoEntry> photos) {
        Map<String, Path> photoHashIndex = new LinkedHashMap<>();
        for (PhotoEntry entry : photos) {
            String hash = computeFileHash(entry.path());
            if (!hash.isEmpty()) {
                photoHashIndex.putIfAbsent(hash, entry.path());
            }
        }
        return photoHashIndex;
    }

    private static CachedImage getOrCreateCachedImage(Path sourcePath, int maxWidth, int maxHeight, boolean alwaysCache) {
        if (sourcePath == null || !Files.exists(sourcePath)) {
            return new CachedImage(sourcePath, 1, 1);
        }

        int[] sourceDims = getImageDimensions(sourcePath);
        int[] targetDims = computeScaledDimensions(sourceDims[0], sourceDims[1], maxWidth, maxHeight);
        if (!alwaysCache && sourceDims[0] == targetDims[0] && sourceDims[1] == targetDims[1]) {
            return new CachedImage(sourcePath, sourceDims[0], sourceDims[1]);
        }

        String photoHash = computeFileHash(sourcePath);
        if (photoHash.isEmpty()) {
            return new CachedImage(sourcePath, sourceDims[0], sourceDims[1]);
        }

        Path cacheDir = getTempPhotosDir().resolve(photoHash);
        String prefix = photoHash.substring(0, Math.min(8, photoHash.length()));
        Path cachePath = cacheDir.resolve(prefix + "_" + targetDims[0] + "x" + targetDims[1] + ".png");

        try {
            Files.createDirectories(cacheDir);
            long sourceModified = getLastModified(sourcePath);
            if (Files.exists(cachePath) && getLastModified(cachePath) >= sourceModified) {
                cacheDimensions(cachePath, targetDims[0], targetDims[1]);
                return new CachedImage(cachePath, targetDims[0], targetDims[1]);
            }

            if (sourceDims[0] == targetDims[0] && sourceDims[1] == targetDims[1]) {
                Files.copy(sourcePath, cachePath, StandardCopyOption.REPLACE_EXISTING);
                cacheDimensions(cachePath, targetDims[0], targetDims[1]);
                return new CachedImage(cachePath, targetDims[0], targetDims[1]);
            }

            BufferedImage sourceImage = ImageIO.read(sourcePath.toFile());
            if (sourceImage == null) {
                Files.copy(sourcePath, cachePath, StandardCopyOption.REPLACE_EXISTING);
                cacheDimensions(cachePath, targetDims[0], targetDims[1]);
                return new CachedImage(cachePath, targetDims[0], targetDims[1]);
            }

            BufferedImage scaledImage = scaleImage(sourceImage, targetDims[0], targetDims[1]);
            ImageIO.write(scaledImage, "png", cachePath.toFile());
            cacheDimensions(cachePath, targetDims[0], targetDims[1]);
            fileHashCache.remove(cachePath.toAbsolutePath().toString());
            return new CachedImage(cachePath, targetDims[0], targetDims[1]);
        } catch (Exception e) {
            Tzz_mod.LOGGER.warn("Failed to build cached gallery image for {}: {}", sourcePath, e.getMessage());
            return new CachedImage(sourcePath, sourceDims[0], sourceDims[1]);
        }
    }

    private static BufferedImage scaleImage(BufferedImage sourceImage, int targetWidth, int targetHeight) {
        int imageType = sourceImage.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage scaledImage = new BufferedImage(targetWidth, targetHeight, imageType);
        Graphics2D graphics = scaledImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();
        return scaledImage;
    }

    private static int normalizeCacheDimension(int requested, int min, int max) {
        int bounded = Math.max(min, Math.min(max, requested));
        int stepped = ((bounded + CACHE_STEP - 1) / CACHE_STEP) * CACHE_STEP;
        return Math.max(min, Math.min(max, stepped));
    }

    private static boolean isGalleryPerformanceModeEnabled() {
        try {
            return PhoneSettingsClient.isGalleryPerformanceModeEnabled();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int[] computeScaledDimensions(int sourceWidth, int sourceHeight, int maxWidth, int maxHeight) {
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return new int[]{1, 1};
        }
        double scale = Math.min(1.0D, Math.min(maxWidth / (double) sourceWidth, maxHeight / (double) sourceHeight));
        int targetWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
        int targetHeight = Math.max(1, (int) Math.round(sourceHeight * scale));
        return new int[]{targetWidth, targetHeight};
    }

    private static int[] computeScaledDimensionsForMaxPixels(int sourceWidth, int sourceHeight, long maxPixels) {
        long pixelCount = (long) sourceWidth * sourceHeight;
        if (pixelCount <= maxPixels) {
            return new int[]{sourceWidth, sourceHeight};
        }
        double scale = Math.sqrt(maxPixels / (double) pixelCount);
        int targetWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
        int targetHeight = Math.max(1, (int) Math.round(sourceHeight * scale));
        return new int[]{targetWidth, targetHeight};
    }

    private static void cacheDimensions(Path filePath, int width, int height) {
        dimensionCache.put(filePath.toAbsolutePath().toString(), new int[]{width, height});
    }

    private static long getLastModified(Path filePath) {
        try {
            return Files.getLastModifiedTime(filePath).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private static void deleteDirectoryQuietly(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try (var walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    /**
     * A loaded photo entry with its file path and metadata.
     */
    public record PhotoEntry(Path path, PhotoMetadata metadata) {}

    public record CachedImage(Path path, int width, int height) {}

    public record PreparedUploadPhoto(Path filePath, byte[] data, String photoId, PhotoMetadata metadata) {}
}
