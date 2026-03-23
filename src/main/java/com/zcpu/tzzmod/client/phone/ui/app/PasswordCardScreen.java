package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.password.PasswordClient;
import com.zcpu.tzzmod.password.PasswordCodeUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

public class PasswordCardScreen extends AbstractPasswordPadScreen {
    private final Hand hand;

    public PasswordCardScreen(Screen parent, Hand hand, String initialCode) {
        super(Text.translatable("item.tzz_mod.password_config_card"), parent, PasswordCodeUtil.normalize(initialCode));
        this.hand = hand;
    }

    @Override
    protected void onConfirmCode(String code) {
        PasswordClient.saveCardCode(hand, PasswordCodeUtil.normalize(code));
    }

    @Override
    protected String getSubtitleTranslationKey() {
        return "phone.tzz_mod.password.card.subtitle";
    }

    @Override
    protected String getConfirmTranslationKey() {
        return "phone.tzz_mod.password.save";
    }
}

