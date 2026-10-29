package com.chronogenesis.item;

import java.util.List;
import javax.annotation.Nullable;

import com.chronogenesis.registry.ModDataComponents;
import com.chronogenesis.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A fossil of one kind of dinosaur ({@code dino} component): what is left in a beast of the Jurassic once all its Time
 * has been drawn out, the shape of it set hard. Shown to the Chronocortex it learns the creature, and can bud its
 * eggs from its own flesh. The same item names the egg ({@link #species}).
 */
public class FossilItem extends Item {
    public FossilItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(EntityType<?> type) {
        ItemStack stack = new ItemStack(ModItems.FOSSIL.get());
        stack.set(ModDataComponents.DINO.get(), BuiltInRegistries.ENTITY_TYPE.getKey(type));
        return stack;
    }

    @Nullable
    public static EntityType<?> species(ItemStack stack) {
        ResourceLocation id = stack.get(ModDataComponents.DINO.get());
        return id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
    }

    @Override
    public Component getName(ItemStack stack) {
        EntityType<?> type = species(stack);
        return type == null ? super.getName(stack) : Component.translatable(getDescriptionId() + ".of", type.getDescription());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
