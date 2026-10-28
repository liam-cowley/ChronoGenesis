package com.chronogenesis.item;

import java.util.List;

import com.chronogenesis.registry.ModDataComponents;
import com.chronogenesis.timetravel.FossilCortexBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A Nerve Spool: a long nerve wound on a brass bobbin. Its loose end takes root in a Fossil Cortex; carried to the
 * Chronocortex and used on it, the nerve is laid between them (however far), and the fossil's memories flow in.
 */
public class NerveSpoolItem extends Item {
    public NerveSpoolItem(Properties properties) {
        super(properties);
    }

    /** The loose end takes root in a fossil. */
    public static ItemInteractionResult bind(ItemStack stack, Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof FossilCortexBlockEntity fossil)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide()) {
            if (fossil.linked()) {
                player.displayClientMessage(Component.translatable("message.chronogenesis.spool.already").withStyle(ChatFormatting.GRAY), true);
            } else {
                stack.set(ModDataComponents.SPOOL_END.get(), GlobalPos.of(level.dimension(), pos));
                level.playSound(null, pos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 0.8f, 0.6f);
                player.displayClientMessage(Component.translatable("message.chronogenesis.spool.bound").withStyle(ChatFormatting.DARK_AQUA), true);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModDataComponents.SPOOL_END.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GlobalPos end = stack.get(ModDataComponents.SPOOL_END.get());
        if (end == null) {
            tooltip.add(Component.translatable("tooltip.chronogenesis.nerve_spool").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.chronogenesis.nerve_spool.bound", end.pos().getX(), end.pos().getY(),
                    end.pos().getZ()).withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
