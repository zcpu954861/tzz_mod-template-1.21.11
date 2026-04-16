package com.zcpu.tzzmod.phone.chat;

import net.minecraft.server.MinecraftServer;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PhoneChatImageStore {
    private static final String CHAT_IMAGE_DIR = "chat_images";
    private static final int THUMBNAIL_MAX_EDGE = 640;
    private static final long TARGET_UPLOAD_PIXELS = 1920L * 1080L;

    private PhoneChatImageStore() {
    }

    public static void ensureStorageReady(MinecraftServer server) throws IOException {
        Files.createDirectories(getFullDir(server));
        Files.createDirectories(getThumbDir(server));
    }

    public static boolean imageExists(MinecraftServer server, String imageId) {
        return Files.exists(getFullDir(server).resolve(imageId + ".png"));
    }

    public static StoredChatImage saveImage(MinecraftServer server, String imageId, byte[] sourceData) throws IOException {
        ensureStorageReady(server);

        Path fullPath = getFullDir(server).resolve(imageId + ".png");
        Path thumbPath = getThumbDir(server).resolve(imageId + ".png");
        if (Files.exists(fullPath)) {
            if (!Files.exists(thumbPath)) {
                Files.write(thumbPath, createThumbnailBytes(Files.readAllBytes(fullPath)));
            }
            int[] dims = readImageSize(fullPath);
            return new StoredChatImage(imageId, dims[0], dims[1], true);
        }

        ProcessedChatImage processed = processUploadedImage(sourceData);
        Files.write(fullPath, processed.fullImageData());
        Files.write(thumbPath, processed.thumbnailData());
        return new StoredChatImage(imageId, processed.width(), processed.height(), false);
    }

    public static Path resolveDownloadFile(MinecraftServer server, String imageId, boolean thumbnail) throws IOException {
        ensureStorageReady(server);

        if (!thumbnail) {
            return getFullDir(server).resolve(imageId + ".png");
        }

        Path thumbPath = getThumbDir(server).resolve(imageId + ".png");
        if (Files.exists(thumbPath)) {
            return thumbPath;
        }

        Path fullPath = getFullDir(server).resolve(imageId + ".png");
        if (!Files.exists(fullPath)) {
            return thumbPath;
        }

        Files.write(thumbPath, createThumbnailBytes(Files.readAllBytes(fullPath)));
        return thumbPath;
    }

    private static Path getRootDir(MinecraftServer server) {
        return server.getRunDirectory().resolve("tzzserverphotos").resolve(CHAT_IMAGE_DIR);
    }

    private static Path getFullDir(MinecraftServer server) {
        return getRootDir(server).resolve("full");
    }

    private static Path getThumbDir(MinecraftServer server) {
        return getRootDir(server).resolve("thumbs");
    }

    private static ProcessedChatImage processUploadedImage(byte[] sourceData) throws IOException {
        BufferedImage sourceImage = ImageIO.read(new ByteArrayInputStream(sourceData));
        if (sourceImage == null) {
            throw new IOException("Invalid chat image data");
        }

        int[] fullDims = scaleToMaxPixels(sourceImage.getWidth(), sourceImage.getHeight(), TARGET_UPLOAD_PIXELS);
        BufferedImage fullImage = fullDims[0] == sourceImage.getWidth() && fullDims[1] == sourceImage.getHeight()
                ? sourceImage
                : scaleImage(sourceImage, fullDims[0], fullDims[1]);

        int[] thumbDims = scaleToFit(fullImage.getWidth(), fullImage.getHeight(), THUMBNAIL_MAX_EDGE, THUMBNAIL_MAX_EDGE);
        BufferedImage thumbImage = thumbDims[0] == fullImage.getWidth() && thumbDims[1] == fullImage.getHeight()
                ? fullImage
                : scaleImage(fullImage, thumbDims[0], thumbDims[1]);

        return new ProcessedChatImage(encodePng(fullImage), encodePng(thumbImage), fullImage.getWidth(), fullImage.getHeight());
    }

    private static byte[] createThumbnailBytes(byte[] sourceData) throws IOException {
        BufferedImage sourceImage = ImageIO.read(new ByteArrayInputStream(sourceData));
        if (sourceImage == null) {
            throw new IOException("Invalid chat image data");
        }

        int[] thumbDims = scaleToFit(sourceImage.getWidth(), sourceImage.getHeight(), THUMBNAIL_MAX_EDGE, THUMBNAIL_MAX_EDGE);
        BufferedImage thumbImage = thumbDims[0] == sourceImage.getWidth() && thumbDims[1] == sourceImage.getHeight()
                ? sourceImage
                : scaleImage(sourceImage, thumbDims[0], thumbDims[1]);
        return encodePng(thumbImage);
    }

    private static int[] readImageSize(Path imagePath) throws IOException {
        BufferedImage image = ImageIO.read(imagePath.toFile());
        if (image == null) {
            throw new IOException("Invalid stored chat image");
        }
        return new int[]{image.getWidth(), image.getHeight()};
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

    private record ProcessedChatImage(byte[] fullImageData, byte[] thumbnailData, int width, int height) {
    }

    public record StoredChatImage(String imageId, int width, int height, boolean reused) {
    }
}