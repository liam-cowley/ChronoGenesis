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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A lobe of the Chronocortex: brain matter grown over Brass Armature around it, fed on Awareness from its wired
 * organisms (see {@link ChronocortexBlockEntity}). Each lobe is room for one more thing in its mind.
 * <p>
 * {@link #PART}: a fresh lobe is still a soft mass in its frame, and firms up with time. Once the lobes fill a
 * 5 x 5 x 5 cranium around the jar, the mind closes over it: the lobes become the Cerebrum, its final stage,
 * with an eye opening in the middle of every face.
 * Using any lobe is using the Chronocortex.
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
    /** How far from the jar a lobe may be and still belong to it. */
    public static final int REACH = 4;

    public CortexLobeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, Part.FRESH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
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

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(PART) == Part.FRESH ? Shapes.empty() : Shapes.block();
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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        ChronocortexBlockEntity cortex = cortexOf(level, pos);
        if (cortex == null) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, OpenChronocortexPayload.of(cortex));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        ChronocortexBlockEntity cortex = cortexOf(level, pos);
        if (cortex == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return cortex.teach(stack, player);
    }
}
