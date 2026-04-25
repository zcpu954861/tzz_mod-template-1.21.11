package com.zcpu.tzzmod.ModBlock.custom;

import com.mojang.serialization.MapCodec;
import com.zcpu.tzzmod.ModBlock.entity.SignalEmitterBlockEntity;
import com.zcpu.tzzmod.action.ActionExecutionResult;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
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
import net.minecraft.world.block.WireOrientation;
import org.jspecify.annotations.Nullable;

public class SignalEmitterBlock extends BlockWithEntity {
    public static final MapCodec<SignalEmitterBlock> CODEC = createCodec(SignalEmitterBlock::new);
    public static final BooleanProperty POWERED = Properties.POWERED;

    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D),
            Block.createCuboidShape(2.0D, 4.0D, 2.0D, 14.0D, 12.0D, 14.0D),
            Block.createCuboidShape(5.0D, 12.0D, 5.0D, 11.0D, 16.0D, 11.0D)
    ).simplify();

    public SignalEmitterBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SignalEmitterBlockEntity(pos, state);
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(POWERED, ctx.getWorld().isReceivingRedstonePower(ctx.getBlockPos()));
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (world.isClient()) {
            return;
        }

        boolean powered = world.isReceivingRedstonePower(pos);
        if (world instanceof ServerWorld serverWorld && world.getBlockEntity(pos) instanceof SignalEmitterBlockEntity blockEntity) {
            blockEntity.setLastPowered(powered);
            com.zcpu.tzzmod.signal.device.SignalDeviceStore.upsertEmitter(serverWorld, pos, blockEntity);
        }
        if (state.get(POWERED) != powered) {
            world.setBlockState(pos, state.with(POWERED, powered), Block.NOTIFY_ALL);
        }
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation orientation, boolean notify) {
        if (world.isClient() || !(world instanceof ServerWorld serverWorld)) {
            return;
        }

        updatePoweredState(serverWorld, pos, state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        if (player instanceof ServerPlayerEntity serverPlayer
                && world instanceof ServerWorld serverWorld
                && world.getBlockEntity(pos) instanceof SignalEmitterBlockEntity blockEntity) {
            sendStatus(serverPlayer, serverWorld, pos, blockEntity);
            return ActionResult.SUCCESS_SERVER;
        }

        return ActionResult.CONSUME;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    private static void updatePoweredState(ServerWorld world, BlockPos pos, BlockState state) {
        boolean newPowered = world.isReceivingRedstonePower(pos);
        if (!(world.getBlockEntity(pos) instanceof SignalEmitterBlockEntity blockEntity)) {
            if (state.get(POWERED) != newPowered) {
                world.setBlockState(pos, state.with(POWERED, newPowered), Block.NOTIFY_ALL);
            }
            return;
        }

        boolean oldPowered = blockEntity.lastPowered();
        if (oldPowered != newPowered) {
            blockEntity.setLastPowered(newPowered);
        }
        if (state.get(POWERED) != newPowered) {
            world.setBlockState(pos, state.with(POWERED, newPowered), Block.NOTIFY_ALL);
        }
        if (!oldPowered && newPowered) {
            ActionExecutionResult result = blockEntity.emitSignal(world, null);
            if (blockEntity.enabled()
                    && !blockEntity.channel().isBlank()
                    && com.zcpu.tzzmod.signal.SignalChannel.isValid(blockEntity.channel())) {
                com.zcpu.tzzmod.signal.device.SignalDeviceStore.recordTrigger(world, pos, blockEntity, result);
            }
        }
    }

    private static void sendStatus(ServerPlayerEntity player, ServerWorld world, BlockPos pos, SignalEmitterBlockEntity blockEntity) {
        player.sendMessage(Text.literal("===========").formatted(Formatting.AQUA), false);
        player.sendMessage(Text.literal("信号发射器").formatted(Formatting.GOLD), false);
        player.sendMessage(field("频道", blockEntity.channel().isBlank()
                ? Text.literal("未绑定").formatted(Formatting.YELLOW)
                : Text.literal(blockEntity.channel()).formatted(Formatting.AQUA)), false);
        player.sendMessage(field("状态", Text.literal(blockEntity.enabled() ? "启用" : "禁用")
                .formatted(blockEntity.enabled() ? Formatting.GREEN : Formatting.RED)), false);
        player.sendMessage(field("红石", Text.literal(world.isReceivingRedstonePower(pos) ? "已通电" : "未通电")
                .formatted(world.isReceivingRedstonePower(pos) ? Formatting.GREEN : Formatting.GRAY)), false);
        player.sendMessage(field("位置", Text.literal(positionText(pos)).formatted(Formatting.LIGHT_PURPLE)), false);
        if (player.isCreativeLevelTwoOp()) {
            player.sendMessage(field("绑定频道", Text.literal("/tzz signal device bind " + positionText(pos) + " <channel>")
                    .formatted(Formatting.GREEN)), false);
            player.sendMessage(field("测试发射", Text.literal("/tzz signal device test " + positionText(pos))
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
