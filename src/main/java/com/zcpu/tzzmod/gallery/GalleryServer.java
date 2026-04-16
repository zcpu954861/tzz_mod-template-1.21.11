package com.zcpu.tzzmod.gallery;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.network.GalleryC2SPayload;
import com.zcpu.tzzmod.network.GalleryS2CPayload;
import com.zcpu.tzzmod.util.NullSafety;
import com.zcpu.tzzmod.util.SharedImageTransferBudget;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side online gallery system.
 *
 * Storage structure:
 *   <server root>/tzzserverphotos/photos/<photoId>.png     — full image
 *   <server root>/tzzserverphotos/thumbs/<photoId>.png     — gallery thumbnail
 *   <server root>/tzzserverphotos/meta/<photoId>.json      — metadata JSON
 *   <server root>/tzzserverphotos/config.json              — gallery config (bandwidth, enabled)
 *
 * Photo ID = SHA-256 hash of the file content (same as local hash).
 *
 * Wire protocol actions:
 *   C2S: "upload_start", "upload_chunk", "upload_finish",
 *        "list", "download_start", "download_ack",
 *        "delete"
 *   S2C: "upload_ack", "upload_complete",
 *        "photo_list", "download_data", "download_complete",
 *        "deleted", "error"
 */
public final class GalleryServer {

    private static final String GALLERY_ROOT_DIR = "tzzserverphotos";
    private static final int THUMBNAIL_MAX_EDGE = 640;
    private static final long TARGET_UPLOAD_PIXELS = 1920L * 1080L;
    private static final Map<UUID, UploadSession> uploads = new ConcurrentHashMap<>();
    private static final Set<Path> migratedRoots = ConcurrentHashMap.newKeySet();

    private GalleryServer() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(GalleryC2SPayload.ID, (payload, context) ->
            context.server().execute(() -> handlePayload(context.server(), context.player(), payload))
        );
    }

    private static void handlePayload(MinecraftServer server, ServerPlayerEntity player, GalleryC2SPayload payload) {
        GalleryConfig config = GalleryConfig.get(server);
        if (!config.enabled) {
            sendError(player, "Online gallery is disabled.");
            return;
        }

        ensureGalleryStorageReady(server);

        JsonObject body = parse(payload.bodyJson());
        switch (payload.action()) {
            case "list" -> handleList(server, player);
            case "upload_start" -> handleUploadStart(server, player, body, config);
            case "upload_chunk" -> handleUploadChunk(server, player, body, config);
            case "upload_finish" -> handleUploadFinish(server, player, body);
            case "download_start" -> handleDownloadStart(server, player, body, config);
            case "delete" -> handleDelete(server, player, body);
            default -> sendError(player, "Unknown gallery action: " + payload.action());
        }
    }

    // --- List ---
    private static void handleList(MinecraftServer server, ServerPlayerEntity player) {
        Path metaDir = getMetaDir(server);
        List<JsonObject> entries = new ArrayList<>();
        if (Files.isDirectory(metaDir)) {
            try (var stream = Files.list(metaDir)) {
                stream.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                    try {
                        String json = Files.readString(p, StandardCharsets.UTF_8);
                        JsonObject meta = JsonParser.parseString(json).getAsJsonObject();
                        if (!isHidden(meta)) {
                            entries.add(meta);
                        }
                    } catch (Exception ignored) {}
                });
            } catch (Exception e) {
                Tzz_mod.LOGGER.warn("Failed to list gallery photos: {}", e.getMessage());
            }
        }
        entries.sort(Comparator.comparingLong(GalleryServer::getPhotoSortTime).reversed());
        JsonArray arr = new JsonArray();
        for (JsonObject entry : entries) {
            arr.add(entry);
        }
        JsonObject resp = new JsonObject();
        resp.add("photos", arr);
        sendResponse(player, "photo_list", resp);
    }

    // --- Upload ---
    private static void handleUploadStart(MinecraftServer server, ServerPlayerEntity player, JsonObject body, GalleryConfig config) {
        String photoId = body.has("photoId") ? body.get("photoId").getAsString() : "";
        int totalSize = body.has("totalSize") ? body.get("totalSize").getAsInt() : 0;
        String metaJson = body.has("metadata") ? body.get("metadata").toString() : "{}";

        if (photoId.isEmpty() || totalSize <= 0) {
            sendError(player, "Invalid upload parameters.");
            return;
        }

        // Check if photo already exists
        Path photoFile = getPhotosDir(server).resolve(photoId + ".png");
        if (Files.exists(photoFile)) {
            try {
                restoreExistingPhoto(server, player, photoId, metaJson);
                JsonObject resp = new JsonObject();
                resp.addProperty("photoId", photoId);
                resp.addProperty("skippedUpload", true);
                sendResponse(player, "upload_complete", resp);
            } catch (Exception e) {
                Tzz_mod.LOGGER.error("Failed to restore hidden gallery photo {}: {}", photoId, e.getMessage());
                sendError(player, "Server failed to restore existing photo.");
            }
            return;
        }

        SharedImageTransferBudget.TransferLease lease = SharedImageTransferBudget.acquireUpload();
        UploadSession session = new UploadSession(photoId, totalSize, metaJson,
            player.getUuidAsString(), player.getName().getString(), lease);
        UploadSession previous = uploads.put(player.getUuid(), session);
        if (previous != null) {
            previous.close();
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("photoId", photoId);
        resp.addProperty("chunkSize", currentUploadChunkSize(config.uploadBandwidthMbps));
        sendResponse(player, "upload_ack", resp);
    }

    private static void handleUploadChunk(MinecraftServer server, ServerPlayerEntity player, JsonObject body, GalleryConfig config) {
        UploadSession session = uploads.get(player.getUuid());
        if (session == null) {
            sendError(player, "No active upload session.");
            return;
        }

        String dataBase64 = body.has("data") ? body.get("data").getAsString() : "";
        if (dataBase64.isEmpty()) {
            sendError(player, "Empty upload chunk.");
            return;
        }

        byte[] chunk = Base64.getDecoder().decode(dataBase64);
        if (session.receivedBytes + chunk.length > session.totalSize) {
            uploads.remove(player.getUuid());
            session.close();
            sendError(player, "Upload exceeded declared size.");
            return;
        }
        session.appendData(chunk);

        // Progress ACK
        float progress = (float) session.receivedBytes / session.totalSize;
        JsonObject resp = new JsonObject();
        resp.addProperty("photoId", session.photoId);
        resp.addProperty("progress", Math.min(1f, progress));
        resp.addProperty("chunkSize", currentUploadChunkSize(config.uploadBandwidthMbps));
        sendResponse(player, "upload_ack", resp);
    }

    private static void handleUploadFinish(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        UploadSession session = uploads.remove(player.getUuid());
        if (session == null) {
            sendError(player, "No active upload session.");
            return;
        }

        try {
            if (session.receivedBytes != session.totalSize) {
                sendError(player, "Upload is incomplete.");
                return;
            }

            ProcessedImages processedImages = processUploadedImages(session.data.toByteArray());

            // Save photo file
            Path photosDir = getPhotosDir(server);
            Path thumbsDir = getThumbsDir(server);
            Files.createDirectories(photosDir);
            Files.createDirectories(thumbsDir);
            Path photoFile = photosDir.resolve(session.photoId + ".png");
            Path thumbFile = thumbsDir.resolve(session.photoId + ".png");
            Files.write(photoFile, processedImages.fullImageData());
            Files.write(thumbFile, processedImages.thumbnailData());

            // Save metadata
            JsonObject meta = parse(session.metaJson);
            meta.addProperty("photoId", session.photoId);
            meta.addProperty("uploaderUuid", session.uploaderUuid);
            meta.addProperty("uploaderName", session.uploaderName);
            meta.addProperty("uploadTimeMs", System.currentTimeMillis());
            meta.addProperty("hidden", false);
            if (processedImages.width() > 0 && processedImages.height() > 0) {
                meta.addProperty("imageWidth", processedImages.width());
                meta.addProperty("imageHeight", processedImages.height());
            }

            Path metaDir = getMetaDir(server);
            Files.createDirectories(metaDir);
            Files.writeString(metaDir.resolve(session.photoId + ".json"),
                    meta.toString(), StandardCharsets.UTF_8);

            JsonObject resp = new JsonObject();
            resp.addProperty("photoId", session.photoId);
            sendResponse(player, "upload_complete", resp);

        } catch (Exception e) {
            Tzz_mod.LOGGER.error("Failed to save uploaded photo: {}", e.getMessage());
            sendError(player, "Server failed to save photo.");
        } finally {
            session.close();
        }
    }

    // --- Download ---
    private static void handleDownloadStart(MinecraftServer server, ServerPlayerEntity player, JsonObject body, GalleryConfig config) {
        String photoId = body.has("photoId") ? body.get("photoId").getAsString() : "";
        boolean thumbnail = body.has("thumbnail") && body.get("thumbnail").getAsBoolean();
        if (photoId.isEmpty()) {
            sendError(player, "Missing photoId.");
            return;
        }

        Path metaFile = getMetaDir(server).resolve(photoId + ".json");
        if (Files.exists(metaFile)) {
            try {
                if (isHidden(parse(Files.readString(metaFile, StandardCharsets.UTF_8)))) {
                    sendError(player, "Photo not found.", photoId, thumbnail);
                    return;
                }
            } catch (IOException e) {
                Tzz_mod.LOGGER.warn("Failed to read gallery metadata for {}: {}", photoId, e.getMessage());
            }
        }

        Path photoFile;
        try {
            photoFile = resolveDownloadFile(server, photoId, thumbnail);
        } catch (IOException e) {
            Tzz_mod.LOGGER.error("Failed to prepare gallery download for {}: {}", photoId, e.getMessage());
            sendError(player, "Server failed to prepare photo.", photoId, thumbnail);
            return;
        }

        if (!Files.exists(photoFile)) {
            sendError(player, "Photo not found.", photoId, thumbnail);
            return;
        }

        try {
            byte[] data = Files.readAllBytes(photoFile);
            Thread downloadThread = new Thread(() -> streamDownload(server, player, photoId, thumbnail, data, config.downloadBandwidthMbps),
                    "TzzMod-PhotoDownload-" + (thumbnail ? "thumb-" : "full-") + photoId.substring(0, Math.min(8, photoId.length())));
            downloadThread.setDaemon(true);
            downloadThread.start();

        } catch (Exception e) {
            Tzz_mod.LOGGER.error("Failed to send photo download: {}", e.getMessage());
            sendError(player, "Server failed to send photo.", photoId, thumbnail);
        }
    }

    // --- Delete ---
    private static void handleDelete(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        String photoId = body.has("photoId") ? body.get("photoId").getAsString() : "";
        boolean forceDelete = body.has("force") && body.get("force").getAsBoolean();

        if (photoId.isEmpty()) {
            sendError(player, "Missing photoId.");
            return;
        }

        Path metaFile = getMetaDir(server).resolve(photoId + ".json");
        if (!Files.exists(metaFile)) {
            sendError(player, "Photo not found.");
            return;
        }

        try {
            String metaStr = Files.readString(metaFile, StandardCharsets.UTF_8);
            JsonObject meta = JsonParser.parseString(metaStr).getAsJsonObject();
            String uploaderUuid = meta.has("uploaderUuid") ? meta.get("uploaderUuid").getAsString() : "";

            boolean isUploader = player.getUuidAsString().equals(uploaderUuid);
            boolean isOp = player.isCreativeLevelTwoOp();

            if (!isUploader && !(forceDelete && isOp)) {
                sendError(player, "You do not have permission to delete this photo.");
                return;
            }

            meta.addProperty("hidden", true);
            meta.addProperty("hiddenTimeMs", System.currentTimeMillis());
            Files.writeString(metaFile, meta.toString(), StandardCharsets.UTF_8);

            JsonObject resp = new JsonObject();
            resp.addProperty("photoId", photoId);
            sendResponse(player, "deleted", resp);

        } catch (Exception e) {
            Tzz_mod.LOGGER.error("Failed to delete photo: {}", e.getMessage());
            sendError(player, "Server failed to delete photo.");
        }
    }

    // --- Utilities ---
    private static Path getGalleryDir(MinecraftServer server) {
        return server.getRunDirectory().resolve(GALLERY_ROOT_DIR);
    }

    private static Path getPhotosDir(MinecraftServer server) {
        return getGalleryDir(server).resolve("photos");
    }

    private static Path getThumbsDir(MinecraftServer server) {
        return getGalleryDir(server).resolve("thumbs");
    }

    private static Path getMetaDir(MinecraftServer server) {
        return getGalleryDir(server).resolve("meta");
    }

    private static void sendResponse(ServerPlayerEntity player, String action, JsonObject body) {
        ServerPlayNetworking.send(NullSafety.requireNonNull(player), new GalleryS2CPayload(action, body.toString()));
    }

    private static void sendError(ServerPlayerEntity player, String message) {
        JsonObject error = new JsonObject();
        error.addProperty("message", message);
        sendResponse(player, "error", error);
    }

    private static void sendError(ServerPlayerEntity player, String message, String photoId, boolean thumbnail) {
        JsonObject error = new JsonObject();
        error.addProperty("message", message);
        error.addProperty("photoId", photoId);
        error.addProperty("thumbnail", thumbnail);
        sendResponse(player, "error", error);
    }

    private static JsonObject parse(String json) {
        try {
            JsonElement el = JsonParser.parseString(json);
            return el.isJsonObject() ? el.getAsJsonObject() : new JsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    private static long getPhotoSortTime(JsonObject meta) {
        if (meta.has("uploadTimeMs")) {
            return meta.get("uploadTimeMs").getAsLong();
        }
        if (meta.has("captureTimeMs")) {
            return meta.get("captureTimeMs").getAsLong();
        }
        return 0L;
    }

    private static int currentUploadChunkSize(double bandwidthMbps) {
        return SharedImageTransferBudget.recommendUploadChunkSize(bandwidthMbps);
    }

    private static int currentDownloadChunkSize(double bandwidthMbps) {
        return SharedImageTransferBudget.recommendDownloadChunkSize(bandwidthMbps);
    }

    private static boolean isHidden(JsonObject meta) {
        return meta.has("hidden") && meta.get("hidden").getAsBoolean();
    }

    private static void restoreExistingPhoto(MinecraftServer server, ServerPlayerEntity player, String photoId, String metaJson) throws IOException {
        Path photoFile = getPhotosDir(server).resolve(photoId + ".png");
        Path thumbFile = getThumbsDir(server).resolve(photoId + ".png");
        Path metaFile = getMetaDir(server).resolve(photoId + ".json");

        JsonObject mergedMeta = Files.exists(metaFile)
                ? parse(Files.readString(metaFile, StandardCharsets.UTF_8))
                : new JsonObject();
        JsonObject incomingMeta = parse(metaJson);
        for (Map.Entry<String, JsonElement> entry : incomingMeta.entrySet()) {
            mergedMeta.add(entry.getKey(), entry.getValue().deepCopy());
        }

        mergedMeta.addProperty("photoId", photoId);
        mergedMeta.addProperty("uploaderUuid", player.getUuidAsString());
        mergedMeta.addProperty("uploaderName", player.getName().getString());
        mergedMeta.addProperty("uploadTimeMs", System.currentTimeMillis());
        mergedMeta.addProperty("hidden", false);
        mergedMeta.remove("hiddenTimeMs");

        Files.createDirectories(getMetaDir(server));
        Files.writeString(metaFile, mergedMeta.toString(), StandardCharsets.UTF_8);

        if (Files.exists(photoFile) && !Files.exists(thumbFile)) {
            Files.createDirectories(thumbFile.getParent());
            Files.write(thumbFile, createThumbnailBytes(Files.readAllBytes(photoFile)));
        }
    }

    private static void streamDownload(MinecraftServer server, ServerPlayerEntity player,
                                       String photoId, boolean thumbnail, byte[] data, double bandwidthMbps) {
        try (SharedImageTransferBudget.TransferLease ignored = SharedImageTransferBudget.acquireDownload()) {
            int offset = 0;

            while (offset < data.length) {
                int chunkSize = currentDownloadChunkSize(bandwidthMbps);
                int end = Math.min(offset + chunkSize, data.length);
                byte[] chunk = Arrays.copyOfRange(data, offset, end);
                String base64 = Base64.getEncoder().encodeToString(chunk);

                JsonObject resp = new JsonObject();
                resp.addProperty("photoId", photoId);
                resp.addProperty("thumbnail", thumbnail);
                resp.addProperty("data", base64);
                resp.addProperty("progress", (float) end / data.length);

                JsonObject responseBody = resp;
                server.execute(() -> sendResponse(player, "download_data", responseBody));

                offset = end;
                if (offset < data.length) {
                    Thread.sleep(SharedImageTransferBudget.getTransferIntervalMs());
                }
            }

            JsonObject complete = new JsonObject();
            complete.addProperty("photoId", photoId);
            complete.addProperty("thumbnail", thumbnail);
            server.execute(() -> sendResponse(player, "download_complete", complete));
        } catch (Exception e) {
            Tzz_mod.LOGGER.error("Failed to stream gallery download for {}: {}", photoId, e.getMessage());
            server.execute(() -> sendError(player, "Server failed to send photo.", photoId, thumbnail));
        }
    }

    private static void ensureGalleryStorageReady(MinecraftServer server) {
        Path galleryRoot = getGalleryDir(server);
        if (!migratedRoots.add(galleryRoot)) {
            return;
        }

        try {
            Files.createDirectories(getPhotosDir(server));
            Files.createDirectories(getThumbsDir(server));
            Files.createDirectories(getMetaDir(server));
            migrateLegacyGallery(server, galleryRoot);
        } catch (IOException e) {
            Tzz_mod.LOGGER.warn("Failed to prepare gallery storage at {}: {}", galleryRoot, e.getMessage());
        }
    }

    private static void migrateLegacyGallery(MinecraftServer server, Path galleryRoot) {
        Path legacyRoot = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("tzz_mod").resolve("gallery");
        if (galleryRoot.equals(legacyRoot) || !Files.exists(legacyRoot)) {
            return;
        }

        try {
            copyDirectoryIfMissing(legacyRoot.resolve("photos"), getPhotosDir(server));
            copyDirectoryIfMissing(legacyRoot.resolve("meta"), getMetaDir(server));
        } catch (IOException e) {
            Tzz_mod.LOGGER.warn("Failed to migrate legacy gallery data: {}", e.getMessage());
        }
    }

    private static void copyDirectoryIfMissing(Path sourceDir, Path targetDir) throws IOException {
        if (!Files.isDirectory(sourceDir)) {
            return;
        }

        Files.createDirectories(targetDir);
        try (var stream = Files.list(sourceDir)) {
            for (Path source : stream.toList()) {
                Path target = targetDir.resolve(source.getFileName().toString());
                if (!Files.exists(target)) {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static Path resolveDownloadFile(MinecraftServer server, String photoId, boolean thumbnail) throws IOException {
        if (!thumbnail) {
            return getPhotosDir(server).resolve(photoId + ".png");
        }

        Path thumbFile = getThumbsDir(server).resolve(photoId + ".png");
        if (Files.exists(thumbFile)) {
            return thumbFile;
        }

        Path fullFile = getPhotosDir(server).resolve(photoId + ".png");
        if (!Files.exists(fullFile)) {
            return thumbFile;
        }

        Files.createDirectories(thumbFile.getParent());
        Files.write(thumbFile, createThumbnailBytes(Files.readAllBytes(fullFile)));
        return thumbFile;
    }

    private static ProcessedImages processUploadedImages(byte[] sourceData) throws IOException {
        BufferedImage sourceImage = ImageIO.read(new ByteArrayInputStream(sourceData));
        if (sourceImage == null) {
            return new ProcessedImages(sourceData, sourceData, 0, 0);
        }

        int[] fullDims = scaleToMaxPixels(sourceImage.getWidth(), sourceImage.getHeight(), TARGET_UPLOAD_PIXELS);
        BufferedImage fullImage = (fullDims[0] == sourceImage.getWidth() && fullDims[1] == sourceImage.getHeight())
                ? sourceImage
                : scaleImage(sourceImage, fullDims[0], fullDims[1]);

        int[] thumbDims = scaleToFit(fullImage.getWidth(), fullImage.getHeight(), THUMBNAIL_MAX_EDGE, THUMBNAIL_MAX_EDGE);
        BufferedImage thumbnailImage = (thumbDims[0] == fullImage.getWidth() && thumbDims[1] == fullImage.getHeight())
                ? fullImage
                : scaleImage(fullImage, thumbDims[0], thumbDims[1]);

        return new ProcessedImages(
                encodePng(fullImage),
                encodePng(thumbnailImage),
                fullImage.getWidth(),
                fullImage.getHeight()
        );
    }

    private static byte[] createThumbnailBytes(byte[] sourceData) throws IOException {
        BufferedImage sourceImage = ImageIO.read(new ByteArrayInputStream(sourceData));
        if (sourceImage == null) {
            return sourceData;
        }

        int[] thumbDims = scaleToFit(sourceImage.getWidth(), sourceImage.getHeight(), THUMBNAIL_MAX_EDGE, THUMBNAIL_MAX_EDGE);
        BufferedImage thumbnailImage = (thumbDims[0] == sourceImage.getWidth() && thumbDims[1] == sourceImage.getHeight())
                ? sourceImage
                : scaleImage(sourceImage, thumbDims[0], thumbDims[1]);
        return encodePng(thumbnailImage);
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

    private static byte[] encodePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static int[] scaleToFit(int width, int height, int maxWidth, int maxHeight) {
        if (width <= 0 || height <= 0) {
            return new int[]{1, 1};
        }

        double scale = Math.min(1.0D, Math.min(maxWidth / (double) width, maxHeight / (double) height));
        return new int[]{
                Math.max(1, (int) Math.round(width * scale)),
                Math.max(1, (int) Math.round(height * scale))
        };
    }

    private static int[] scaleToMaxPixels(int width, int height, long maxPixels) {
        long pixelCount = (long) width * height;
        if (pixelCount <= maxPixels) {
            return new int[]{width, height};
        }

        double scale = Math.sqrt(maxPixels / (double) pixelCount);
        return new int[]{
                Math.max(1, (int) Math.round(width * scale)),
                Math.max(1, (int) Math.round(height * scale))
        };
    }

    // --- Upload session state ---
    private static class UploadSession {
        final String photoId;
        final int totalSize;
        final String metaJson;
        final String uploaderUuid;
        final String uploaderName;
        final SharedImageTransferBudget.TransferLease lease;
        final ByteArrayOutputStream data = new ByteArrayOutputStream();
        int receivedBytes = 0;

        UploadSession(String photoId, int totalSize, String metaJson, String uploaderUuid, String uploaderName,
                      SharedImageTransferBudget.TransferLease lease) {
            this.photoId = photoId;
            this.totalSize = totalSize;
            this.metaJson = metaJson;
            this.uploaderUuid = uploaderUuid;
            this.uploaderName = uploaderName;
            this.lease = lease;
        }

        void appendData(byte[] chunk) {
            data.write(chunk, 0, chunk.length);
            receivedBytes += chunk.length;
        }

        void close() {
            lease.close();
        }
    }

    private record ProcessedImages(byte[] fullImageData, byte[] thumbnailData, int width, int height) {}
}
