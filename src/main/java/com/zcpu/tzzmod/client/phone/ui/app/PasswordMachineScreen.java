package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.password.PasswordClient;
import com.zcpu.tzzmod.password.PasswordCodeUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class PasswordMachineScreen extends AbstractPasswordPadScreen {
    private final BlockPos machinePos;

    public PasswordMachineScreen(Screen parent, BlockPos machinePos) {
        super(Text.translatable("block.tzz_mod.password_machine"), parent, "");
        this.machinePos = machinePos.toImmutable();
    }

    @Override
    protected void onConfirmCode(String code) {
        PasswordClient.submitMachineAttempt(machinePos, PasswordCodeUtil.normalize(code));
    }

    @Override
    protected String getSubtitleTranslationKey() {
        return "phone.tzz_mod.password.machine.subtitle";
    }

    @Override
    protected String getConfirmTranslationKey() {
        return "phone.tzz_mod.password.confirm";
    }
}

