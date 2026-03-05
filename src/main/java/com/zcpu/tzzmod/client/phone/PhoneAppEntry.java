package com.zcpu.tzzmod.client.phone;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public record PhoneAppEntry(String id, Text name, Identifier iconTexture,
                            Function<Screen, Screen> rootScreenFactory) {
}

