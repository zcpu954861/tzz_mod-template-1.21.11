package com.zcpu.tzzmod.blocking;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.zcpu.tzzmod.ModItem.ModItems;
import com.zcpu.tzzmod.network.BlockingCardC2SPayload;
import com.zcpu.tzzmod.network.BlockingCardS2CPayload;
import com.zcpu.tzzmod.util.NullSafety;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.permission.PermissionPredicate;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Property;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BlockingCardServer {
    private BlockingCardServer() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(BlockingCardC2SPayload.ID, (payload, context) ->
                context.server().execute(() -> handlePayload(context.player(), payload))
        );
    }

    public static boolean matchesBlock(net.minecraft.world.World world, BlockPos pos, String input) {
        BlockConditionSpec spec = parseBlockCondition(input);
        if (spec == null) {
            return false;
        }

        BlockState state = world.getBlockState(pos);
        Identifier actualId = Registries.BLOCK.getId(state.getBlock());
        if (!spec.blockId().equals(actualId)) {
            return false;
        }

        for (Map.Entry<String, String> entry : spec.properties().entrySet()) {
            Property<?> property = state.getBlock().getStateManager().getProperty(entry.getKey());
            if (property == null || !propertyMatches(state, property, entry.getValue())) {
                return false;
            }
        }

        if (spec.nbt() == null) {
            return true;
        }

        var blockEntity = world.getBlockEntity(pos);
        if (blockEntity == null) {
            return false;
        }

        NbtCompound actualNbt = blockEntity.createNbtWithIdentifyingData(world.getRegistryManager());
        return NbtHelper.matches(spec.nbt(), actualNbt, true);
    }

    public static boolean looksLikeEntityMatch(Entity entity, String rawInput) {
        String trimmed = rawInput == null ? "" : rawInput.trim();
        if (trimmed.isBlank()) {
            return false;
        }
        if (trimmed.startsWith("@")) {
            return true;
        }

        int bracketIndex = trimmed.indexOf('[');
        String typeId = bracketIndex >= 0 ? trimmed.substring(0, bracketIndex).trim() : trimmed;
        Identifier expectedId = Identifier.tryParse(typeId);
        Identifier actualId = Registries.ENTITY_TYPE.getId(entity.getType());
        return expectedId != null && expectedId.equals(actualId);
    }

    public static ActionResult activateFromEntity(ServerPlayerEntity player, Hand hand, Entity entity, BlockingCardConfig.Data data) {
        if (!matchesEntity(player, entity, data.activationInput())) {
            return ActionResult.PASS;
        }
        return executeConfiguredCommand(player, hand, player.getEntityWorld(), entity.getEyePos(), data);
    }

    public static ActionResult activateFromBlock(ServerPlayerEntity player, Hand hand, BlockPos pos, BlockingCardConfig.Data data) {
        if (!matchesBlock(player.getEntityWorld(), pos, data.activationInput())) {
            return ActionResult.PASS;
        }
        return executeConfiguredCommand(player, hand, player.getEntityWorld(), Vec3d.ofCenter(pos), data);
    }

    private static void handlePayload(ServerPlayerEntity player, BlockingCardC2SPayload payload) {
        JsonObject body = parse(payload.bodyJson());
        if (!"save_config".equals(payload.action())) {
            sendResult(player, false, Text.literal("Unknown blocking card action: " + payload.action()));
            return;
        }
        handleSaveConfig(player, body);
    }

    private static void handleSaveConfig(ServerPlayerEntity player, JsonObject body) {
        Hand hand = "off_hand".equals(getString(body, "hand")) ? Hand.OFF_HAND : Hand.MAIN_HAND;
        ItemStack configurator = player.getStackInHand(hand);
        if (!configurator.isOf(ModItems.BLOCKING_CARD_CONFIGURATOR)) {
            sendResult(player, false, Text.translatable("item.tzz_mod.blocking_card_configurator.not_holding"));
            return;
        }

        BlockingCardConfiguratorState.StoredCards storedCards = BlockingCardConfiguratorState.read(configurator);
        if (!storedCards.isPresent()) {
            sendResult(player, false, Text.translatable("item.tzz_mod.blocking_card_configurator.no_cards_loaded"));
            return;
        }

        BlockingCardConfig.ActivationType activationType = BlockingCardConfig.ActivationType.fromId(getString(body, "activationType"));
        String activationInput = getString(body, "activationInput").trim();
        String command = BlockingCardConfig.normalizeCommand(getString(body, "command"));
        boolean notifyOps = getBoolean(body, "notifyOps", false);

        Text validationError = validateConfiguration(player, activationType, activationInput, command);
        if (validationError != null) {
            sendResult(player, false, validationError);
            return;
        }

        BlockingCardConfiguratorState.updateConfiguration(configurator, new BlockingCardConfig.Data(activationType, activationInput, command, notifyOps));
        player.getEntityWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.7F, 1.3F);
        sendResult(player, true, Text.translatable("item.tzz_mod.blocking_card_configurator.saved"));
    }

    private static Text validateConfiguration(ServerPlayerEntity player, BlockingCardConfig.ActivationType activationType, String activationInput, String command) {
        if (command.isBlank()) {
            return Text.translatable("item.tzz_mod.blocking_card_configurator.invalid_command");
        }
        if (!isCommandValid(player, command)) {
            return Text.translatable("item.tzz_mod.blocking_card_configurator.invalid_command");
        }
        if (activationType == BlockingCardConfig.ActivationType.ENTITY && !isEntityInputValid(activationInput)) {
            return Text.translatable("item.tzz_mod.blocking_card_configurator.invalid_entity_condition");
        }
        if (activationType == BlockingCardConfig.ActivationType.BLOCK && parseBlockCondition(activationInput) == null) {
            return Text.translatable("item.tzz_mod.blocking_card_configurator.invalid_block_condition");
        }
        return null;
    }

    private static boolean isCommandValid(ServerPlayerEntity player, String command) {
        try {
            ServerCommandSource source = player.getCommandSource()
                    .withPermissions(PermissionPredicate.ALL)
                    .withSilent();
            ParseResults<ServerCommandSource> parseResults = source.getServer().getCommandManager().getDispatcher().parse(
                    command,
                    source
            );
            return parseResults.getReader().getRemaining().isEmpty() && parseResults.getExceptions().isEmpty();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isEntityInputValid(String rawInput) {
        try {
            EntityArgumentType.entities().parse(new StringReader(normalizeEntitySelector(rawInput)));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean matchesEntity(ServerPlayerEntity player, Entity entity, String rawInput) {
        try {
            String selectorText = normalizeEntitySelector(rawInput);
            var selector = EntityArgumentType.entities().parse(new StringReader(selectorText));
            ServerCommandSource source = player.getCommandSource()
                    .withPermissions(PermissionPredicate.ALL)
                    .withSilent()
                    .withWorld(player.getEntityWorld())
                    .withPosition(entity.getEyePos());
            return selector.getEntities(source).stream().anyMatch(candidate -> candidate != null && candidate.getUuid().equals(entity.getUuid()));
        } catch (Exception ignored) {
            return false;
        }
    }

    private static ActionResult executeConfiguredCommand(ServerPlayerEntity player, Hand hand, ServerWorld world, Vec3d triggerPos, BlockingCardConfig.Data data) {
        String command = BlockingCardConfig.normalizeCommand(data.command());
        if (command.isBlank()) {
            return ActionResult.PASS;
        }

        try {
            ServerCommandSource source = player.getCommandSource()
                    .withPermissions(PermissionPredicate.ALL)
                    .withSilent()
                    .withWorld(world)
                    .withPosition(triggerPos);
            source.getServer().getCommandManager().getDispatcher().execute(command, source);
            world.playSound(null, BlockPos.ofFloored(triggerPos), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.65F, 1.2F);
            if (data.notifyOps()) {
                notifyOperators(player, player.getStackInHand(hand), command);
            }
            return ActionResult.CONSUME;
        } catch (Exception exception) {
            player.sendMessage(Text.translatable("item.tzz_mod.blocking_card.command_failed", exception.getMessage()), true);
            return ActionResult.CONSUME;
        }
    }

    private static void notifyOperators(ServerPlayerEntity player, ItemStack stack, String command) {
        MutableText message = Text.literal("[封锁卡] ").formatted(Formatting.GOLD)
                .append(Text.literal(player.getName().getString()).formatted(Formatting.YELLOW))
                .append(Text.literal(" 激活了 ").formatted(Formatting.GOLD))
                .append(Text.literal(stack.getName().getString()).formatted(Formatting.AQUA))
                .append(Text.literal(" -> /").formatted(Formatting.GOLD))
                .append(Text.literal(command).formatted(Formatting.GREEN));

        for (ServerPlayerEntity onlinePlayer : player.getCommandSource().getServer().getPlayerManager().getPlayerList()) {
            if (onlinePlayer.isCreativeLevelTwoOp()) {
                onlinePlayer.sendMessage(message, false);
            }
        }
    }

    private static String normalizeEntitySelector(String rawInput) {
        String trimmed = rawInput == null ? "" : rawInput.trim();
        if (trimmed.startsWith("@")) {
            return trimmed;
        }

        int bracketIndex = trimmed.indexOf('[');
        if (bracketIndex < 0) {
            return "@e[type=" + trimmed + "]";
        }

        int closeBracket = trimmed.lastIndexOf(']');
        if (closeBracket <= bracketIndex) {
            return trimmed;
        }

        String typeId = trimmed.substring(0, bracketIndex).trim();
        String selectorArgs = trimmed.substring(bracketIndex + 1, closeBracket).trim();
        if (selectorArgs.isBlank()) {
            return "@e[type=" + typeId + "]";
        }
        return "@e[type=" + typeId + "," + selectorArgs + "]";
    }

    private static BlockConditionSpec parseBlockCondition(String rawInput) {
        try {
            String trimmed = rawInput == null ? "" : rawInput.trim();
            if (trimmed.isBlank()) {
                return null;
            }

            int cursor = trimmed.length();
            int stateStart = trimmed.indexOf('[');
            int nbtStart = trimmed.indexOf('{');
            if (stateStart >= 0) {
                cursor = Math.min(cursor, stateStart);
            }
            if (nbtStart >= 0) {
                cursor = Math.min(cursor, nbtStart);
            }

            String blockIdText = trimmed.substring(0, cursor).trim();
            Identifier blockId = Identifier.tryParse(blockIdText);
            if (blockId == null || !Registries.BLOCK.containsId(blockId)) {
                return null;
            }

            Map<String, String> properties = new LinkedHashMap<>();
            NbtCompound nbt = null;
            int index = cursor;
            if (index < trimmed.length() && trimmed.charAt(index) == '[') {
                int close = trimmed.indexOf(']', index);
                if (close < 0) {
                    return null;
                }
                String propertyBody = trimmed.substring(index + 1, close).trim();
                if (!propertyBody.isBlank()) {
                    for (String token : propertyBody.split(",")) {
                        String[] pair = token.split("=", 2);
                        if (pair.length != 2) {
                            return null;
                        }
                        properties.put(pair[0].trim(), pair[1].trim());
                    }
                }
                index = close + 1;
            }

            if (index < trimmed.length()) {
                String nbtText = trimmed.substring(index).trim();
                if (!nbtText.startsWith("{")) {
                    return null;
                }
                nbt = StringNbtReader.readCompound(nbtText);
            }

            return new BlockConditionSpec(blockId, properties, nbt);
        } catch (Exception ignored) {
            return null;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean propertyMatches(BlockState state, Property property, String expectedValue) {
        return property.name(state.get(property)).equals(expectedValue);
    }

    private static void sendResult(ServerPlayerEntity player, boolean success, Text message) {
        JsonObject body = new JsonObject();
        body.addProperty("success", success);
        body.addProperty("message", message.getString());
        ServerPlayNetworking.send(NullSafety.requireNonNull(player), new BlockingCardS2CPayload("config_result", body.toString()));
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

    private static boolean getBoolean(JsonObject object, String key, boolean fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        try {
            return object.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private record BlockConditionSpec(Identifier blockId, Map<String, String> properties, NbtCompound nbt) {
    }
}