package com.zcpu.tzzmod.client.phone;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import com.zcpu.tzzmod.client.phone.ui.app.MapAppScreen;
import com.zcpu.tzzmod.client.phone.ui.app.PhoneChatAppScreen;
import com.zcpu.tzzmod.client.phone.ui.app.PhoneCallAdminScreen;
import com.zcpu.tzzmod.client.phone.ui.app.PhoneTaskAppScreen;
import com.zcpu.tzzmod.client.phone.ui.app.PhoneSettingsAppScreen;
import com.zcpu.tzzmod.client.phone.ui.app.CompassAppScreen;
import com.zcpu.tzzmod.client.phone.ui.app.PhoneAdminAppScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PhoneAppRegistry {
    private static final Identifier MAP_ICON = Identifier.of(Tzz_mod.MOD_ID, "textures/gui/phone/icons/map.png");
    private static final Identifier CHAT_ICON = Identifier.of(Tzz_mod.MOD_ID, "textures/gui/phone/icons/chat.png");
    private static final Identifier CALL_ADMIN_ICON = Identifier.of(Tzz_mod.MOD_ID, "textures/gui/phone/icons/call_admin.png");
    private static final Identifier TASK_ICON = Identifier.of(Tzz_mod.MOD_ID, "textures/gui/phone/icons/task.png");
    private static final Identifier SETTINGS_ICON = Identifier.of(Tzz_mod.MOD_ID, "textures/gui/phone/icons/settings.png");
    private static final Identifier COMPASS_ICON = Identifier.of(Tzz_mod.MOD_ID, "textures/gui/phone/icons/compass.png");
    private static final Identifier ADMIN_ICON = Identifier.of(Tzz_mod.MOD_ID, "textures/gui/phone/icons/admin.png");

    private PhoneAppRegistry() {
    }

    public static List<PhoneAppEntry> getAppEntries() {
        MinecraftClient client = MinecraftClient.getInstance();
        Map<String, Identifier> iconOverrides = PhoneCustomization.resolveAppIconOverrides(client.getResourceManager());

        // Do not request bootstrap from here (this method is called frequently by the UI).
        // PhoneHomeScreen will request bootstrap when opened to keep network traffic bounded.

        // canonical list (ordered)
        List<PhoneAppEntry> canonical = new ArrayList<>();
        canonical.add(new PhoneAppEntry("map", Text.translatable("phone.tzz_mod.app.map"), iconOverrides.getOrDefault("map", MAP_ICON), MapAppScreen::new));
        canonical.add(new PhoneAppEntry("settings", Text.translatable("phone.tzz_mod.app.settings"), iconOverrides.getOrDefault("settings", SETTINGS_ICON), PhoneSettingsAppScreen::new));
        canonical.add(new PhoneAppEntry("chat", Text.translatable("phone.tzz_mod.app.chat"), iconOverrides.getOrDefault("chat", CHAT_ICON), PhoneChatAppScreen::new));
        canonical.add(new PhoneAppEntry("task", Text.translatable("phone.tzz_mod.app.task"), iconOverrides.getOrDefault("task", TASK_ICON), PhoneTaskAppScreen::new));
        canonical.add(new PhoneAppEntry("call_admin", Text.translatable("phone.tzz_mod.app.call_admin"), iconOverrides.getOrDefault("call_admin", CALL_ADMIN_ICON), PhoneCallAdminScreen::new));
        canonical.add(new PhoneAppEntry("compass", Text.translatable("phone.tzz_mod.app.compass"), iconOverrides.getOrDefault("compass", COMPASS_ICON), CompassAppScreen::new));
        canonical.add(new PhoneAppEntry("admin", Text.translatable("phone.tzz_mod.app.admin"), iconOverrides.getOrDefault("admin", ADMIN_ICON), PhoneAdminAppScreen::new));

        List<PhoneAppEntry> entries = new ArrayList<>();
        for (PhoneAppEntry e : canonical) {
            String vis = com.zcpu.tzzmod.client.phone.PhoneAppsClient.getVisibility(e.id());
            boolean allowed;
            switch (vis) {
                case "true" -> allowed = true;
                case "false" -> allowed = false;
                case "op" -> allowed = PhoneChatClient.isOp();
                default -> allowed = true;
            }
            if (allowed) entries.add(e);
        }

        return entries;
    }
}
