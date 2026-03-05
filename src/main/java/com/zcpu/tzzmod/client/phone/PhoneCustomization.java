package com.zcpu.tzzmod.client.phone;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.Tzz_mod;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class PhoneCustomization {
    private static final Identifier THEME_CONFIG = Identifier.of(Tzz_mod.MOD_ID, "phone/theme.json");
    private static final Identifier APPS_CONFIG = Identifier.of(Tzz_mod.MOD_ID, "phone/apps.json");

    private PhoneCustomization() {
    }

    public static Identifier resolveWallpaper(ResourceManager resourceManager, Identifier fallback) {
        Optional<Resource> optionalResource = resourceManager.getResource(THEME_CONFIG);
        if (optionalResource.isEmpty()) {
            return fallback;
        }

        try (InputStreamReader reader = new InputStreamReader(optionalResource.get().getInputStream(), StandardCharsets.UTF_8)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            if (!jsonObject.has("wallpaper")) {
                return fallback;
            }
            return Identifier.of(jsonObject.get("wallpaper").getAsString());
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to load phone theme config: {}", exception.getMessage());
            return fallback;
        }
    }

    public static Map<String, Identifier> resolveAppIconOverrides(ResourceManager resourceManager) {
        Optional<Resource> optionalResource = resourceManager.getResource(APPS_CONFIG);
        if (optionalResource.isEmpty()) {
            return Map.of();
        }

        try (InputStreamReader reader = new InputStreamReader(optionalResource.get().getInputStream(), StandardCharsets.UTF_8)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            if (!jsonObject.has("icons") || !jsonObject.get("icons").isJsonObject()) {
                return Map.of();
            }

            Map<String, Identifier> overrides = new HashMap<>();
            JsonObject icons = jsonObject.getAsJsonObject("icons");
            for (Map.Entry<String, JsonElement> entry : icons.entrySet()) {
                overrides.put(entry.getKey(), Identifier.of(entry.getValue().getAsString()));
            }
            return overrides;
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to load phone app config: {}", exception.getMessage());
            return Map.of();
        }
    }
}

