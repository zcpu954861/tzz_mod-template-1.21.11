package com.zcpu.tzzmod.blocking;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class BlockingCardConfiguratorState {
    private static final String KEY_ITEM_ID = "tzz_blocking_configurator_item";
    private static final String KEY_ITEM_COUNT = "tzz_blocking_configurator_count";
    private static final String KEY_ACTIVATION_TYPE = "tzz_blocking_configurator_activation_type";
    private static final String KEY_ACTIVATION_INPUT = "tzz_blocking_configurator_activation_input";
    private static final String KEY_COMMAND = "tzz_blocking_configurator_command";
    private static final String KEY_NOTIFY_OPS = "tzz_blocking_configurator_notify_ops";

    private BlockingCardConfiguratorState() {
    }

    public static StoredCards read(ItemStack stack) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        return new StoredCards(
                nbt.getString(KEY_ITEM_ID).orElse(""),
                nbt.getInt(KEY_ITEM_COUNT).orElse(0),
                new BlockingCardConfig.Data(
                        BlockingCardConfig.ActivationType.fromId(nbt.getString(KEY_ACTIVATION_TYPE).orElse("")),
                        nbt.getString(KEY_ACTIVATION_INPUT).orElse(""),
                        BlockingCardConfig.normalizeCommand(nbt.getString(KEY_COMMAND).orElse("")),
                        nbt.getBoolean(KEY_NOTIFY_OPS).orElse(false)
                )
        );
    }

    public static boolean hasStoredCards(ItemStack stack) {
        return read(stack).isPresent();
    }

    public static void store(ItemStack configurator, ItemStack blockingCards) {
        Identifier itemId = Registries.ITEM.getId(blockingCards.getItem());
        write(
                configurator,
                itemId == null ? "" : itemId.toString(),
                blockingCards.getCount(),
                BlockingCardConfig.read(blockingCards)
        );
    }

    public static void updateConfiguration(ItemStack configurator, BlockingCardConfig.Data data) {
        StoredCards storedCards = read(configurator);
        if (!storedCards.isPresent()) {
            return;
        }
        write(configurator, storedCards.itemId(), storedCards.count(), data);
    }

    public static ItemStack extract(ItemStack configurator) {
        StoredCards storedCards = read(configurator);
        ItemStack result = extractPreview(storedCards);
        if (result.isEmpty()) {
            clear(configurator);
            return ItemStack.EMPTY;
        }
        clear(configurator);
        return result;
    }

    public static ItemStack extractPreview(StoredCards storedCards) {
        if (storedCards == null || !storedCards.isPresent()) {
            return ItemStack.EMPTY;
        }

        Identifier itemId = Identifier.tryParse(storedCards.itemId());
        if (itemId == null || !Registries.ITEM.containsId(itemId)) {
            return ItemStack.EMPTY;
        }

        Item item = Registries.ITEM.get(itemId);
        ItemStack result = new ItemStack(item, storedCards.count());
        BlockingCardConfig.write(result, storedCards.config());
        return result;
    }

    public static void clear(ItemStack configurator) {
        NbtCompound nbt = configurator.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        nbt.remove(KEY_ITEM_ID);
        nbt.remove(KEY_ITEM_COUNT);
        nbt.remove(KEY_ACTIVATION_TYPE);
        nbt.remove(KEY_ACTIVATION_INPUT);
        nbt.remove(KEY_COMMAND);
        nbt.remove(KEY_NOTIFY_OPS);
        configurator.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    private static void write(ItemStack configurator, String itemId, int count, BlockingCardConfig.Data data) {
        NbtCompound nbt = configurator.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (itemId == null || itemId.isBlank() || count <= 0) {
            clear(configurator);
            return;
        }

        nbt.putString(KEY_ITEM_ID, itemId);
        nbt.putInt(KEY_ITEM_COUNT, Math.max(1, count));
        nbt.putString(KEY_ACTIVATION_TYPE, data.activationType().id());
        if (data.activationInput().isBlank()) {
            nbt.remove(KEY_ACTIVATION_INPUT);
        } else {
            nbt.putString(KEY_ACTIVATION_INPUT, data.activationInput().trim());
        }
        if (data.command().isBlank()) {
            nbt.remove(KEY_COMMAND);
        } else {
            nbt.putString(KEY_COMMAND, BlockingCardConfig.normalizeCommand(data.command()));
        }
        if (data.notifyOps()) {
            nbt.putBoolean(KEY_NOTIFY_OPS, true);
        } else {
            nbt.remove(KEY_NOTIFY_OPS);
        }
        configurator.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    public record StoredCards(String itemId, int count, BlockingCardConfig.Data config) {
        public boolean isPresent() {
            return !itemId.isBlank() && count > 0;
        }
    }
}