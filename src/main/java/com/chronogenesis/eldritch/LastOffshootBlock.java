package com.chronogenesis.eldritch;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.item.NerveSpoolItem;
import com.chronogenesis.registry.ModBlockEntities;
import com.chronogenesis.registry.ModBlocks;
import com.chronogenesis.registry.ModDataComponents;
import com.chronogenesis.timetravel.ElderArena;
import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Last Offshoot: the Chronocortex's last bud, grown by research, to be planted at the beginning of time. It takes
 * root only in the Cradle Stone of an arena Before Time. There a threaded Nerve Spool, bound to the Chronocortex, links
 * it through time (it becomes the cortex, here); then, used, it wakes ({@link LastOffshootBlockEntity}).
 */
public class LastOffshootBlock extends BaseEntityBlock {
    public static final MapCodec<LastOffshootBlock> CODEC = simpleCodec(LastOffshootBlock::new);
    private static final VoxelShape SHAPE = box(4, 0, 4, 12, 10, 12);

    public LastOffshootBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(ModBlocks.CRADLE_STONE.get());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LastOffshootBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.LAST_OFFSHOOT.get(),
                level.isClientSide() ? LastOffshootBlockEntity::clientTick : LastOffshootBlockEntity::serverTick);
    }

    /** A threaded spool bound to the Chronocortex lays its nerve into it, through time. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof NerveSpoolItem) || !(level.getBlockEntity(pos) instanceof LastOffshootBlockEntity offshoot)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (offshoot.linked()) {
            player.displayClientMessage(LastOffshootBlockEntity.hint("linked_already"), true);
        } else if (!NerveSpoolItem.threaded(stack)) {
            player.displayClientMessage(LastOffshootBlockEntity.hint("frays"), false);
        } else if (!stack.has(ModDataComponents.SPOOL_CORTEX.get())) {
            player.displayClientMessage(LastOffshootBlockEntity.hint("loose"), false);
        } else {
            String name = stack.get(ModDataComponents.SPOOL_CORTEX.get());
            offshoot.link(name);
            stack.consume(1, player);
            ServerLevel server = (ServerLevel) level;
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 30, 0.3, 0.4, 0.3, 0.1);
            server.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5f, 0.3f);
            player.displayClientMessage(com.chronogenesis.biocomputer.Whispers.voiced(offshoot.name(),
                    Component.translatable("whisper.chronogenesis.unbound.linked")), false);
        }
        return ItemInteractionResult.SUCCESS;
    }

    /** Linked, it wakes when used (once nothing else is waking or fighting in its arena). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof LastOffshootBlockEntity offshoot)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            ElderArena.Arena arena = ElderArena.arenas(server).at(pos.below(2));
            if (!offshoot.linked()) {
                player.displayClientMessage(LastOffshootBlockEntity.hint("asleep"), false);
            } else if (offshoot.waking() >= 0) {
                return InteractionResult.CONSUME;
            } else if (arena == null || arena.state == ElderArena.State.FIGHT) {
                player.displayClientMessage(LastOffshootBlockEntity.hint("not_now"), true);
            } else {
                arena.state = ElderArena.State.WAKING;
                ElderArena.arenas(server).setDirty();
                offshoot.wake();
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** The Last Offshoot as an item: it takes root only in a Cradle Stone. */
    public static class Item extends BlockItem {
        public Item(LastOffshootBlock block, Properties properties) {
            super(block, properties);
        }

        @Override
        public InteractionResult place(BlockPlaceContext context) {
            if (!context.getLevel().getBlockState(context.getClickedPos().below()).is(ModBlocks.CRADLE_STONE.get())) {
                if (context.getPlayer() != null && !context.getLevel().isClientSide()) {
                    context.getPlayer().displayClientMessage(LastOffshootBlockEntity.hint("no_cradle").withStyle(ChatFormatting.GRAY), true);
                }
                return InteractionResult.FAIL;
            }
            return super.place(context);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("tooltip.chronogenesis.last_offshoot").withStyle(ChatFormatting.GRAY));
        }
    }
}
