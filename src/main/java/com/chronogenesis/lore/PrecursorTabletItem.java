package com.chronogenesis.lore;

import java.util.List;

import com.chronogenesis.biocomputer.Research;
import com.chronogenesis.entity.Precursor;
import com.chronogenesis.registry.ModDataComponents;
import com.chronogenesis.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A clay tablet of the Precursors with one fragment of their story ({@link PrecursorLore}) pressed into it. Found in
 * their cities' archives and tombs, under the present in their buried ruins (brush it out of the silt), in the ruins
 * that survive on the surface, and in the temples of the Jurassic. Readable once their tongue is understood (by
 * linking the memory of their age); until then it is only marks.
 */
public class PrecursorTabletItem extends Item {
    public PrecursorTabletItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(String fragment) {
        ItemStack stack = new ItemStack(ModItems.PRECURSOR_TABLET.get());
        stack.set(ModDataComponents.LORE.get(), PrecursorLore.id(fragment));
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ResourceLocation fragment = stack.get(ModDataComponents.LORE.get());
        if (fragment == null) {
            return InteractionResultHolder.pass(stack);
        }
        if (player instanceof ServerPlayer server) {
            server.displayClientMessage(Component.literal(Precursor.tongue(fragment.hashCode() & 0xffff))
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
            if (Research.has(player, Precursor.TONGUE)) {
                server.displayClientMessage(PrecursorLore.title(fragment).copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), false);
                server.displayClientMessage(PrecursorLore.text(fragment).copy().withStyle(ChatFormatting.GRAY), false);
                PrecursorLore.learn(server, fragment);
            } else {
                server.displayClientMessage(Component.translatable("message.chronogenesis.tablet.unreadable")
                        .withStyle(ChatFormatting.DARK_GRAY), false);
            }
        }
        player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0f, 0.6f);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation fragment = stack.get(ModDataComponents.LORE.get());
        if (fragment != null) {
            tooltip.add(Component.literal(Precursor.tongue(fragment.hashCode() & 0xffff)).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        }
        tooltip.add(Component.translatable("tooltip.chronogenesis.precursor_tablet").withStyle(ChatFormatting.DARK_GRAY));
    }
}
