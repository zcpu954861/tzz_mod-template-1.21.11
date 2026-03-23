package com.zcpu.tzzmod.ModBlock;

import com.zcpu.tzzmod.ModBlock.entity.PasswordMachineBlockEntity;
import com.zcpu.tzzmod.Tzz_mod;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlockEntities {
    public static final BlockEntityType<PasswordMachineBlockEntity> PASSWORD_MACHINE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(Tzz_mod.MOD_ID, "password_machine"),
            FabricBlockEntityTypeBuilder.create(PasswordMachineBlockEntity::new, ModBlocks.PASSWORD_MACHINE).build()
    );

    private ModBlockEntities() {
    }

    public static void init() {
    }
}

