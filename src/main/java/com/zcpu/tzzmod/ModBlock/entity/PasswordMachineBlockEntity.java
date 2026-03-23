package com.zcpu.tzzmod.ModBlock.entity;

import com.zcpu.tzzmod.ModBlock.ModBlockEntities;
import com.zcpu.tzzmod.password.PasswordCodeUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;

public class PasswordMachineBlockEntity extends BlockEntity {
    private static final String PASSWORD_KEY = "PasswordCode";

    private String passwordCode = PasswordCodeUtil.DEFAULT_CODE;

    public PasswordMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PASSWORD_MACHINE, pos, state);
    }


    public boolean matches(String inputCode) {
        return passwordCode.equals(PasswordCodeUtil.normalize(inputCode));
    }

    public void setPasswordCode(String passwordCode) {
        this.passwordCode = PasswordCodeUtil.normalize(passwordCode);
        markDirty();
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        passwordCode = PasswordCodeUtil.normalize(view.getString(PASSWORD_KEY, PasswordCodeUtil.DEFAULT_CODE));
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putString(PASSWORD_KEY, passwordCode);
    }
}

