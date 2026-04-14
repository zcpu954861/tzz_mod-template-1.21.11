package com.zcpu.tzzmod.client.phone.ui.state;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple client-side settings persistence for phone settings.
 */
public final class PhoneSettingsClient {
    private static final Path CONFIG_PATH = new File("run/config/tzz_mod/phone_settings.json").toPath();
    private static final String PROFILES_KEY = "profiles";
    private static final String DEFAULT_PROFILE_KEY = "default";
    private static final boolean DEFAULT_ALERT_MODE = false;
    private static final boolean DEFAULT_ALWAYS_SHOW_REGION_TITLE = false;
    private static final boolean DEFAULT_ANIMATIONS_ENABLED = true;
    private static final boolean DEFAULT_EXPERIMENTAL_UI = false;
    private static final boolean DEFAULT_LIGHT_MODE = false;
    private static final boolean DEFAULT_AR_MASK_ENABLED = false;

    private static boolean alertMode = DEFAULT_ALERT_MODE;
    private static boolean alwaysShowRegionTitle = DEFAULT_ALWAYS_SHOW_REGION_TITLE;
    private static boolean animationsEnabled = DEFAULT_ANIMATIONS_ENABLED;
    private static boolean experimentalUi = DEFAULT_EXPERIMENTAL_UI;
    private static boolean lightMode = DEFAULT_LIGHT_MODE;
    private static boolean arMaskEnabled = DEFAULT_AR_MASK_ENABLED;
    private static String loadedProfileKey;

    private PhoneSettingsClient() {
    }

    public static boolean isAlertModeEnabled() {
        ensureLoadedForCurrentProfile();
        return alertMode;
    }

    public static void setAlertModeEnabled(boolean enabled) {
        ensureLoadedForCurrentProfile();
        alertMode = enabled;
        save();
    }

    public static boolean isAlwaysShowRegionTitleEnabled() {
        ensureLoadedForCurrentProfile();
        return alwaysShowRegionTitle;
    }

    public static void setAlwaysShowRegionTitleEnabled(boolean enabled) {
        ensureLoadedForCurrentProfile();
        alwaysShowRegionTitle = enabled;
        save();
    }

    public static boolean isAnimationsEnabled() {
        ensureLoadedForCurrentProfile();
        return animationsEnabled;
    }

    public static void setAnimationsEnabled(boolean enabled) {
        ensureLoadedForCurrentProfile();
        animationsEnabled = enabled;
        save();
    }

    public static boolean isExperimentalUiEnabled() {
        ensureLoadedForCurrentProfile();
        return experimentalUi;
    }

    public static void setExperimentalUiEnabled(boolean enabled) {
        ensureLoadedForCurrentProfile();
        experimentalUi = enabled;
        save();
    }

    public static boolean isLightModeEnabled() {
        ensureLoadedForCurrentProfile();
        return lightMode;
    }

    public static void setLightModeEnabled(boolean enabled) {
        ensureLoadedForCurrentProfile();
        lightMode = enabled;
        save();
    }

    public static boolean isARMaskEnabled() {
        ensureLoadedForCurrentProfile();
        return arMaskEnabled;
    }

    public static void setARMaskEnabled(boolean enabled) {
        ensureLoadedForCurrentProfile();
        arMaskEnabled = enabled;
        save();
    }

    public static void load() {
        loadForProfile(currentProfileKey());
    }

    private static void ensureLoadedForCurrentProfile() {
        String profileKey = currentProfileKey();
        if (loadedProfileKey == null || !loadedProfileKey.equals(profileKey)) {
            loadForProfile(profileKey);
        }
    }

    private static void loadForProfile(String profileKey) {
        resetToDefaults();
        loadedProfileKey = profileKey;
        try {
            JsonObject root = readConfigRoot();
            applySettings(root);
            if (root.has(PROFILES_KEY) && root.get(PROFILES_KEY).isJsonObject()) {
                JsonObject profiles = root.getAsJsonObject(PROFILES_KEY);
                if (profiles.has(profileKey) && profiles.get(profileKey).isJsonObject()) {
                    applySettings(profiles.getAsJsonObject(profileKey));
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try {
            String profileKey = currentProfileKey();
            loadedProfileKey = profileKey;
            File parent = CONFIG_PATH.toFile().getParentFile();
            if (!parent.exists()) parent.mkdirs();

            JsonObject obj = readConfigRoot();
            // Do NOT write to root — only write to the per-player profile section
            // to avoid overwriting another client's settings when sharing run/

            JsonObject profiles = obj.has(PROFILES_KEY) && obj.get(PROFILES_KEY).isJsonObject()
                    ? obj.getAsJsonObject(PROFILES_KEY)
                    : new JsonObject();
            JsonObject profile = new JsonObject();
            writeSettings(profile);
            profiles.add(profileKey, profile);
            obj.add(PROFILES_KEY, profiles);

            try (FileWriter fw = new FileWriter(CONFIG_PATH.toFile())) {
                fw.write(obj.toString());
            }
        } catch (Exception ignored) {
        }
    }

    private static JsonObject readConfigRoot() throws Exception {
        if (!Files.exists(CONFIG_PATH)) {
            return new JsonObject();
        }

        String content = Files.readString(CONFIG_PATH);
        if (content == null || content.isBlank()) {
            return new JsonObject();
        }
        return JsonParser.parseString(content).getAsJsonObject();
    }

    private static void applySettings(JsonObject obj) {
        if (obj.has("alertMode")) {
            alertMode = obj.get("alertMode").getAsBoolean();
        }
        if (obj.has("alwaysShowRegionTitle")) {
            alwaysShowRegionTitle = obj.get("alwaysShowRegionTitle").getAsBoolean();
        }
        if (obj.has("animationsEnabled")) {
            animationsEnabled = obj.get("animationsEnabled").getAsBoolean();
        }
        if (obj.has("experimentalUi")) {
            experimentalUi = obj.get("experimentalUi").getAsBoolean();
        }
        if (obj.has("lightMode")) {
            lightMode = obj.get("lightMode").getAsBoolean();
        }
        if (obj.has("arMaskEnabled")) {
            arMaskEnabled = obj.get("arMaskEnabled").getAsBoolean();
        }
    }

    private static void writeSettings(JsonObject obj) {
        obj.add("alertMode", new JsonPrimitive(alertMode));
        obj.add("alwaysShowRegionTitle", new JsonPrimitive(alwaysShowRegionTitle));
        obj.add("animationsEnabled", new JsonPrimitive(animationsEnabled));
        obj.add("experimentalUi", new JsonPrimitive(experimentalUi));
        obj.add("lightMode", new JsonPrimitive(lightMode));
        obj.add("arMaskEnabled", new JsonPrimitive(arMaskEnabled));
    }

    private static void resetToDefaults() {
        alertMode = DEFAULT_ALERT_MODE;
        alwaysShowRegionTitle = DEFAULT_ALWAYS_SHOW_REGION_TITLE;
        animationsEnabled = DEFAULT_ANIMATIONS_ENABLED;
        experimentalUi = DEFAULT_EXPERIMENTAL_UI;
        lightMode = DEFAULT_LIGHT_MODE;
        arMaskEnabled = DEFAULT_AR_MASK_ENABLED;
    }

    private static String currentProfileKey() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            return client.player.getUuidAsString();
        }
        return DEFAULT_PROFILE_KEY;
    }
}

