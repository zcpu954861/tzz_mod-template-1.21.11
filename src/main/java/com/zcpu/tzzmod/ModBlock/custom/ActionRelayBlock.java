package com.zcpu.tzzmod.ModBlock.custom;

import com.mojang.serialization.MapCodec;
import com.zcpu.tzzmod.ModBlock.ModBlockEntities;
import com.zcpu.tzzmod.ModBlock.entity.ActionRelayBlockEntity;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

public class ActionRelayBlock extends BlockWithEntity {
    public static final MapCodec<ActionRelayBlock> CODEC = createCodec(ActionRelayBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");

    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D),
            Block.createCuboidShape(2.0D, 4.0D, 2.0D, 14.0D, 12.0D, 14.0D),
            Block.createCuboidShape(5.0D, 12.0D, 5.0D, 11.0D, 16.0D, 11.0D)
    ).simplify();

    public ActionRelayBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ActionRelayBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient() ? null : validateTicker(type, ModBlockEntities.ACTION_RELAY, ActionRelayBlockEntity::tickServer);
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(ACTIVE, false);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (world instanceof ServerWorld serverWorld && world.getBlockEntity(pos) instanceof ActionRelayBlockEntity blockEntity) {
            SignalDeviceStore.upsertActionRelay(serverWorld, pos, blockEntity);
        }
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        if (player instanceof ServerPlayerEntity serverPlayer
                && world instanceof ServerWorld serverWorld
                && world.getBlockEntity(pos) instanceof ActionRelayBlockEntity blockEntity) {
            sendStatus(serverPlayer, serverWorld, pos, blockEntity);
            return ActionResult.SUCCESS_SERVER;
        }

        return ActionResult.CONSUME;
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (!(world.getBlockState(pos).getBlock() instanceof ActionRelayBlock)) {
            SignalDeviceStore.remove(world.getServer(), world, pos);
        }
        super.onStateReplaced(state, world, pos, moved);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    public static void setActive(ServerWorld world, BlockPos pos, BlockState state, boolean active) {
        state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ActionRelayBlock) || state.get(ACTIVE) == active) {
            return;
        }
        world.setBlockState(pos, state.with(ACTIVE, active), Block.NOTIFY_ALL);
    }

    private static void sendStatus(ServerPlayerEntity player, ServerWorld world, BlockPos pos, ActionRelayBlockEntity blockEntity) {
        player.sendMessage(Text.literal("===========").formatted(Formatting.AQUA), false);
        player.sendMessage(Text.literal("动作继电器").formatted(Formatting.GOLD), false);
        player.sendMessage(field("频道", blockEntity.channel().isBlank()
                ? Text.literal("未绑定").formatted(Formatting.YELLOW)
                : Text.literal(blockEntity.channel()).formatted(Formatting.AQUA)), false);
        player.sendMessage(field("状态", Text.literal(blockEntity.enabled() ? "启用" : "禁用")
                .formatted(blockEntity.enabled() ? Formatting.GREEN : Formatting.RED)), false);
        player.sendMessage(field("冷却", Text.literal(blockEntity.cooldownTicks() + " GT").formatted(Formatting.LIGHT_PURPLE)), false);
        player.sendMessage(field("动作数量", Text.literal(Integer.toString(blockEntity.actions().size())).formatted(Formatting.LIGHT_PURPLE)), false);
        player.sendMessage(field("最近执行", blockEntity.lastRunWallTimeMillis() <= 0
                ? Text.literal("尚未执行").formatted(Formatting.YELLOW)
                : Text.literal(blockEntity.lastResult().isBlank() ? "已执行" : blockEntity.lastResult()).formatted(Formatting.WHITE)), false);
        player.sendMessage(field("当前高亮", Text.literal(world.getBlockState(pos).get(ACTIVE) ? "高亮" : "待机")
                .formatted(world.getBlockState(pos).get(ACTIVE) ? Formatting.GREEN : Formatting.GRAY)), false);
        player.sendMessage(field("位置", Text.literal(positionText(pos)).formatted(Formatting.LIGHT_PURPLE)), false);
        if (player.isCreativeLevelTwoOp()) {
            player.sendMessage(field("绑定频道", Text.literal("/tzz signal relay bind " + positionText(pos) + " <channel>")
                    .formatted(Formatting.GREEN)), false);
            player.sendMessage(field("添加命令动作", Text.literal("/tzz signal relay addAction " + positionText(pos) + " command <command>")
                    .formatted(Formatting.GREEN)), false);
            player.sendMessage(field("测试执行", Text.literal("/tzz signal relay trigger " + positionText(pos))
                    .formatted(Formatting.GREEN)), false);
        }
    }

    private static MutableText field(String label, Text value) {
        return Text.literal(label + "：").formatted(Formatting.GRAY).append(value);
    }

    private static String positionText(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}
