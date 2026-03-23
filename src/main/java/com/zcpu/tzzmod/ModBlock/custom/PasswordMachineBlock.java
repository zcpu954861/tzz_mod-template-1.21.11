package com.zcpu.tzzmod.ModBlock.custom;

import com.mojang.serialization.MapCodec;
import com.zcpu.tzzmod.ModBlock.entity.PasswordMachineBlockEntity;
import com.zcpu.tzzmod.ModItem.ModItems;
import com.zcpu.tzzmod.ModItem.custom.PasswordConfigCardItem;
import com.zcpu.tzzmod.password.PasswordMachineClientAccess;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public class PasswordMachineBlock extends BlockWithEntity {
    public static final MapCodec<PasswordMachineBlock> CODEC = createCodec(PasswordMachineBlock::new);
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = Properties.POWERED;
    public static final int ACTIVE_TICKS = 4;

    // Model basis: blockstate facing=east uses y=0, so these cuboids are defined in EAST orientation.
    // Parts above y=16 are clipped/ignored per request.
    private static final VoxelShape EAST_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(0.0D, 1.0D, 0.0D, 16.0D, 13.0D, 16.0D),
            Block.createCuboidShape(0.0D, 0.0D, 0.0D, 2.0D, 1.0D, 2.0D),
            Block.createCuboidShape(14.0D, 0.0D, 0.0D, 16.0D, 1.0D, 2.0D),
            Block.createCuboidShape(14.0D, 0.0D, 14.0D, 16.0D, 1.0D, 16.0D),
            Block.createCuboidShape(0.0D, 0.0D, 14.0D, 2.0D, 1.0D, 16.0D),
            Block.createCuboidShape(1.0D, 13.0D, 0.0D, 15.0D, 16.0D, 2.0D),
            Block.createCuboidShape(1.0D, 13.0D, 2.0D, 15.0D, 15.0D, 9.0D),
            Block.createCuboidShape(1.0D, 15.0D, 2.0D, 15.0D, 16.0D, 3.0D),
            Block.createCuboidShape(1.0D, 13.0D, 9.0D, 15.0D, 14.0D, 16.0D),
            Block.createCuboidShape(0.0D, 13.0D, 0.0D, 1.0D, 16.0D, 16.0D),
            Block.createCuboidShape(15.0D, 13.0D, 0.0D, 16.0D, 16.0D, 16.0D)
    );
    private static final VoxelShape SOUTH_SHAPE = rotateClockwise(EAST_SHAPE);
    private static final VoxelShape WEST_SHAPE = rotateClockwise(SOUTH_SHAPE);
    private static final VoxelShape NORTH_SHAPE = rotateClockwise(WEST_SHAPE);

    public PasswordMachineBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PasswordMachineBlockEntity(pos, state);
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(POWERED, FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!stack.isOf(ModItems.PASSWORD_CONFIG_CARD) || !PasswordConfigCardItem.hasConfiguredPassword(stack)) {
            return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
        }

        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        if (!(world.getBlockEntity(pos) instanceof PasswordMachineBlockEntity blockEntity)) {
            return ActionResult.PASS;
        }

        blockEntity.setPasswordCode(PasswordConfigCardItem.getStoredPassword(stack));
        world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.BLOCKS, 0.85F, 1.15F);
        player.sendMessage(Text.translatable("block.tzz_mod.password_machine.load_success"), true);
        return ActionResult.SUCCESS_SERVER;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            PasswordMachineClientAccess.openScreen(pos);
            return ActionResult.SUCCESS;
        }
        return ActionResult.CONSUME;
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!state.get(POWERED)) {
            return;
        }
        world.setBlockState(pos, state.with(POWERED, false), Block.NOTIFY_ALL);
        updateRedstone(world, pos);
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

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getShapeForFacing(state.get(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getShapeForFacing(state.get(FACING));
    }

    public static void activate(ServerWorld world, BlockPos pos, BlockState state) {
        if (!state.get(POWERED)) {
            world.setBlockState(pos, state.with(POWERED, true), Block.NOTIFY_ALL);
        }
        updateRedstone(world, pos);
        world.scheduleBlockTick(pos, state.getBlock(), ACTIVE_TICKS);
    }

    private static void updateRedstone(World world, BlockPos pos) {
        world.updateNeighborsAlways(pos, world.getBlockState(pos).getBlock(), null);
        for (Direction direction : Direction.values()) {
            world.updateNeighborsAlways(pos.offset(direction), world.getBlockState(pos).getBlock(), null);
        }
    }

    private static VoxelShape getShapeForFacing(Direction facing) {
        return switch (facing) {
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case NORTH -> NORTH_SHAPE;
            default -> EAST_SHAPE;
        };
    }

    private static VoxelShape rotateClockwise(VoxelShape shape) {
        VoxelShape[] rotated = new VoxelShape[]{VoxelShapes.empty()};
        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> rotated[0] = VoxelShapes.union(
                rotated[0],
                Block.createCuboidShape(
                        (1.0D - maxZ) * 16.0D,
                        minY * 16.0D,
                        minX * 16.0D,
                        (1.0D - minZ) * 16.0D,
                        maxY * 16.0D,
                        maxX * 16.0D
                )
        ));
        return rotated[0].simplify();
    }
}

