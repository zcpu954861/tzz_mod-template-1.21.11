package com.zcpu.tzzmod.ModBlock.custom;

import com.mojang.serialization.MapCodec;
import com.zcpu.tzzmod.ModBlock.ModBlockEntities;
import com.zcpu.tzzmod.ModBlock.entity.SignalReceiverBlockEntity;
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
import net.minecraft.state.property.Properties;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

public class SignalReceiverBlock extends BlockWithEntity {
    public static final MapCodec<SignalReceiverBlock> CODEC = createCodec(SignalReceiverBlock::new);
    public static final BooleanProperty POWERED = Properties.POWERED;

    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D),
            Block.createCuboidShape(2.0D, 4.0D, 2.0D, 14.0D, 12.0D, 14.0D),
            Block.createCuboidShape(5.0D, 12.0D, 5.0D, 11.0D, 16.0D, 11.0D)
    ).simplify();

    public SignalReceiverBlock(Settings settings) {
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
        return new SignalReceiverBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient() ? null : validateTicker(type, ModBlockEntities.SIGNAL_RECEIVER, SignalReceiverBlockEntity::tickServer);
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(POWERED, false);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (world instanceof ServerWorld serverWorld && world.getBlockEntity(pos) instanceof SignalReceiverBlockEntity blockEntity) {
            SignalDeviceStore.upsertReceiver(serverWorld, pos, blockEntity);
        }
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        if (player instanceof ServerPlayerEntity serverPlayer
                && world instanceof ServerWorld serverWorld
                && world.getBlockEntity(pos) instanceof SignalReceiverBlockEntity blockEntity) {
            sendStatus(serverPlayer, serverWorld, pos, blockEntity);
            return ActionResult.SUCCESS_SERVER;
        }

        return ActionResult.CONSUME;
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (!(world.getBlockState(pos).getBlock() instanceof SignalReceiverBlock)) {
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

    @Override
    protected boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    protected int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(POWERED) ? 15 : 0;
    }

    @Override
    protected int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(POWERED) ? 15 : 0;
    }

    public static void setPowered(ServerWorld world, BlockPos pos, BlockState state, boolean powered) {
        state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof SignalReceiverBlock) || state.get(POWERED) == powered) {
            return;
        }

        world.setBlockState(pos, state.with(POWERED, powered), Block.NOTIFY_ALL);
        updateNeighbors(world, pos);
    }

    private static void updateNeighbors(World world, BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();
        world.updateNeighbors(pos, block);
        for (Direction direction : Direction.values()) {
            world.updateNeighbors(pos.offset(direction), block);
        }
    }

    private static void sendStatus(ServerPlayerEntity player, ServerWorld world, BlockPos pos, SignalReceiverBlockEntity blockEntity) {
        player.sendMessage(Text.literal("===========").formatted(Formatting.AQUA), false);
        player.sendMessage(Text.literal("信号接收器").formatted(Formatting.GOLD), false);
        player.sendMessage(field("频道", blockEntity.channel().isBlank()
                ? Text.literal("未绑定").formatted(Formatting.YELLOW)
                : Text.literal(blockEntity.channel()).formatted(Formatting.AQUA)), false);
        player.sendMessage(field("状态", Text.literal(blockEntity.enabled() ? "启用" : "禁用")
                .formatted(blockEntity.enabled() ? Formatting.GREEN : Formatting.RED)), false);
        player.sendMessage(field("脉冲时长", Text.literal(blockEntity.pulseTicks() + " GT").formatted(Formatting.LIGHT_PURPLE)), false);
        player.sendMessage(field("红石输出", Text.literal(world.getBlockState(pos).get(POWERED) ? "正在输出" : "未输出")
                .formatted(world.getBlockState(pos).get(POWERED) ? Formatting.RED : Formatting.GRAY)), false);
        player.sendMessage(field("剩余 ticks", Text.literal(Integer.toString(blockEntity.remainingPulseTicks())).formatted(Formatting.LIGHT_PURPLE)), false);
        player.sendMessage(field("位置", Text.literal(positionText(pos)).formatted(Formatting.LIGHT_PURPLE)), false);
        if (player.isCreativeLevelTwoOp()) {
            player.sendMessage(field("绑定频道", Text.literal("/tzz signal device bind " + positionText(pos) + " <channel>")
                    .formatted(Formatting.GREEN)), false);
            player.sendMessage(field("设置脉冲", Text.literal("/tzz signal receiver pulse " + positionText(pos) + " 5")
                    .formatted(Formatting.GREEN)), false);
            player.sendMessage(field("测试输出", Text.literal("/tzz signal receiver trigger " + positionText(pos))
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
