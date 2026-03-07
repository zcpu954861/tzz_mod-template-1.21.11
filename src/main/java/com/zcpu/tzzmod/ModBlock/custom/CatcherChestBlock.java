package com.zcpu.tzzmod.ModBlock.custom;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

// Added for HALF enum and shapes
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.WireOrientation;

public class CatcherChestBlock extends Block {
    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = Properties.OPEN;
    public static final BooleanProperty POWERED = Properties.POWERED;
    public static final EnumProperty<DoorHinge> HINGE = Properties.DOOR_HINGE;

    // 1 pixel = 1/16 of a block; coordinates are in 0..16
    private static final VoxelShape BOTTOM = Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);
    private static final VoxelShape TOP = Block.createCuboidShape(0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape NORTH_WALL = Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D);
    private static final VoxelShape SOUTH_WALL = Block.createCuboidShape(0.0D, 0.0D, 15.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape WEST_WALL = Block.createCuboidShape(0.0D, 0.0D, 0.0D, 1.0D, 16.0D, 16.0D);
    private static final VoxelShape EAST_WALL = Block.createCuboidShape(15.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    // Walls only (no top/bottom)
    private static final VoxelShape WALLS_SHAPE = VoxelShapes.union(NORTH_WALL, SOUTH_WALL, WEST_WALL, EAST_WALL);
    private static final VoxelShape WALLS_NO_NORTH = VoxelShapes.union(SOUTH_WALL, WEST_WALL, EAST_WALL);
    private static final VoxelShape WALLS_NO_SOUTH = VoxelShapes.union(NORTH_WALL, WEST_WALL, EAST_WALL);
    private static final VoxelShape WALLS_NO_WEST = VoxelShapes.union(NORTH_WALL, SOUTH_WALL, EAST_WALL);
    private static final VoxelShape WALLS_NO_EAST = VoxelShapes.union(NORTH_WALL, SOUTH_WALL, WEST_WALL);

    // Lower half: bottom + walls
    private static final VoxelShape LOWER_SHAPE = VoxelShapes.union(BOTTOM, WALLS_SHAPE);
    private static final VoxelShape LOWER_OPEN_NORTH = VoxelShapes.union(BOTTOM, WALLS_NO_NORTH);
    private static final VoxelShape LOWER_OPEN_SOUTH = VoxelShapes.union(BOTTOM, WALLS_NO_SOUTH);
    private static final VoxelShape LOWER_OPEN_WEST = VoxelShapes.union(BOTTOM, WALLS_NO_WEST);
    private static final VoxelShape LOWER_OPEN_EAST = VoxelShapes.union(BOTTOM, WALLS_NO_EAST);

    // Upper half: top + walls
    private static final VoxelShape UPPER_SHAPE = VoxelShapes.union(TOP, WALLS_SHAPE);
    private static final VoxelShape UPPER_OPEN_NORTH = VoxelShapes.union(TOP, WALLS_NO_NORTH);
    private static final VoxelShape UPPER_OPEN_SOUTH = VoxelShapes.union(TOP, WALLS_NO_SOUTH);
    private static final VoxelShape UPPER_OPEN_WEST = VoxelShapes.union(TOP, WALLS_NO_WEST);
    private static final VoxelShape UPPER_OPEN_EAST = VoxelShapes.union(TOP, WALLS_NO_EAST);

    public CatcherChestBlock(Settings settings) {
        super(settings);
        // default to lower half, facing north, closed
        setDefaultState(getStateManager().getDefaultState()
                .with(HALF, DoubleBlockHalf.LOWER)
                .with(FACING, Direction.NORTH)
                .with(OPEN, false)
                .with(POWERED, false)
                .with(HINGE, DoorHinge.LEFT)
        );
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        World world = ctx.getWorld();
        BlockPos upPos = pos.up();
        if (!world.getBlockState(upPos).canReplace(ctx)) {
            return null;
        }

        Direction facing = ctx.getHorizontalPlayerFacing().getOpposite();
        DoorHinge hinge = getHingeFromContext(ctx, facing);
        boolean powered = world.isReceivingRedstonePower(pos) || world.isReceivingRedstonePower(upPos);

        return getDefaultState()
                .with(FACING, facing)
                .with(HINGE, hinge)
                .with(OPEN, powered)
                .with(POWERED, powered)
                .with(HALF, DoubleBlockHalf.LOWER);
    }

    private static DoorHinge getHingeFromContext(ItemPlacementContext ctx, Direction facing) {
        double hitX = ctx.getHitPos().x - ctx.getBlockPos().getX();
        double hitZ = ctx.getHitPos().z - ctx.getBlockPos().getZ();

        boolean clickedRightSide;
        if (facing == Direction.NORTH) {
            clickedRightSide = hitX >= 0.5D;
        } else if (facing == Direction.SOUTH) {
            clickedRightSide = hitX < 0.5D;
        } else if (facing == Direction.WEST) {
            clickedRightSide = hitZ < 0.5D;
        } else { // EAST
            clickedRightSide = hitZ >= 0.5D;
        }

        return clickedRightSide ? DoorHinge.RIGHT : DoorHinge.LEFT;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world.isClient()) return;

        BlockPos upPos = pos.up();
        BlockState upperState = state.with(HALF, DoubleBlockHalf.UPPER);
        world.setBlockState(upPos, upperState, Block.NOTIFY_LISTENERS);
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation orientation, boolean notify) {
        if (world.isClient()) {
            return;
        }

        boolean isLower = state.get(HALF) == DoubleBlockHalf.LOWER;
        BlockPos otherPos = isLower ? pos.up() : pos.down();
        if (!world.getBlockState(otherPos).isOf(this)) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
            return;
        }

        if (isLower) {
            boolean powered = world.isReceivingRedstonePower(pos) || world.isReceivingRedstonePower(otherPos);
            boolean open = state.get(OPEN);
            if (powered != state.get(POWERED)) {
                world.setBlockState(pos, state.with(POWERED, powered).with(OPEN, powered), Block.NOTIFY_LISTENERS);
                BlockState upper = world.getBlockState(otherPos);
                if (upper.isOf(this)) {
                    world.setBlockState(otherPos, upper.with(POWERED, powered).with(OPEN, powered), Block.NOTIFY_LISTENERS);
                }
                if (open != powered) {
                    world.playSound(null, pos, powered ? SoundEvents.BLOCK_IRON_DOOR_OPEN : SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                }
            }
        }
    }

    @Override
    public void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        // Called when the block is changed. Ensure the other half is removed when this half is removed.
        if (state.isOf(this)) {
            // If the new state at pos is still this block, do nothing.
            BlockState newState = world.getBlockState(pos);
            if (newState.isOf(this)) {
                super.onStateReplaced(state, world, pos, moved);
                return;
            }

            DoubleBlockHalf half = state.get(HALF);
            BlockPos otherPos = half == DoubleBlockHalf.LOWER ? pos.up() : pos.down();
            if (world.getBlockState(otherPos).isOf(this)) {
                world.setBlockState(otherPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
        }
        super.onStateReplaced(state, world, pos, moved);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING, OPEN, POWERED, HINGE);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        boolean open = state.get(OPEN);
        Direction facing = state.get(FACING);
        if (!open) {
            return state.get(HALF) == DoubleBlockHalf.LOWER ? LOWER_SHAPE : UPPER_SHAPE;
        } else {
            if (state.get(HALF) == DoubleBlockHalf.LOWER) {
                switch (facing) {
                    case NORTH: return LOWER_OPEN_NORTH;
                    case SOUTH: return LOWER_OPEN_SOUTH;
                    case WEST: return LOWER_OPEN_WEST;
                    default: return LOWER_OPEN_EAST;
                }
            } else {
                switch (facing) {
                    case NORTH: return UPPER_OPEN_NORTH;
                    case SOUTH: return UPPER_OPEN_SOUTH;
                    case WEST: return UPPER_OPEN_WEST;
                    default: return UPPER_OPEN_EAST;
                }
            }
        }
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // Use open shapes when OPEN
        return getOutlineShape(state, world, pos, context);
    }
}
