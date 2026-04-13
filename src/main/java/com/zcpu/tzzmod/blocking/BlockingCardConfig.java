package com.zcpu.tzzmod.blocking;

import com.zcpu.tzzmod.ModItem.custom.BlockingCardItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public final class BlockingCardConfig {
    private static final String KEY_ACTIVATION_TYPE = "tzz_blocking_activation_type";
    private static final String KEY_ACTIVATION_INPUT = "tzz_blocking_activation_input";
    private static final String KEY_COMMAND = "tzz_blocking_command";
    private static final String KEY_NOTIFY_OPS = "tzz_blocking_notify_ops";

    private BlockingCardConfig() {
    }

    public static Data read(ItemStack stack) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        return new Data(
                ActivationType.fromId(nbt.getString(KEY_ACTIVATION_TYPE).orElse("")),
                nbt.getString(KEY_ACTIVATION_INPUT).orElse(""),
                normalizeCommand(nbt.getString(KEY_COMMAND).orElse("")),
                nbt.getBoolean(KEY_NOTIFY_OPS).orElse(false)
        );
    }

    public static void write(ItemStack stack, Data data) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (!data.isConfigured()) {
            clearKeys(nbt);
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
            return;
        }

        nbt.putString(KEY_ACTIVATION_TYPE, data.activationType().id());
        if (data.activationType() == ActivationType.DISABLED || data.activationInput().isBlank()) {
            nbt.remove(KEY_ACTIVATION_INPUT);
        } else {
            nbt.putString(KEY_ACTIVATION_INPUT, data.activationInput().trim());
        }
        nbt.putString(KEY_COMMAND, normalizeCommand(data.command()));
        if (data.notifyOps()) {
            nbt.putBoolean(KEY_NOTIFY_OPS, true);
        } else {
            nbt.remove(KEY_NOTIFY_OPS);
        }
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    public static boolean isBlockingCard(ItemStack stack) {
        return stack.getItem() instanceof BlockingCardItem;
    }

    public static String normalizeCommand(String command) {
        String trimmed = command == null ? "" : command.trim();
        if (trimmed.startsWith("/")) {
            return trimmed.substring(1).trim();
        }
        return trimmed;
    }

    private static void clearKeys(NbtCompound nbt) {
        nbt.remove(KEY_ACTIVATION_TYPE);
        nbt.remove(KEY_ACTIVATION_INPUT);
        nbt.remove(KEY_COMMAND);
        nbt.remove(KEY_NOTIFY_OPS);
    }

    public enum ActivationType {
        ENTITY("entity"),
        BLOCK("block"),
        DISABLED("disabled");

        private final String id;

        ActivationType(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public static ActivationType fromId(String raw) {
            for (ActivationType value : values()) {
                if (value.id.equalsIgnoreCase(raw)) {
                    return value;
                }
            }
            return ENTITY;
        }
    }

    public record Data(ActivationType activationType, String activationInput, String command, boolean notifyOps) {
        public static final Data EMPTY = new Data(ActivationType.ENTITY, "", "", false);

        public boolean isConfigured() {
            return !BlockingCardConfig.normalizeCommand(command).isBlank();
        }
    }
}