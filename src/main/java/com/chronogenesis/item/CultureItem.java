package com.chronogenesis.item;

import java.util.List;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.organism.FleshBlock;
import com.chronogenesis.registry.ModBlocks;
import com.chronogenesis.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A living culture of one aspect. Use it on a Copper Armature block to seed an organism there;
 * then feed it essence of the same aspect and it grows into the rest of the frame.
 */
public class CultureItem extends Item {
    public CultureItem(Properties properties) {
        super(properties);
    }

    public static Aspect aspect(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ASPECT.get(), Aspect.DENSITY);
    }

    public static ItemStack of(Aspect aspect) {
        ItemStack stack = new ItemStack(com.chronogenesis.registry.ModItems.CULTURE.get());
        stack.set(ModDataComponents.ASPECT.get(), aspect);
        return stack;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(ModBlocks.COPPER_ARMATURE.get())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            Aspect aspect = aspect(context.getItemInHand());
            level.setBlockAndUpdate(pos, ModBlocks.ORGANISM_CORE.get().defaultBlockState().setValue(FleshBlock.ASPECT, aspect));
            level.playSound(null, pos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 1.0f, 0.6f);
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.translatable("message.chronogenesis.culture.took_hold")
                        .withStyle(ChatFormatting.GRAY), true);
                if (!context.getPlayer().isCreative()) {
                    context.getItemInHand().shrink(1);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.chronogenesis.culture.of", aspect(stack).displayName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.chronogenesis.culture").withStyle(ChatFormatting.DARK_GRAY));
    }
}
