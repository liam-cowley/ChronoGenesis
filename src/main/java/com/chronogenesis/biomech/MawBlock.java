package com.chronogenesis.biomech;

import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.essence.EssenceStorage;
import com.chronogenesis.item.EssenceHolder;
import com.chronogenesis.item.EssenceItems;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Maw graft (input): swallows essence poured into it, and every second sucks from whatever it faces: an
 * Essence Jug, or another organism's Tap pointing back at it.
 */
public class MawBlock extends GraftBlock {
    public static final MapCodec<MawBlock> CODEC = simpleCodec(MawBlock::new);
    private static final int INTERVAL = 20;
    private static final int DRAMS_PER_GULP = 2;

    public MawBlock(Properties properties) {
        super(properties, 3, 0, 3, 13, 4, 13);
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof EssenceHolder)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        OrganismCoreBlockEntity core = core(level, state, pos);
        if (core == null || !EssenceItems.exchangeWithStorage(player, stack, core, true, false)) {
            player.displayClientMessage(Component.translatable("message.chronogenesis.organism.refuses"), true);
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, INTERVAL);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.scheduleTick(pos, this, INTERVAL);
        OrganismCoreBlockEntity core = core(level, state, pos);
        if (core == null) {
            return;
        }
        BlockPos facingPos = pos.relative(state.getValue(FACING));
        BlockState facingState = level.getBlockState(facingPos);
        EssenceStorage source = null;
        if (level.getBlockEntity(facingPos) instanceof EssenceStorage storage && !(storage instanceof OrganismCoreBlockEntity)) {
            source = storage;
        } else if (facingState.getBlock() instanceof TapBlock
                && facingState.getValue(FACING) == state.getValue(FACING).getOpposite()) {
            source = TapBlock.coreOf(level, facingState, facingPos);
        }
        if (source == null || source == core) {
            return;
        }
        EssenceContents contents = source.contents();
        if (contents == null) {
            return;
        }
        int amount = core.insert(contents.aspect(), source.extract(DRAMS_PER_GULP, true), true);
        if (amount > 0) {
            source.extract(amount, false);
            core.insert(contents.aspect(), amount, false);
        }
    }
}
