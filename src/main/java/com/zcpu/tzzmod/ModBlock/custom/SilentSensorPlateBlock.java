package com.zcpu.tzzmod.ModBlock.custom;

import com.mojang.serialization.MapCodec;
import com.zcpu.tzzmod.ModBlock.ModBlockEntities;
import com.zcpu.tzzmod.ModBlock.entity.SilentSensorPlateBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

public class SilentSensorPlateBlock extends BlockWithEntity {
    public static final MapCodec<SilentSensorPlateBlock> CODEC = createCodec(SilentSensorPlateBlock::new);
    public static final BooleanProperty POWERED = Properties.POWERED;

    private static final VoxelShape DEFAULT_SHAPE = Block.createColumnShape(14.0D, 0.0D, 1.0D);
    private static final VoxelShape PRESSED_SHAPE = Block.createColumnShape(14.0D, 0.0D, 0.5D);
    private static final Box DETECTION_BOX = Block.createColumnShape(14.0D, 0.0D, 4.0D).getBoundingBoxes().getFirst();

    public SilentSensorPlateBlock(Settings settings) {
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
        return new SilentSensorPlateBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient() ? null : validateTicker(type, ModBlockEntities.SILENT_SENSOR_PLATE, SilentSensorPlateBlockEntity::tickServer);
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(POWERED) ? PRESSED_SHAPE : DEFAULT_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(POWERED) ? PRESSED_SHAPE : DEFAULT_SHAPE;
    }

    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos downPos = pos.down();
        return hasTopRim(world, downPos) || sideCoversSmallSquare(world, downPos, Direction.UP);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(
            BlockState state,
            WorldView world,
            ScheduledTickView tickView,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            Random random
    ) {
        if (direction == Direction.DOWN && !state.canPlaceAt(world, pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (!moved && state.get(POWERED)) {
            updateNeighbors(world, pos);
        }
        super.onStateReplaced(state, world, pos, moved);
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
        return direction == Direction.UP && state.get(POWERED) ? 15 : 0;
    }

    public static Box getDetectionBox(BlockPos pos) {
        return DETECTION_BOX.offset(pos);
    }

    public static void setPowered(World world, BlockPos pos, BlockState state, boolean powered) {
        if (state.get(POWERED) == powered) {
            return;
        }

        BlockState newState = state.with(POWERED, powered);
        world.setBlockState(pos, newState, Block.NOTIFY_ALL);
        world.playSound(
                null,
                pos,
                powered ? SoundEvents.BLOCK_METAL_PRESSURE_PLATE_CLICK_ON : SoundEvents.BLOCK_METAL_PRESSURE_PLATE_CLICK_OFF,
                SoundCategory.BLOCKS
        );
        updateNeighbors(world, pos);
    }

    private static void updateNeighbors(World world, BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();
        world.updateNeighbors(pos, block);
        world.updateNeighbors(pos.down(), block);
    }
}

