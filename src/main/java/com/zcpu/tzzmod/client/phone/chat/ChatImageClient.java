package com.zcpu.tzzmod.client.phone.chat;

import com.google.gson.JsonObject;
import com.zcpu.tzzmod.client.photo.PhotoManager;
import com.zcpu.tzzmod.network.PhoneChatC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ChatImageClient {
    private static final int MAX_SAFE_UPLOAD_CHUNK_SIZE = 12_000;
    private static final long TRANSFER_INTERVAL_MS = 50L;

    private static final Map<String, DownloadSession> downloadSessions = new HashMap<>();
    private static final Set<String> pendingThumbnailRequests = Collections.synchronizedSet(new HashSet<>());
    private static final Map<String, Path> recentUploadedPhotoSources = new ConcurrentHashMap<>();

    private static int uploadChunkSize = MAX_SAFE_UPLOAD_CHUNK_SIZE;
    private static PendingUpload pendingUpload;
    private static Consumer<Float> uploadProgressCallback;
    private static Consumer<Boolean> uploadCompleteCallback;

    private ChatImageClient() {
    }

    public static void reset() {
        synchronized (downloadSessions) {
            downloadSessions.clear();
        }
        pendingThumbnailRequests.clear();
        recentUploadedPhotoSources.clear();
        uploadChunkSize = MAX_SAFE_UPLOAD_CHUNK_SIZE;
        pendingUpload = null;
        uploadProgressCallback = null;
        uploadCompleteCallback = null;
    }

    public static void handlePayload(String action, JsonObject body) {
        switch (action) {
            case "image_upload_ack" -> handleUploadAck(body);
            case "image_upload_complete" -> handleUploadComplete();
            case "image_download_data" -> handleDownloadData(body);
            case "image_download_complete" -> handleDownloadComplete(body);
            default -> {
            }
        }
    }

    public static void handleError(JsonObject body) {
        if (!getBoolean(body, "chatImage", false)) {
            return;
        }

        String imageId = getString(body, "imageId");
        boolean thumbnail = getBoolean(body, "thumbnail", false);
        if (getBoolean(body, "chatImageUpload", false)) {
            clearUploadState(false);
        }

        if (!imageId.isBlank()) {
            pendingThumbnailRequests.remove(imageId);
            DownloadSession session;
            synchronized (downloadSessions) {
                session = downloadSessions.remove(downloadKey(imageId, thumbnail));
            }
            if (session != null && session.completeCallback != null) {
                session.completeCallback.accept(null);
            }
        }
    }

    public static void abortUpload() {
        clearUploadState(false);
    }

    public static void uploadImageMessage(String type, String targetId, Path photoPath,
                                          Consumer<Float> progressCallback,
                                          Consumer<Boolean> completeCallback) {
        if (pendingUpload != null || uploadCompleteCallback != null || photoPath == null || !Files.exists(photoPath)) {
            if (completeCallback != null) {
                completeCallback.accept(false);
            }
            return;
        }

        uploadProgressCallback = progressCallback;
        uploadCompleteCallback = completeCallback;

        try {
            PhotoManager.PreparedUploadPhoto preparedPhoto = PhotoManager.preparePhotoForUpload(photoPath, PhotoManager.readMetadata(photoPath));
            String imageId = preparedPhoto.photoId();
            if (imageId.isBlank()) {
                clearUploadState(false);
                return;
            }

            recentUploadedPhotoSources.put(imageId, preparedPhoto.filePath());
            pendingUpload = new PendingUpload(imageId, preparedPhoto.data());

            JsonObject body = new JsonObject();
            body.addProperty("conversationType", type);
            body.addProperty("targetId", targetId);
            body.addProperty("imageId", imageId);
            body.addProperty("totalSize", preparedPhoto.data().length);
            body.addProperty("imageWidth", preparedPhoto.metadata().imageWidth());
            body.addProperty("imageHeight", preparedPhoto.metadata().imageHeight());
            send("image_upload_start", body);
        } catch (IOException exception) {
            clearUploadState(false);
        }
    }

    public static void requestThumbnailIfNeeded(PhoneChatClient.ChatMessageData message) {
        if (message == null || !message.isImage()) {
            return;
        }

        String imageId = message.imageId();
        if (imageId.isBlank() || getThumbnailSourcePath(imageId) != null) {
            return;
        }

        String key = downloadKey(imageId, true);
        synchronized (downloadSessions) {
            if (downloadSessions.containsKey(key) || !pendingThumbnailRequests.add(imageId)) {
                return;
            }

            Path cachePath = getThumbCacheDir().resolve(imageId + ".png");
            downloadSessions.put(key, new DownloadSession(cachePath, null, null));
        }

        JsonObject body = new JsonObject();
        body.addProperty("imageId", imageId);
        body.addProperty("thumbnail", true);
        send("image_download_start", body);
    }

    public static Path getThumbnailSourcePath(String imageId) {
        Path reusable = resolveReusableFullPath(imageId);
        if (reusable != null) {
            return reusable;
        }

        Path thumbPath = getThumbCacheDir().resolve(imageId + ".png");
        return Files.exists(thumbPath) ? thumbPath : null;
    }

    public static Path getFullImagePath(String imageId) {
        return resolveReusableFullPath(imageId);
    }

    public static void downloadFullImage(String imageId,
                                         Consumer<Float> progressCallback,
                                         Consumer<Path> completeCallback) {
        if (imageId == null || imageId.isBlank()) {
            if (completeCallback != null) {
                completeCallback.accept(null);
            }
            return;
        }

        Path reusable = resolveReusableFullPath(imageId);
        if (reusable != null) {
            if (completeCallback != null) {
                completeCallback.accept(reusable);
            }
            return;
        }

        Path cachePath = getFullCacheDir().resolve(imageId + ".png");
        if (Files.exists(cachePath)) {
            if (completeCallback != null) {
                completeCallback.accept(cachePath);
            }
            return;
        }

        String key = downloadKey(imageId, false);
        synchronized (downloadSessions) {
            if (downloadSessions.containsKey(key)) {
                return;
            }
            downloadSessions.put(key, new DownloadSession(cachePath, progressCallback, completeCallback));
        }

        JsonObject body = new JsonObject();
        body.addProperty("imageId", imageId);
        body.addProperty("thumbnail", false);
        send("image_download_start", body);
    }

    private static void handleUploadAck(JsonObject body) {
        if (body.has("chunkSize")) {
            uploadChunkSize = Math.max(1024, Math.min(MAX_SAFE_UPLOAD_CHUNK_SIZE, getInt(body, "chunkSize", MAX_SAFE_UPLOAD_CHUNK_SIZE)));
            if (pendingUpload != null && !pendingUpload.started) {
                pendingUpload.started = true;
                startUploadThread(pendingUpload);
            }
        }
        if (uploadProgressCallback != null && body.has("progress")) {
            uploadProgressCallback.accept(Math.max(0.0F, Math.min(1.0F, getFloat(body, "progress", 0.0F))));
        }
    }

    private static void handleUploadComplete() {
        clearUploadState(true);
    }

    private static void handleDownloadData(JsonObject body) {
        String imageId = getString(body, "imageId");
        boolean thumbnail = getBoolean(body, "thumbnail", false);
        DownloadSession session;
        synchronized (downloadSessions) {
            session = downloadSessions.get(downloadKey(imageId, thumbnail));
        }
        if (session == null) {
            return;
        }

        String data = getString(body, "data");
        if (!data.isBlank()) {
            byte[] bytes;
            try {
                bytes = Base64.getDecoder().decode(data);
            } catch (IllegalArgumentException exception) {
                pendingThumbnailRequests.remove(imageId);
                synchronized (downloadSessions) {
                    downloadSessions.remove(downloadKey(imageId, thumbnail));
                }
                if (session.completeCallback != null) {
                    session.completeCallback.accept(null);
                }
                return;
            }
            session.data.write(bytes, 0, bytes.length);
        }

        if (session.progressCallback != null) {
            session.progressCallback.accept(getFloat(body, "progress", 0.0F));
        }
    }

    private static void handleDownloadComplete(JsonObject body) {
        String imageId = getString(body, "imageId");
        boolean thumbnail = getBoolean(body, "thumbnail", false);
        pendingThumbnailRequests.remove(imageId);

        DownloadSession session;
        synchronized (downloadSessions) {
            session = downloadSessions.remove(downloadKey(imageId, thumbnail));
        }
        if (session == null) {
            return;
        }

        try {
            Files.createDirectories(session.cachePath.getParent());
            Files.write(session.cachePath, session.data.toByteArray());
            if (session.completeCallback != null) {
                session.completeCallback.accept(session.cachePath);
            }
        } catch (IOException exception) {
            if (session.completeCallback != null) {
                session.completeCallback.accept(null);
            }
        }
    }

    private static void startUploadThread(PendingUpload upload) {
        Thread uploadThread = new Thread(() -> {
            try {
                int offset = 0;
                while (offset < upload.fileData.length) {
                    int chunkSize = Math.max(1024, Math.min(MAX_SAFE_UPLOAD_CHUNK_SIZE, uploadChunkSize));
                    int end = Math.min(offset + chunkSize, upload.fileData.length);
                    byte[] chunk = Arrays.copyOfRange(upload.fileData, offset, end);
                    String encoded = Base64.getEncoder().encodeToString(chunk);
                    int sentBytes = end;

                    JsonObject body = new JsonObject();
                    body.addProperty("imageId", upload.imageId);
                    body.addProperty("data", encoded);

                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client == null) {
                        throw new IllegalStateException("Missing minecraft client during chat image upload");
                    }

                    client.execute(() -> {
                        send("image_upload_chunk", body);
                        if (uploadProgressCallback != null) {
                            uploadProgressCallback.accept((float) sentBytes / upload.fileData.length);
                        }
                    });

                    offset = end;
                    if (offset < upload.fileData.length) {
                        Thread.sleep(TRANSFER_INTERVAL_MS);
                    }
                }

                MinecraftClient client = MinecraftClient.getInstance();
                if (client == null) {
                    throw new IllegalStateException("Missing minecraft client while finalizing chat image upload");
                }
                client.execute(() -> {
                    JsonObject finishBody = new JsonObject();
                    finishBody.addProperty("imageId", upload.imageId);
                    send("image_upload_finish", finishBody);
                });
            } catch (Exception exception) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null) {
                    client.execute(() -> clearUploadState(false));
                } else {
                    clearUploadState(false);
                }
            }
        }, "TzzMod-ChatImageUpload");
        uploadThread.setDaemon(true);
        uploadThread.start();
    }

    private static void clearUploadState(boolean success) {
        pendingUpload = null;
        if (uploadCompleteCallback != null) {
            uploadCompleteCallback.accept(success);
        }
        uploadProgressCallback = null;
        uploadCompleteCallback = null;
    }

    private static Path resolveReusableFullPath(String imageId) {
        Path recentPath = recentUploadedPhotoSources.get(imageId);
        if (matchesPhotoHash(recentPath, imageId)) {
            return recentPath;
        }

        Path localPhotoPath = PhotoManager.findLocalPhotoByHash(imageId);
        if (matchesPhotoHash(localPhotoPath, imageId)) {
            return localPhotoPath;
        }

        Path fullCachePath = getFullCacheDir().resolve(imageId + ".png");
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

    private static String downloadKey(String imageId, boolean thumbnail) {
        return (thumbnail ? "thumb:" : "full:") + imageId;
    }

    private static Path getCacheRoot() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return Path.of(".").resolve("tzzphotos").resolve("chat_cache").resolve("local");
        }
        return client.runDirectory.toPath().resolve("tzzphotos").resolve("chat_cache").resolve(PhotoManager.getServerIp());
    }

    private static Path getFullCacheDir() {
        return getCacheRoot().resolve("full");
    }

    private static Path getThumbCacheDir() {
        return getCacheRoot().resolve("thumbs");
    }

    private static void send(String action, JsonObject body) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getNetworkHandler() == null) {
            return;
        }
        ClientPlayNetworking.send(new PhoneChatC2SPayload(action, body.toString()));
    }

    private static String getString(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static boolean getBoolean(JsonObject object, String key, boolean fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        try {
            return object.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        try {
            return object.get(key).getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static float getFloat(JsonObject object, String key, float fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        try {
            return object.get(key).getAsFloat();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static final class PendingUpload {
        private final String imageId;
        private final byte[] fileData;
        private boolean started;

        private PendingUpload(String imageId, byte[] fileData) {
            this.imageId = imageId;
            this.fileData = fileData;
        }
    }

    private static final class DownloadSession {
        private final Path cachePath;
        private final Consumer<Float> progressCallback;
        private final Consumer<Path> completeCallback;
        private final ByteArrayOutputStream data = new ByteArrayOutputStream();

        private DownloadSession(Path cachePath,
                                Consumer<Float> progressCallback,
                                Consumer<Path> completeCallback) {
            this.cachePath = cachePath;
            this.progressCallback = progressCallback;
            this.completeCallback = completeCallback;
        }
    }
}