package com.chronogenesis.item;

import java.util.List;

import com.chronogenesis.biomech.OrganismCoreBlock;
import com.chronogenesis.biomech.OrganismCoreBlockEntity;
import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.registry.ModBlocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Rotten flesh and bone meal in a bottle. Pour {@value #DRAMS_NEEDED} drams of one stable aspect into it and
 * it becomes a Culture of that aspect, which is set into a Copper Armature to start an organism
 * (core-systems.md §2, "Growing one").
 */
public class CultureJarItem extends EssenceContainerItem {
    public static final int DRAMS_NEEDED = 4;

    public CultureJarItem(Properties properties) {
        super(properties, DRAMS_NEEDED, EssenceContainerItem::stableOnly);
    }

    /** Essence poured into a culture becomes part of it. */
    @Override
    public boolean canGive(ItemStack stack) {
        return false;
    }

    public static boolean isRipe(ItemStack stack) {
        return EssenceItems.drams(stack) >= DRAMS_NEEDED;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        if (!level.getBlockState(pos).is(ModBlocks.COPPER_ARMATURE.get())) {
            return InteractionResult.PASS;
        }
        EssenceContents contents = EssenceItems.contents(stack);
        if (contents == null || !isRipe(stack)) {
            if (context.getPlayer() != null && !level.isClientSide) {
                context.getPlayer().displayClientMessage(Component.translatable("message.chronogenesis.culture.unripe"), true);
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            level.setBlockAndUpdate(pos, ModBlocks.ORGANISM_CORE.get().defaultBlockState()
                    .setValue(OrganismCoreBlock.ASPECT, contents.aspect()));
            if (level.getBlockEntity(pos) instanceof OrganismCoreBlockEntity core) {
                core.setStored(contents.drams());
            }
            level.playSound(null, pos, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 1f, 0.6f);
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.translatable("message.chronogenesis.culture.planted"), true);
            }
            stack.consume(1, context.getPlayer());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        EssenceContents contents = EssenceItems.contents(stack);
        if (contents != null && isRipe(stack)) {
            return Component.translatable("item.chronogenesis.culture", contents.aspect().displayName());
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(isRipe(stack)
                ? "tooltip.chronogenesis.culture.ripe" : "tooltip.chronogenesis.culture.unripe").withStyle(ChatFormatting.DARK_GRAY));
    }
}
