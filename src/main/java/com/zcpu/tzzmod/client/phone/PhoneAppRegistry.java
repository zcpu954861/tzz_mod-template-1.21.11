package com.zcpu.tzzmod.client.phone;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import com.zcpu.tzzmod.client.phone.ui.app.MapAppScreen;
import com.zcpu.tzzmod.client.phone.ui.app.PhoneChatAppScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PhoneAppRegistry {
    private static final Identifier MAP_ICON = Identifier.of(Tzz_mod.MOD_ID, "textures/gui/phone/icons/map.png");
    private static final Identifier CHAT_ICON = Identifier.of(Tzz_mod.MOD_ID, "textures/gui/phone/icons/chat.png");

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

        if (PhoneChatClient.isEnabled()) {
            entries.add(new PhoneAppEntry(
                    "chat",
                    Text.translatable("phone.tzz_mod.app.chat"),
                    iconOverrides.getOrDefault("chat", CHAT_ICON),
                    PhoneChatAppScreen::new
            ));
        }

        return entries;
    }
}
