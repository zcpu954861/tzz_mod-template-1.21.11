package com.zcpu.tzzmod.ModBlock;

import com.zcpu.tzzmod.ModBlock.entity.PasswordMachineBlockEntity;
import com.zcpu.tzzmod.ModBlock.entity.SilentSensorPlateBlockEntity;
import com.zcpu.tzzmod.ModBlock.entity.SignalEmitterBlockEntity;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.util.NullSafety;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.NonNull;

public final class ModBlockEntities {
    public static final BlockEntityType<PasswordMachineBlockEntity> PASSWORD_MACHINE = Registry.register(
                        blockEntityTypeRegistry(),
            Identifier.of(Tzz_mod.MOD_ID, "password_machine"),
            FabricBlockEntityTypeBuilder.create(PasswordMachineBlockEntity::new, ModBlocks.PASSWORD_MACHINE).build()
    );

    public static final BlockEntityType<SilentSensorPlateBlockEntity> SILENT_SENSOR_PLATE = Registry.register(
                        blockEntityTypeRegistry(),
            Identifier.of(Tzz_mod.MOD_ID, "silent_sensor_plate"),
            FabricBlockEntityTypeBuilder.create(SilentSensorPlateBlockEntity::new, ModBlocks.SILENT_SENSOR_PLATE).build()
    );

    public static final BlockEntityType<SignalEmitterBlockEntity> SIGNAL_EMITTER = Registry.register(
                        blockEntityTypeRegistry(),
            Identifier.of(Tzz_mod.MOD_ID, "signal_emitter"),
            FabricBlockEntityTypeBuilder.create(SignalEmitterBlockEntity::new, ModBlocks.SIGNAL_EMITTER).build()
    );

    private ModBlockEntities() {
    }

        @SuppressWarnings("unchecked")
        private static Registry<@NonNull BlockEntityType<?>> blockEntityTypeRegistry() {
                return (Registry<@NonNull BlockEntityType<?>>) (Registry<?>) NullSafety.requireNonNull(Registries.BLOCK_ENTITY_TYPE);
        }

    public static void init() {
    }
}

