package com.zcpu.tzzmod.password;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.ModBlock.ModBlocks;
import com.zcpu.tzzmod.ModBlock.custom.PasswordMachineBlock;
import com.zcpu.tzzmod.ModBlock.entity.PasswordMachineBlockEntity;
import com.zcpu.tzzmod.ModItem.ModItems;
import com.zcpu.tzzmod.ModItem.custom.PasswordConfigCardItem;
import com.zcpu.tzzmod.network.PasswordC2SPayload;
import com.zcpu.tzzmod.network.PasswordS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public final class PasswordServer {
    private PasswordServer() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(PasswordC2SPayload.ID, (payload, context) ->
                context.server().execute(() -> handlePayload(context.player(), payload))
        );
    }

    private static void handlePayload(ServerPlayerEntity player, PasswordC2SPayload payload) {
        JsonObject body = parse(payload.bodyJson());
        switch (payload.action()) {
            case "attempt_machine" -> handleMachineAttempt(player, body);
            case "save_card" -> handleSaveCard(player, body);
            default -> sendMessage(player, "machine_result", false, Text.literal("Unknown password action: " + payload.action()), false);
        }
    }

    private static void handleMachineAttempt(ServerPlayerEntity player, JsonObject body) {
        BlockPos pos = getBlockPos(body);
        String code = PasswordCodeUtil.normalize(getString(body, "code"));
        ServerWorld world = player.getCommandSource().getWorld();

        if (!world.isInBuildLimit(pos) || player.squaredDistanceTo(pos.toCenterPos()) > 64.0D) {
            sendMessage(player, "machine_result", false, Text.translatable("block.tzz_mod.password_machine.too_far"), false);
            return;
        }
        if (!world.getBlockState(pos).isOf(ModBlocks.PASSWORD_MACHINE)) {
            sendMessage(player, "machine_result", false, Text.translatable("block.tzz_mod.password_machine.missing"), false);
            return;
        }
        if (!(world.getBlockEntity(pos) instanceof PasswordMachineBlockEntity blockEntity)) {
            sendMessage(player, "machine_result", false, Text.translatable("block.tzz_mod.password_machine.missing"), false);
            return;
        }

        if (blockEntity.matches(code)) {
            PasswordMachineBlock.activate(world, pos, world.getBlockState(pos));
            world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.BLOCKS, 1.0F, 1.25F);
            player.sendMessage(Text.translatable("block.tzz_mod.password_machine.success"), true);
            sendMessage(player, "machine_result", true, Text.translatable("block.tzz_mod.password_machine.success"), true);
            return;
        }

        world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.BLOCKS, 0.9F, 0.85F);
        player.sendMessage(Text.translatable("block.tzz_mod.password_machine.failed"), true);
        sendMessage(player, "machine_result", false, Text.translatable("block.tzz_mod.password_machine.failed"), false);
    }

    private static void handleSaveCard(ServerPlayerEntity player, JsonObject body) {
        Hand hand = "off_hand".equals(getString(body, "hand")) ? Hand.OFF_HAND : Hand.MAIN_HAND;
        String code = PasswordCodeUtil.normalize(getString(body, "code"));
        ItemStack stack = player.getStackInHand(hand);
        ServerWorld world = player.getCommandSource().getWorld();

        if (!stack.isOf(ModItems.PASSWORD_CONFIG_CARD)) {
            sendMessage(player, "card_saved", false, Text.translatable("item.tzz_mod.password_config_card.not_holding"), false);
            return;
        }

        PasswordConfigCardItem.setStoredPassword(stack, code);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.75F, 1.25F);
        player.sendMessage(Text.translatable("item.tzz_mod.password_config_card.saved", code), true);
        sendMessage(player, "card_saved", true, Text.translatable("item.tzz_mod.password_config_card.saved", code), true);
    }

    private static void sendMessage(ServerPlayerEntity player, String action, boolean success, Text message, boolean closeScreen) {
        JsonObject body = new JsonObject();
        body.addProperty("success", success);
        body.addProperty("message", message.getString());
        body.addProperty("close", closeScreen);
        ServerPlayNetworking.send(player, new PasswordS2CPayload(action, body.toString()));
    }

    private static JsonObject parse(String body) {
        try {
            if (body == null || body.isBlank()) {
                return new JsonObject();
            }
            return JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception ignored) {
            return new JsonObject();
        }
    }

    private static String getString(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static BlockPos getBlockPos(JsonObject object) {
        return new BlockPos(getInt(object, "x"), getInt(object, "y"), getInt(object, "z"));
    }

    private static int getInt(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            return 0;
        }
        try {
            return object.get(key).getAsInt();
        } catch (Exception ignored) {
            return 0;
        }
    }
}

