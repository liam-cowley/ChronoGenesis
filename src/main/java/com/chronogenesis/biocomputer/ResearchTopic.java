package com.chronogenesis.biocomputer;

import java.util.List;
import java.util.Map;

import com.chronogenesis.essence.Aspect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * A research topic the Chronocortex can work through (core-systems.md section 5). Data-driven:
 * {@code data/<ns>/chronogenesis/research/<name>.json}. Synced to clients so the research screen can show it.
 *
 * @param stage     the Chronocortex stage needed to see this far
 * @param requires  topics that must be completed first
 * @param essence   drams drawn from organisms wired to the Chronocortex with Ganglions
 * @param materials items taken from the researcher's inventory
 * @param clues     alternative clue sets: any one complete set will do, and is spent. Empty = no clues needed
 * @param order     position in the research list
 * @param rewards   items the Chronocortex grows for the researcher on completion. A completed topic with rewards
 *                  can be grown again for its essence and materials (no clues), so a lost item is not lost forever
 */
public record ResearchTopic(Component title, Component description, Item icon, int stage, List<ResourceLocation> requires,
                            Map<Aspect, Integer> essence, List<SizedIngredient> materials,
                            List<List<ResourceLocation>> clues, int order, List<ItemStack> rewards) {
    public static final Codec<ResearchTopic> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("title").forGetter(ResearchTopic::title),
            ComponentSerialization.CODEC.fieldOf("description").forGetter(ResearchTopic::description),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("icon").forGetter(ResearchTopic::icon),
            Codec.intRange(0, 16).optionalFieldOf("stage", 0).forGetter(ResearchTopic::stage),
            ResourceLocation.CODEC.listOf().optionalFieldOf("requires", List.of()).forGetter(ResearchTopic::requires),
            Codec.unboundedMap(Aspect.CODEC, Codec.intRange(1, 100_000)).optionalFieldOf("essence", Map.of())
                    .forGetter(ResearchTopic::essence),
            SizedIngredient.FLAT_CODEC.listOf().optionalFieldOf("materials", List.of()).forGetter(ResearchTopic::materials),
            ResourceLocation.CODEC.listOf().listOf().optionalFieldOf("clues", List.of()).forGetter(ResearchTopic::clues),
            Codec.INT.optionalFieldOf("order", 0).forGetter(ResearchTopic::order),
            ItemStack.CODEC.listOf().optionalFieldOf("rewards", List.of()).forGetter(ResearchTopic::rewards)
    ).apply(instance, ResearchTopic::new));
}
