package com.chronogenesis.entity;

import java.util.List;

import com.chronogenesis.item.FossilItem;
import com.chronogenesis.registry.ModDataComponents;
import com.chronogenesis.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** A dinosaur's egg as an item: named for what is in it ({@code dino} component), placed as a {@link DinoEggBlock}. */
public class DinoEggItem extends BlockItem {
    public DinoEggItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static ItemStack of(EntityType<?> type) {
        ItemStack stack = new ItemStack(ModItems.DINO_EGG.get());
        stack.set(ModDataComponents.DINO.get(), BuiltInRegistries.ENTITY_TYPE.getKey(type));
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        EntityType<?> type = FossilItem.species(stack);
        return type == null ? super.getName(stack) : Component.translatable(getDescriptionId() + ".of", type.getDescription());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.chronogenesis.dino_egg.desc", DinoEggBlock.HATCH_AT).withStyle(ChatFormatting.GRAY));
    }
}
