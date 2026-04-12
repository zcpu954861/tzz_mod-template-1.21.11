package com.zcpu.tzzmod.client.phone.ui.state;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple client-side settings persistence for phone settings.
 */
public final class PhoneSettingsClient {
    private static final Path CONFIG_PATH = new File("run/config/tzz_mod/phone_settings.json").toPath();

    private static boolean alertMode = false;
    private static boolean alwaysShowRegionTitle = false;

    private PhoneSettingsClient() {
    }

    public static boolean isAlertModeEnabled() {
        return alertMode;
    }

    public static void setAlertModeEnabled(boolean enabled) {
        alertMode = enabled;
        save();
    }

    public static boolean isAlwaysShowRegionTitleEnabled() {
        return alwaysShowRegionTitle;
    }

    public static void setAlwaysShowRegionTitleEnabled(boolean enabled) {
        alwaysShowRegionTitle = enabled;
        save();
    }

    public static void load() {
        try {
            if (!Files.exists(CONFIG_PATH)) return;
            String content = Files.readString(CONFIG_PATH);
            JsonObject obj = JsonParser.parseString(content).getAsJsonObject();
            if (obj.has("alertMode")) {
                alertMode = obj.get("alertMode").getAsBoolean();
            }
            if (obj.has("alwaysShowRegionTitle")) {
                alwaysShowRegionTitle = obj.get("alwaysShowRegionTitle").getAsBoolean();
            }
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try {
            File parent = CONFIG_PATH.toFile().getParentFile();
            if (!parent.exists()) parent.mkdirs();
            JsonObject obj = new JsonObject();
            obj.add("alertMode", new JsonPrimitive(alertMode));
            obj.add("alwaysShowRegionTitle", new JsonPrimitive(alwaysShowRegionTitle));
            try (FileWriter fw = new FileWriter(CONFIG_PATH.toFile())) {
                fw.write(obj.toString());
            }
        } catch (Exception ignored) {
        }
    }
}

