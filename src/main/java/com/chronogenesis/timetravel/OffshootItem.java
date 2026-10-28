package com.chronogenesis.timetravel;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

/** The Cortex Offshoot as an item: it will only be planted in a past era (it needs ages to grow, not days). */
public class OffshootItem extends BlockItem {
    public OffshootItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (Remnants.eraOf(context.getLevel()) == null) {
            if (context.getPlayer() != null && !context.getLevel().isClientSide()) {
                context.getPlayer().displayClientMessage(Component.translatable("message.chronogenesis.offshoot.present")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.chronogenesis.cortex_offshoot").withStyle(ChatFormatting.GRAY));
    }
}
