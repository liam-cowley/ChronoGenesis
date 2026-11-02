package com.chronogenesis.biocomputer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Time Capsule: a brass and glass vessel with a moment of the deep past sealed in it. One is pieced together from
 * parts found in the Age of the Precursors and the Ice Age; another stands at the heart of every Jurassic temple.
 * Built into the last shell of the Chronocortex (in place of a frame), each lets it reach further back
 * ({@link ChronocortexBlockEntity#capsules()}, the eras' {@code capsules}). {@link #FORMED}: part of the cortex's
 * whole body, which draws it (client/CortexBrainRenderer).
 */
public class TimeCapsuleBlock extends Block {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    private static final VoxelShape SHAPE = Shapes.or(box(2, 0, 2, 14, 3, 14), box(3, 3, 3, 13, 13, 13), box(2, 13, 2, 14, 16, 14));

    public TimeCapsuleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FORMED) ? Shapes.block() : SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return CortexLobeBlock.usePart(level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        ChronocortexBlockEntity cortex = CortexLobeBlock.cortexOf(level, pos);
        return cortex == null ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION : cortex.use(stack, player);
    }

    /** The moment inside glimmers. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(FORMED) && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.3 + random.nextDouble() * 0.5,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.01, 0);
        }
    }
}
