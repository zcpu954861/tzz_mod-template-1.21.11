package com.zcpu.tzzmod.client.phone;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import com.zcpu.tzzmod.client.phone.ui.app.MapAppScreen;
import com.zcpu.tzzmod.client.phone.ui.app.PhoneChatAppScreen;
import com.zcpu.tzzmod.client.phone.ui.app.PhoneCallAdminScreen;
import com.zcpu.tzzmod.client.phone.ui.app.PhoneTaskAppScreen;
import com.zcpu.tzzmod.client.phone.ui.app.PhoneSettingsAppScreen;
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

    private PhoneAppRegistry() {
    }

    public static List<PhoneAppEntry> getAppEntries() {
        MinecraftClient client = MinecraftClient.getInstance();
        Map<String, Identifier> iconOverrides = PhoneCustomization.resolveAppIconOverrides(client.getResourceManager());

        List<PhoneAppEntry> entries = new ArrayList<>();
        entries.add(new PhoneAppEntry(
                "map",
                Text.translatable("phone.tzz_mod.app.map"),
                iconOverrides.getOrDefault("map", MAP_ICON),
                MapAppScreen::new
        ));

        // Settings app (always available)
        entries.add(new PhoneAppEntry(
                "settings",
                Text.translatable("phone.tzz_mod.app.settings"),
                iconOverrides.getOrDefault("settings", SETTINGS_ICON),
                PhoneSettingsAppScreen::new
        ));

        if (PhoneChatClient.isEnabled()) {
            entries.add(new PhoneAppEntry(
                    "chat",
                    Text.translatable("phone.tzz_mod.app.chat"),
                    iconOverrides.getOrDefault("chat", CHAT_ICON),
                    PhoneChatAppScreen::new
            ));
        }

        entries.add(new PhoneAppEntry(
                "task",
                Text.translatable("phone.tzz_mod.app.task"),
                iconOverrides.getOrDefault("task", TASK_ICON),
                PhoneTaskAppScreen::new
        ));

        // Call Admin app (always available)
        entries.add(new PhoneAppEntry(
                "call_admin",
                Text.translatable("phone.tzz_mod.app.call_admin"),
                iconOverrides.getOrDefault("call_admin", CALL_ADMIN_ICON),
                PhoneCallAdminScreen::new
        ));

        return entries;
    }
}
