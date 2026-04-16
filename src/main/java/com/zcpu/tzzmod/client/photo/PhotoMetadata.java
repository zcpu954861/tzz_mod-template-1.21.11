package com.zcpu.tzzmod.client.photo;

/**
 * Metadata embedded in photo files to identify them as mod-captured photos.
 */
public record PhotoMetadata(
        String modTag,          // "tzz_mod_photo" — verifies this is a mod-captured photo
        long captureTimeMs,     // System.currentTimeMillis() at capture
        String worldId,         // World/server unique identifier
        String playerName,      // Player who captured the photo
        String playerUuid,      // Player UUID
        int imageWidth,
        int imageHeight
) {
    public static final String MOD_TAG = "tzz_mod_photo";
}
