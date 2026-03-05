package com.zcpu.tzzmod.ModBlock.custom;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.block.WireOrientation;
import org.jetbrains.annotations.Nullable;

// Added imports
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.block.enums.DoorHinge;

public class SplitIronDoorBlock extends Block {
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = Properties.OPEN;
    public static final BooleanProperty POWERED = Properties.POWERED;
    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<DoorHinge> HINGE = Properties.DOOR_HINGE;


    // IMPORTANT: These shapes must match the model JSONs, otherwise you'll always feel "render and collision not together".

    // Explicit per-facing closed shapes and open shapes.
    // Author base shapes for NORTH, then generate EAST/SOUTH/WEST in the static block.
    private static final VoxelShape CLOSED_NORTH = Block.createCuboidShape(0.0D, 0.0D, 14.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape CLOSED_EAST;
    private static final VoxelShape CLOSED_SOUTH;
    private static final VoxelShape CLOSED_WEST;

    private static final VoxelShape OPEN_NORTH = VoxelShapes.union(
            Block.createCuboidShape(0.0D, 0.0D, 8.0D, 2.0D, 16.0D, 16.0D),
            Block.createCuboidShape(14.0D, 0.0D, 8.0D, 16.0D, 16.0D, 16.0D)
    );
    private static final VoxelShape OPEN_EAST;
    private static final VoxelShape OPEN_SOUTH;
    private static final VoxelShape OPEN_WEST;

    static {
        // Generate rotated variants once at class load
        CLOSED_EAST = rotateY270(CLOSED_NORTH);
        CLOSED_SOUTH = rotateY180(CLOSED_NORTH);
        CLOSED_WEST = rotateY90(CLOSED_NORTH);

        OPEN_EAST = rotateY270(OPEN_NORTH);
        OPEN_SOUTH = rotateY180(OPEN_NORTH);
        OPEN_WEST = rotateY90(OPEN_NORTH);
    }

    public SplitIronDoorBlock(AbstractBlock.Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(OPEN, false)
                .with(POWERED, false)
                .with(HALF, DoubleBlockHalf.LOWER)
                .with(HINGE, DoorHinge.LEFT));
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

        // Simple, stable hinge rule: based on where the player clicked inside the block.
        // This avoids the current "always centered" collision bug and gives predictable左右开.
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
        // Using click position: for N/S doors, hinge depends on click X; for E/W depends on click Z.
        // Then interpret it as "left/right" relative to the door's facing.
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
        // When placed on the client, don't modify the world (server only)
        if (world.isClient()) return;

        BlockPos upPos = pos.up();
        BlockState upperState = state.with(HALF, DoubleBlockHalf.UPPER);
        world.setBlockState(upPos, upperState, Block.NOTIFY_LISTENERS);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        // Behaves like iron door: redstone-only, no manual toggle.
        return ActionResult.PASS;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation orientation, boolean notify) {
        if (world.isClient()) {
            return;
        }

        boolean isLower = state.get(HALF) == DoubleBlockHalf.LOWER;
        BlockPos otherPos = isLower ? pos.up() : pos.down();
        if (!world.getBlockState(otherPos).isOf(this)) {
            world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
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
                world.setBlockState(otherPos, net.minecraft.block.Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
        }
        super.onStateReplaced(state, world, pos, moved);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getShapeForState(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getShapeForState(state);
    }

    private VoxelShape getShapeForState(BlockState state) {
        Direction facing = state.get(FACING);
        boolean open = state.get(OPEN);

        if (!open) {
            if (facing == Direction.NORTH) return CLOSED_NORTH;
            if (facing == Direction.EAST) return CLOSED_EAST;
            if (facing == Direction.SOUTH) return CLOSED_SOUTH;
            return CLOSED_WEST;
        } else {
            if (facing == Direction.NORTH) return OPEN_NORTH;
            if (facing == Direction.EAST) return OPEN_EAST;
            if (facing == Direction.SOUTH) return OPEN_SOUTH;
            return OPEN_WEST;
        }
    }

    private static VoxelShape rotateY90(VoxelShape shape) {
        // (x,z) -> (z, 16-x) applied to normalized (0..1) coordinates
        VoxelShape[] buffer = new VoxelShape[]{VoxelShapes.empty()};
        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            VoxelShape rotated = Block.createCuboidShape(
                    minZ * 16.0D, minY * 16.0D, (1.0D - maxX) * 16.0D,
                    maxZ * 16.0D, maxY * 16.0D, (1.0D - minX) * 16.0D
            );
            buffer[0] = VoxelShapes.union(buffer[0], rotated);
        });
        return buffer[0];
    }

    private static VoxelShape rotateY180(VoxelShape shape) {
        // (x,z) -> (16-x, 16-z) applied to normalized (0..1) coordinates
        VoxelShape[] buffer = new VoxelShape[]{VoxelShapes.empty()};
        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            VoxelShape rotated = Block.createCuboidShape(
                    (1.0D - maxX) * 16.0D, minY * 16.0D, (1.0D - maxZ) * 16.0D,
                    (1.0D - minX) * 16.0D, maxY * 16.0D, (1.0D - minZ) * 16.0D
            );
            buffer[0] = VoxelShapes.union(buffer[0], rotated);
        });
        return buffer[0];
    }

    private static VoxelShape rotateY270(VoxelShape shape) {
        // (x,z) -> (16-z, x) applied to normalized (0..1) coordinates
        VoxelShape[] buffer = new VoxelShape[]{VoxelShapes.empty()};
        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            VoxelShape rotated = Block.createCuboidShape(
                    (1.0D - maxZ) * 16.0D, minY * 16.0D, minX * 16.0D,
                    (1.0D - minZ) * 16.0D, maxY * 16.0D, maxX * 16.0D
            );
            buffer[0] = VoxelShapes.union(buffer[0], rotated);
        });
        return buffer[0];
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, POWERED, HALF, HINGE);
    }
}
