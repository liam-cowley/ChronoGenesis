package com.chronogenesis.biocomputer;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.essence.EssenceItems;
import com.chronogenesis.registry.ModItems;
import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Dissection Table: the reanimation altar. Use a Preserved Brain on it to set the brain in its jar
 * (sneak with an empty hand to take it back out), pour Time in from a Stasis Ampoule, and point Taps of
 * an Awareness and a Time organism at it. Use it with an empty hand to see how far along it is.
 */
public class DissectionTableBlock extends BaseEntityBlock {
    public static final MapCodec<DissectionTableBlock> CODEC = simpleCodec(DissectionTableBlock::new);
    public static final BooleanProperty BRAIN = BooleanProperty.create("brain");

    private static final VoxelShape TABLE = Shapes.or(
            box(0, 7, 0, 16, 9, 16),
            box(1, 0, 1, 3, 7, 3), box(13, 0, 1, 15, 7, 3), box(1, 0, 13, 3, 7, 15), box(13, 0, 13, 15, 7, 15));
    private static final VoxelShape WITH_JAR = Shapes.or(TABLE, box(4, 9, 4, 12, 16, 12));

    public DissectionTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BRAIN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BRAIN);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(BRAIN) ? WITH_JAR : TABLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DissectionTableBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DissectionTableBlockEntity table)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(ModItems.PRESERVED_BRAIN.get()) && !state.getValue(BRAIN)) {
            if (!level.isClientSide()) {
                level.setBlockAndUpdate(pos, state.setValue(BRAIN, true));
                stack.consume(1, player);
                level.playSound(null, pos, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 1.0f, 0.6f);
                tell(player, Component.translatable("message.chronogenesis.table.brain_set"));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        EssenceContents held = EssenceItems.get(stack);
        if (held != null && held.aspect() == Aspect.TIME) {
            if (!level.isClientSide()) {
                int space = table.space(Aspect.TIME);
                EssenceContents taken = space > 0 ? EssenceItems.drainHeld(player, hand, Aspect.TIME, space) : null;
                if (taken == null) {
                    tell(player, Component.translatable(table.hasBrain()
                            ? "message.chronogenesis.table.full" : "message.chronogenesis.table.no_brain"));
                    return ItemInteractionResult.FAIL;
                }
                table.insert(Aspect.TIME, taken.amount());
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.8f, 0.6f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof DissectionTableBlockEntity table)) {
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (player.isSecondaryUseActive() && !table.hasBrain() && player instanceof net.minecraft.server.level.ServerPlayer patient
                && com.chronogenesis.player.PlayerGrafts.excise(patient, pos)) {
            // Lying on it, with nothing on it: a graft of the Unbegotten is cut out (core-systems.md section 5).
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive() && table.hasBrain()) {
            level.setBlockAndUpdate(pos, state.setValue(BRAIN, false));
            table.clear();
            player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.PRESERVED_BRAIN.get()));
            return InteractionResult.SUCCESS;
        }
        tell(player, !table.hasBrain()
                ? Component.translatable("message.chronogenesis.table.no_brain")
                : Component.translatable("message.chronogenesis.table.status", table.awareness(),
                        DissectionTableBlockEntity.NEEDED, table.time(), DissectionTableBlockEntity.NEEDED, table.rods()));
        return InteractionResult.SUCCESS;
    }

    private static void tell(Player player, Component message) {
        player.displayClientMessage(message.copy().withStyle(ChatFormatting.GRAY), true);
    }
}
