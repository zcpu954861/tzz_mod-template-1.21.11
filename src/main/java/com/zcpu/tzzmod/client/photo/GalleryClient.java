package com.zcpu.tzzmod.client.photo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.network.GalleryC2SPayload;
import com.zcpu.tzzmod.network.GalleryS2CPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Client-side state manager for the online gallery.
 * Handles S2C responses and provides API for UI screens.
 */
public final class GalleryClient {
    private static final int MAX_SAFE_UPLOAD_CHUNK_SIZE = 12_000;
    private static volatile boolean adminGalleryEnabled = true;
    private static final List<OnlinePhotoEntry> onlinePhotos = new CopyOnWriteArrayList<>();
    private static volatile boolean newData = false;

    // Upload state
    private static int uploadChunkSize = MAX_SAFE_UPLOAD_CHUNK_SIZE;
    private static Consumer<Float> uploadProgressCallback;
    private static Consumer<Boolean> uploadCompleteCallback;
    private static PendingUpload pendingUpload;

    // Download state
    private static final Map<String, DownloadSession> downloadSessions = new HashMap<>();
    private static final Set<String> pendingThumbnailRequests = Collections.synchronizedSet(new HashSet<>());
    private static final Map<String, Path> recentUploadedPhotoSources = new ConcurrentHashMap<>();

    private GalleryClient() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(GalleryS2CPayload.ID, (payload, context) ->
            context.client().execute(() -> handlePayload(context.client(), payload))
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> resetState());
    }

    private static void handlePayload(MinecraftClient client, GalleryS2CPayload payload) {
        JsonObject body = parse(payload.bodyJson());
        switch (payload.action()) {
            case "photo_list" -> handlePhotoList(body);
            case "upload_ack" -> handleUploadAck(body);
            case "upload_complete" -> handleUploadComplete(body);
            case "download_data" -> handleDownloadData(body);
            case "download_complete" -> handleDownloadComplete(body);
            case "deleted" -> handleDeleted(body);
            case "error" -> handleError(body);
        }
    }

    // --- Public API for UI screens ---

    public static boolean isOnlineGalleryEnabled() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc != null && mc.getCurrentServerEntry() != null && adminGalleryEnabled;
    }

    public static void setAdminGalleryEnabled(boolean enabled) {
        adminGalleryEnabled = enabled;
    }

    public static boolean isAdminGalleryEnabled() {
        return adminGalleryEnabled;
    }

    public static void requestOnlinePhotos() {
        send("list", new JsonObject());
    }

    public static List<OnlinePhotoEntry> getOnlinePhotos() {
        return Collections.unmodifiableList(onlinePhotos);
    }

    public static boolean hasNewData() {
        return newData;
    }

    public static void clearNewDataFlag() {
        newData = false;
    }

    /**
     * Upload a local photo to the online gallery.
     */
    public static void uploadPhoto(Path photoPath, PhotoMetadata metadata,
                                   Consumer<Float> progressCallback,
                                   Consumer<Boolean> completeCallback) {
        if (pendingUpload != null || uploadCompleteCallback != null) {
            if (completeCallback != null) {
                completeCallback.accept(false);
            }
            return;
        }

        uploadProgressCallback = progressCallback;
        uploadCompleteCallback = completeCallback;

        try {
            PhotoManager.PreparedUploadPhoto preparedPhoto = PhotoManager.preparePhotoForUpload(photoPath, metadata);
            byte[] fileData = preparedPhoto.data();
            String photoId = preparedPhoto.photoId();
            PhotoMetadata uploadMetadata = preparedPhoto.metadata();
            recentUploadedPhotoSources.put(photoId, preparedPhoto.filePath());
            if (photoId.isEmpty()) {
                if (completeCallback != null) {
                    completeCallback.accept(false);
                }
                uploadProgressCallback = null;
                uploadCompleteCallback = null;
                return;
            }

            // Build metadata JSON
            JsonObject metaObj = new JsonObject();
            if (uploadMetadata != null) {
                metaObj.addProperty("captureTimeMs", uploadMetadata.captureTimeMs());
                metaObj.addProperty("worldId", uploadMetadata.worldId());
                metaObj.addProperty("playerName", uploadMetadata.playerName());
                metaObj.addProperty("playerUuid", uploadMetadata.playerUuid());
                metaObj.addProperty("imageWidth", uploadMetadata.imageWidth());
                metaObj.addProperty("imageHeight", uploadMetadata.imageHeight());
            }

            // Send upload_start
            JsonObject startBody = new JsonObject();
            startBody.addProperty("photoId", photoId);
            startBody.addProperty("totalSize", fileData.length);
            startBody.add("metadata", metaObj);
            pendingUpload = new PendingUpload(photoId, fileData);
            send("upload_start", startBody);

        } catch (IOException e) {
            Tzz_mod.LOGGER.error("Failed to read photo for upload: {}", e.getMessage());
            pendingUpload = null;
            uploadProgressCallback = null;
            uploadCompleteCallback = null;
            if (completeCallback != null) completeCallback.accept(false);
        }
    }

    /**
     * Download an online photo.
     */
    public static void downloadPhoto(OnlinePhotoEntry entry,
                                     Consumer<Float> progressCallback,
                                     Consumer<Path> completeCallback) {
        String photoId = entry.photoId();
        Path reusablePath = resolveReusablePhotoPath(photoId, entry.localCachePath());
        if (reusablePath != null) {
            updateOnlineEntryCache(photoId, entry.thumbnailPath() == null ? reusablePath : null, reusablePath);
            if (completeCallback != null) completeCallback.accept(reusablePath);
            return;
        }

        Path cachePath = getFullCacheDir().resolve(photoId + ".png");

        // If already cached, return immediately
        if (Files.exists(cachePath)) {
            updateOnlineEntryCache(photoId, null, cachePath);
            if (completeCallback != null) completeCallback.accept(cachePath);
            return;
        }

        String downloadKey = downloadKey(photoId, false);
        DownloadSession session = new DownloadSession(cachePath, progressCallback, completeCallback);
        downloadSessions.put(downloadKey, session);

        JsonObject body = new JsonObject();
        body.addProperty("photoId", photoId);
        body.addProperty("thumbnail", false);
        send("download_start", body);
    }

    /**
     * Delete an online photo.
     */
    public static void deleteOnlinePhoto(String photoId, boolean force) {
        JsonObject body = new JsonObject();
        body.addProperty("photoId", photoId);
        body.addProperty("force", force);
        send("delete", body);
    }

    // --- S2C handlers ---

    private static void handlePhotoList(JsonObject body) {
        onlinePhotos.clear();
        if (body.has("photos")) {
            JsonArray arr = body.getAsJsonArray("photos");
            for (JsonElement el : arr) {
                JsonObject meta = el.getAsJsonObject();
                String photoId = meta.has("photoId") ? meta.get("photoId").getAsString() : "";
                String uploaderUuid = meta.has("uploaderUuid") ? meta.get("uploaderUuid").getAsString() : "";
                String uploaderName = meta.has("uploaderName") ? meta.get("uploaderName").getAsString() : "";
                long captureTimeMs = meta.has("captureTimeMs") ? meta.get("captureTimeMs").getAsLong() : 0;
                int imgW = meta.has("imageWidth") ? meta.get("imageWidth").getAsInt() : 0;
                int imgH = meta.has("imageHeight") ? meta.get("imageHeight").getAsInt() : 0;
                String worldId = meta.has("worldId") ? meta.get("worldId").getAsString() : "";
                String playerName = meta.has("playerName") ? meta.get("playerName").getAsString() : "";
                String playerUuid = meta.has("playerUuid") ? meta.get("playerUuid").getAsString() : "";

                PhotoMetadata metadata = new PhotoMetadata(
                        PhotoMetadata.MOD_TAG, captureTimeMs, worldId, playerName, playerUuid, imgW, imgH
                );

                Path reusableLocalPath = resolveReusablePhotoPath(photoId, null);
                Path fullCachePath = getFullCacheDir().resolve(photoId + ".png");
                Path thumbCachePath = getThumbnailCacheDir().resolve(photoId + ".png");
                Path cachePath = reusableLocalPath != null
                    ? reusableLocalPath
                    : (Files.exists(fullCachePath) ? fullCachePath : null);
                Path thumbPath = reusableLocalPath != null
                    ? reusableLocalPath
                    : (Files.exists(thumbCachePath) ? thumbCachePath : cachePath);

                onlinePhotos.add(new OnlinePhotoEntry(
                        photoId, uploaderUuid, uploaderName, thumbPath, cachePath, metadata
                ));
            }
        }
            requestMissingThumbnails();
        newData = true;
    }

    private static void handleUploadAck(JsonObject body) {
        if (body.has("chunkSize")) {
            uploadChunkSize = body.get("chunkSize").getAsInt();
            if (pendingUpload != null && !pendingUpload.started) {
                pendingUpload.started = true;
                startUploadThread(pendingUpload);
            }
        }
        if (body.has("progress") && uploadProgressCallback != null) {
            uploadProgressCallback.accept(body.get("progress").getAsFloat());
        }
    }

    private static void handleUploadComplete(JsonObject body) {
        pendingUpload = null;
        if (uploadCompleteCallback != null) {
            uploadCompleteCallback.accept(true);
        }
        uploadProgressCallback = null;
        uploadCompleteCallback = null;
        // Refresh the list
        requestOnlinePhotos();
    }

    private static void handleDownloadData(JsonObject body) {
        String photoId = body.has("photoId") ? body.get("photoId").getAsString() : "";
        boolean thumbnail = body.has("thumbnail") && body.get("thumbnail").getAsBoolean();
        DownloadSession session = downloadSessions.get(downloadKey(photoId, thumbnail));
        if (session == null) return;

        String base64 = body.has("data") ? body.get("data").getAsString() : "";
        if (!base64.isEmpty()) {
            byte[] chunk;
            try {
                chunk = Base64.getDecoder().decode(base64);
            } catch (IllegalArgumentException exception) {
                pendingThumbnailRequests.remove(photoId);
                DownloadSession failedSession = downloadSessions.remove(downloadKey(photoId, thumbnail));
                if (failedSession != null && failedSession.completeCallback != null) {
                    failedSession.completeCallback.accept(null);
                }
                return;
            }
            session.data.write(chunk, 0, chunk.length);
        }

        float progress = body.has("progress") ? body.get("progress").getAsFloat() : 0f;
        if (session.progressCallback != null) {
            session.progressCallback.accept(progress);
        }
    }

    private static void handleDownloadComplete(JsonObject body) {
        String photoId = body.has("photoId") ? body.get("photoId").getAsString() : "";
        boolean thumbnail = body.has("thumbnail") && body.get("thumbnail").getAsBoolean();
        pendingThumbnailRequests.remove(photoId);
        DownloadSession session = downloadSessions.remove(downloadKey(photoId, thumbnail));
        if (session == null) return;

        try {
            Files.createDirectories(session.cachePath.getParent());
            Files.write(session.cachePath, session.data.toByteArray());
            updateOnlineEntryCache(photoId, thumbnail ? session.cachePath : null, thumbnail ? null : session.cachePath);
            if (session.completeCallback != null) {
                session.completeCallback.accept(session.cachePath);
            }
            newData = true;
        } catch (IOException e) {
            Tzz_mod.LOGGER.error("Failed to save downloaded photo: {}", e.getMessage());
            if (session.completeCallback != null) {
                session.completeCallback.accept(null);
            }
        }
    }

    private static void handleDeleted(JsonObject body) {
        String photoId = body.has("photoId") ? body.get("photoId").getAsString() : "";
        onlinePhotos.removeIf(e -> e.photoId().equals(photoId));
        pendingThumbnailRequests.remove(photoId);
        downloadSessions.remove(downloadKey(photoId, true));
        downloadSessions.remove(downloadKey(photoId, false));
        newData = true;
    }

    private static void handleError(JsonObject body) {
        String msg = body.has("message") ? body.get("message").getAsString() : "Unknown error";
        Tzz_mod.LOGGER.warn("Gallery server error: {}", msg);

        String photoId = body.has("photoId") ? body.get("photoId").getAsString() : "";
        boolean thumbnail = body.has("thumbnail") && body.get("thumbnail").getAsBoolean();
        if (!photoId.isEmpty()) {
            pendingThumbnailRequests.remove(photoId);
            DownloadSession session = downloadSessions.remove(downloadKey(photoId, thumbnail));
            if (session != null && session.completeCallback != null) {
                session.completeCallback.accept(null);
            }
        }

        pendingUpload = null;
        // If we were uploading, signal failure
        if (uploadCompleteCallback != null) {
            uploadCompleteCallback.accept(false);
            uploadCompleteCallback = null;
            uploadProgressCallback = null;
        }
    }

    private static void startUploadThread(PendingUpload upload) {
        Thread uploadThread = new Thread(() -> {
            try {
                int chunkSize = Math.max(1024, Math.min(MAX_SAFE_UPLOAD_CHUNK_SIZE, uploadChunkSize));
                int offset = 0;
                while (offset < upload.fileData.length) {
                    int end = Math.min(offset + chunkSize, upload.fileData.length);
                    byte[] chunk = Arrays.copyOfRange(upload.fileData, offset, end);
                    String base64 = Base64.getEncoder().encodeToString(chunk);

                    JsonObject chunkBody = new JsonObject();
                    chunkBody.addProperty("data", base64);
                    int sentBytes = end;
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client == null) {
                        throw new IllegalStateException("Minecraft client is unavailable during upload.");
                    }
                    client.execute(() -> {
                        send("upload_chunk", chunkBody);
                        if (uploadProgressCallback != null) {
                            uploadProgressCallback.accept((float) sentBytes / upload.fileData.length);
                        }
                    });

                    offset = end;
                    Thread.sleep(50L);
                }

                MinecraftClient client = MinecraftClient.getInstance();
                if (client == null) {
                    throw new IllegalStateException("Minecraft client is unavailable while finishing upload.");
                }
                client.execute(() -> {
                    JsonObject finishBody = new JsonObject();
                    finishBody.addProperty("photoId", upload.photoId);
                    send("upload_finish", finishBody);
                });
            } catch (Exception e) {
                Tzz_mod.LOGGER.error("Upload thread error: {}", e.getMessage());
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null) {
                    client.execute(() -> {
                        pendingUpload = null;
                        if (uploadCompleteCallback != null) {
                            uploadCompleteCallback.accept(false);
                            uploadCompleteCallback = null;
                        }
                        uploadProgressCallback = null;
                    });
                }
            }
        }, "TzzMod-PhotoUpload");
        uploadThread.setDaemon(true);
        uploadThread.start();
    }

    private static void resetState() {
        onlinePhotos.clear();
        downloadSessions.clear();
        pendingThumbnailRequests.clear();
        recentUploadedPhotoSources.clear();
        newData = false;
        uploadChunkSize = MAX_SAFE_UPLOAD_CHUNK_SIZE;
        uploadProgressCallback = null;
        uploadCompleteCallback = null;
        pendingUpload = null;
    }

    // --- Utility ---

    private static void send(String action, JsonObject body) {
        ClientPlayNetworking.send(new GalleryC2SPayload(action, body.toString()));
    }

    private static JsonObject parse(String json) {
        try {
            JsonElement el = JsonParser.parseString(json);
            return el.isJsonObject() ? el.getAsJsonObject() : new JsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    private static void requestMissingThumbnails() {
        for (OnlinePhotoEntry entry : onlinePhotos) {
            requestThumbnailIfNeeded(entry);
        }
    }

    private static void requestThumbnailIfNeeded(OnlinePhotoEntry entry) {
        if (entry == null || entry.photoId().isEmpty()) {
            return;
        }

        if (entry.thumbnailPath() != null && Files.exists(entry.thumbnailPath())) {
            return;
        }

        Path reusablePath = resolveReusablePhotoPath(entry.photoId(), entry.localCachePath());
        if (reusablePath != null) {
            updateOnlineEntryCache(entry.photoId(), reusablePath, reusablePath);
            return;
        }

        Path cachePath = getThumbnailCacheDir().resolve(entry.photoId() + ".png");
        if (Files.exists(cachePath)) {
            updateOnlineEntryCache(entry.photoId(), cachePath, null);
            return;
        }

        String photoId = entry.photoId();
        String downloadKey = downloadKey(photoId, true);
        if (downloadSessions.containsKey(downloadKey) || !pendingThumbnailRequests.add(photoId)) {
            return;
        }

        downloadSessions.put(downloadKey, new DownloadSession(cachePath, null, null));

        JsonObject body = new JsonObject();
        body.addProperty("photoId", photoId);
        body.addProperty("thumbnail", true);
        send("download_start", body);
    }

    private static void updateOnlineEntryCache(String photoId, Path thumbnailPath, Path localCachePath) {
        for (int i = 0; i < onlinePhotos.size(); i++) {
            OnlinePhotoEntry entry = onlinePhotos.get(i);
            if (!entry.photoId().equals(photoId)) {
                continue;
            }

            Path nextThumbnailPath = thumbnailPath != null ? thumbnailPath : entry.thumbnailPath();
            Path nextLocalCachePath = localCachePath != null ? localCachePath : entry.localCachePath();
            onlinePhotos.set(i, new OnlinePhotoEntry(
                    entry.photoId(),
                    entry.uploaderUuid(),
                    entry.uploaderName(),
                    nextThumbnailPath,
                    nextLocalCachePath,
                    entry.metadata()
            ));
            newData = true;
            return;
        }
    }

    private static Path resolveReusablePhotoPath(String photoId, Path candidatePath) {
        Path recentUploadPath = recentUploadedPhotoSources.get(photoId);
        if (matchesPhotoHash(recentUploadPath, photoId)) {
            return recentUploadPath;
        }

        if (matchesPhotoHash(candidatePath, photoId)) {
            return candidatePath;
        }

        Path localPhotoPath = PhotoManager.findLocalPhotoByHash(photoId);
        if (matchesPhotoHash(localPhotoPath, photoId)) {
            return localPhotoPath;
        }

        Path fullCachePath = getFullCacheDir().resolve(photoId + ".png");
        if (Files.exists(fullCachePath)) {
            return fullCachePath;
        }

        return null;
    }

    private static boolean matchesPhotoHash(Path photoPath, String expectedHash) {
        return photoPath != null
                && Files.exists(photoPath)
                && expectedHash.equals(PhotoManager.computeFileHash(photoPath));
    }

    private static String downloadKey(String photoId, boolean thumbnail) {
        return (thumbnail ? "thumb:" : "full:") + photoId;
    }

    private static Path getOnlineCacheDir() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.runDirectory.toPath().resolve("tzzphotos").resolve("online_cache");
    }

    private static Path getFullCacheDir() {
        return getOnlineCacheDir().resolve("full");
    }

    private static Path getThumbnailCacheDir() {
        return getOnlineCacheDir().resolve("thumbs");
    }

    // --- Data types ---

    /**
     * Represents a photo entry from the online gallery.
     */
    public record OnlinePhotoEntry(
            String photoId,
            String uploaderUuid,
            String uploaderName,
            Path thumbnailPath,
            Path localCachePath,
            PhotoMetadata metadata
    ) {}

    private static class DownloadSession {
        final Path cachePath;
        final Consumer<Float> progressCallback;
        final Consumer<Path> completeCallback;
        final ByteArrayOutputStream data = new ByteArrayOutputStream();

        DownloadSession(Path cachePath, Consumer<Float> progressCallback, Consumer<Path> completeCallback) {
            this.cachePath = cachePath;
            this.progressCallback = progressCallback;
            this.completeCallback = completeCallback;
        }
    }

    private static class PendingUpload {
        final String photoId;
        final byte[] fileData;
        boolean started;

        PendingUpload(String photoId, byte[] fileData) {
            this.photoId = photoId;
            this.fileData = fileData;
        }
    }
}
