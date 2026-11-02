package com.chronogenesis.biocomputer;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.network.OpenChronocortexPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A lobe of the Chronocortex: brain matter grown into Brass Armature built round it, fed on Awareness from its wired
 * organisms (see {@link ChronocortexBlockEntity}). Each lobe is room for one more thing in its mind.
 * <p>
 * {@link #PART}: a fresh lobe is still a soft mass in its frame, and firms up with time (still in its frame). Once
 * the lobes fill a stage's structure (the 3 x 3 x 2 box over the jar, then the 5 x 5 x 3 round it), the stage's own
 * model takes their place: {@code CEREBRUM}, not drawn as blocks (client/CortexBrainRenderer draws the model). {@code CEREBRUM_EYE}
 * is from older worlds and counts the same. {@link #OPEN}: part of an open corridor to the jar, nothing to bump into.
 * Using any lobe (empty-handed or with an item) is using the Chronocortex; sneaking empty-handed opens (or closes) the
 * way in.
 */
public class CortexLobeBlock extends Block {
    public enum Part implements StringRepresentable {
        FRESH, MATURE, CEREBRUM, CEREBRUM_EYE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public boolean formed() {
            return this == CEREBRUM || this == CEREBRUM_EYE;
        }
    }

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    /** Parted, to let someone through to the jar. */
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    /** How far from the jar a lobe may be and still belong to it. */
    public static final int REACH = 4;

    public CortexLobeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, Part.FRESH).setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, OPEN);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(PART) == Part.FRESH;
    }

    /** Fresh lobes firm up. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.setBlockAndUpdate(pos, state.setValue(PART, Part.MATURE));
        }
    }

    /** Lobes merged into the body are not drawn as blocks: the Chronocortex draws its body (client/CortexBrainRenderer). */
    public static boolean drawnByCortex(BlockState state) {
        return state.getValue(PART).formed();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return drawnByCortex(state) || state.getValue(OPEN) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    /** The Chronocortex this lobe belongs to: the nearest one within reach. */
    @Nullable
    public static ChronocortexBlockEntity cortexOf(Level level, BlockPos lobe) {
        ChronocortexBlockEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(lobe.offset(-REACH, -REACH, -REACH), lobe.offset(REACH, REACH, REACH))) {
            if (level.getBlockEntity(pos) instanceof ChronocortexBlockEntity cortex) {
                double distance = pos.distSqr(lobe);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = cortex;
                }
            }
        }
        return best;
    }

    /** Used empty-handed, a part of the cortex opens its view; sneaking, it opens (or closes) the way in to the jar. */
    public static InteractionResult usePart(Level level, BlockPos pos, Player player) {
        ChronocortexBlockEntity cortex = cortexOf(level, pos);
        if (cortex == null) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (player.isSecondaryUseActive()) {
                cortex.togglePath(serverPlayer);
            } else {
                PacketDistributor.sendToPlayer(serverPlayer, OpenChronocortexPayload.of(cortex, false));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return usePart(level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        ChronocortexBlockEntity cortex = cortexOf(level, pos);
        if (cortex == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return cortex.use(stack, player);
    }
}
