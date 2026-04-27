package com.zcpu.tzzmod.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ResourceIntegrityTest {
    private static final String MOD_ID = "tzz_mod";
    private static final Path RESOURCES_ASSETS = Path.of("src/main/resources/assets", MOD_ID);
    private static final Path GENERATED_ASSETS = Path.of("src/main/generated/assets", MOD_ID);
    private static final List<Path> ASSET_ROOTS = List.of(RESOURCES_ASSETS, GENERATED_ASSETS);

    private static final List<String> CRITICAL_ITEMS = List.of(
            "phone",
            "ar_headset",
            "attention",
            "map_marker",
            "region_planner",
            "task_configurator",
            "password_config_card",
            "blocking_card_configurator",
            "white_blocking_card",
            "light_gray_blocking_card",
            "gray_blocking_card",
            "black_blocking_card",
            "brown_blocking_card",
            "red_blocking_card",
            "orange_blocking_card",
            "yellow_blocking_card",
            "lime_blocking_card",
            "green_blocking_card",
            "cyan_blocking_card",
            "light_blue_blocking_card",
            "blue_blocking_card",
            "purple_blocking_card",
            "magenta_blocking_card",
            "pink_blocking_card",
            "app_icon_admin",
            "app_icon_call_admin",
            "app_icon_camera",
            "app_icon_chat",
            "app_icon_compass",
            "app_icon_gallery",
            "app_icon_map",
            "app_icon_notes",
            "app_icon_settings",
            "app_icon_task",
            "password_machine",
            "silent_sensor_plate",
            "signal_emitter",
            "signal_receiver",
            "action_relay",
            "catcher_chest"
    );

    private static final List<String> CRITICAL_BLOCKS = List.of(
            "password_machine",
            "silent_sensor_plate",
            "signal_emitter",
            "signal_receiver",
            "action_relay",
            "catcher_chest"
    );

    private ResourceIntegrityTest() {
    }

    public static void run() throws IOException {
        testLangJson();
        testNoUppercaseItemGroupKeyInCode();
        testCriticalItemsHaveDefinitions();
        testCriticalBlocksHaveBlockstates();
        testModelTextureReferencesExist();
        testItemDefinitionsReferenceExistingModels();
    }

    private static void testLangJson() throws IOException {
        JsonObject enUs = readJsonObject(RESOURCES_ASSETS.resolve("lang/en_us.json"));
        JsonObject zhCn = readJsonObject(RESOURCES_ASSETS.resolve("lang/zh_cn.json"));
        requireTranslation(enUs, "itemGroup.tzz_mod.main", "en_us item group main");
        requireTranslation(enUs, "itemGroup.tzz_mod.function", "en_us item group function");
        requireTranslation(zhCn, "itemGroup.tzz_mod.main", "zh_cn item group main");
        requireTranslation(zhCn, "itemGroup.tzz_mod.function", "zh_cn item group function");
    }

    private static void testNoUppercaseItemGroupKeyInCode() throws IOException {
        Path javaRoot = Path.of("src/main/java");
        if (!Files.exists(javaRoot)) {
            return;
        }
        try (var files = Files.walk(javaRoot)) {
            for (Path path : files.filter(Files::isRegularFile).toList()) {
                String text = Files.readString(path, StandardCharsets.UTF_8);
                if (text.contains("ItemGroup.tzz_mod")) {
                    throw new AssertionError("Uppercase ItemGroup translation key found in " + path);
                }
            }
        }
    }

    private static void testCriticalItemsHaveDefinitions() {
        for (String id : CRITICAL_ITEMS) {
            boolean hasItemDefinition = findAsset("items/" + id + ".json") != null;
            boolean hasItemModel = findAsset("models/item/" + id + ".json") != null;
            require(hasItemDefinition || hasItemModel, "Missing item definition/model for " + id);
        }
    }

    private static void testCriticalBlocksHaveBlockstates() {
        for (String id : CRITICAL_BLOCKS) {
            require(findAsset("blockstates/" + id + ".json") != null, "Missing blockstate for " + id);
        }
    }

    private static void testModelTextureReferencesExist() throws IOException {
        for (Path root : ASSET_ROOTS) {
            Path modelsRoot = root.resolve("models");
            if (!Files.exists(modelsRoot)) {
                continue;
            }
            try (var files = Files.walk(modelsRoot)) {
                for (Path path : files.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".json")).toList()) {
                    JsonObject model = readJsonObject(path);
                    checkModelParent(path, model);
                    checkModelTextures(path, model);
                }
            }
        }
    }

    private static void testItemDefinitionsReferenceExistingModels() throws IOException {
        for (Path root : ASSET_ROOTS) {
            Path itemsRoot = root.resolve("items");
            if (!Files.exists(itemsRoot)) {
                continue;
            }
            try (var files = Files.walk(itemsRoot)) {
                for (Path path : files.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".json")).toList()) {
                    JsonObject definition = readJsonObject(path);
                    if (!definition.has("model") || !definition.get("model").isJsonObject()) {
                        continue;
                    }
                    JsonObject modelObject = definition.getAsJsonObject("model");
                    if (!modelObject.has("model") || !modelObject.get("model").isJsonPrimitive()) {
                        continue;
                    }
                    String modelId = modelObject.get("model").getAsString();
                    if (modelId.startsWith(MOD_ID + ":")) {
                        requireModelExists(path, modelId);
                    }
                }
            }
        }
    }

    private static void checkModelParent(Path source, JsonObject model) {
        if (!model.has("parent") || !model.get("parent").isJsonPrimitive()) {
            return;
        }
        String parent = model.get("parent").getAsString();
        if (parent.startsWith(MOD_ID + ":")) {
            requireModelExists(source, parent);
        }
    }

    private static void checkModelTextures(Path source, JsonObject model) {
        if (!model.has("textures") || !model.get("textures").isJsonObject()) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : model.getAsJsonObject("textures").entrySet()) {
            if (!entry.getValue().isJsonPrimitive()) {
                continue;
            }
            String texture = entry.getValue().getAsString();
            if (texture.startsWith("#") || texture.startsWith("minecraft:")) {
                continue;
            }
            String normalized = texture.startsWith(MOD_ID + ":")
                    ? texture.substring((MOD_ID + ":").length())
                    : texture;
            require(findAsset("textures/" + normalized + ".png") != null,
                    "Missing texture " + texture + " referenced by " + source);
        }
    }

    private static void requireModelExists(Path source, String modelId) {
        String normalized = modelId.substring((MOD_ID + ":").length());
        require(findAsset("models/" + normalized + ".json") != null,
                "Missing model " + modelId + " referenced by " + source);
    }

    private static Path findAsset(String relative) {
        String normalized = relative.replace("/", java.io.File.separator);
        List<Path> checked = new ArrayList<>();
        for (Path root : ASSET_ROOTS) {
            Path candidate = root.resolve(normalized);
            checked.add(candidate);
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static JsonObject readJsonObject(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject()) {
                throw new AssertionError("Expected JSON object in " + path);
            }
            return element.getAsJsonObject();
        }
    }

    private static void requireTranslation(JsonObject lang, String key, String message) {
        require(lang.has(key) && lang.get(key).isJsonPrimitive() && !lang.get(key).getAsString().isBlank(),
                "Missing translation key " + key + " in " + message);
    }

    private static void require(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }
}
