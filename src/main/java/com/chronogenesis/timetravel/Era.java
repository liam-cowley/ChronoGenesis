package com.chronogenesis.timetravel;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * A time the Chronocortex can shift a player into (core-systems.md section 5, "Temporal shifts"). Data-driven:
 * {@code data/<ns>/chronogenesis/era/<name>.json}. Synced so its screen can list them.
 *
 * @param dimension the dimension that is this era (it shares the overworld's seed, so the land lines up)
 * @param stage     the Chronocortex stage needed to reach this far back
 * @param lesson    a lesson it must have learned first (it cannot find a time it does not understand)
 * @param memory    an era it must remember first (an offshoot planted there, linked back): each era leads to the next
 * @param cost      drams of Time, drawn from its wired organisms
 * @param minutes   how long the shift holds before time snaps the traveller back
 * @param clue      noticed on arrival
 */
public record Era(Component title, Component description, Item icon, ResourceKey<Level> dimension, int stage,
                  Optional<ResourceLocation> lesson, Optional<ResourceLocation> memory, int cost, int minutes, Optional<ResourceLocation> clue, int order) {
    public static final Codec<Era> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("title").forGetter(Era::title),
            ComponentSerialization.CODEC.fieldOf("description").forGetter(Era::description),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("icon").forGetter(Era::icon),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Era::dimension),
            Codec.intRange(0, 16).optionalFieldOf("stage", 1).forGetter(Era::stage),
            ResourceLocation.CODEC.optionalFieldOf("lesson").forGetter(Era::lesson),
            ResourceLocation.CODEC.optionalFieldOf("requires_memory").forGetter(Era::memory),
            Codec.intRange(0, 100_000).optionalFieldOf("cost", 0).forGetter(Era::cost),
            Codec.intRange(1, 1440).optionalFieldOf("minutes", 5).forGetter(Era::minutes),
            ResourceLocation.CODEC.optionalFieldOf("clue").forGetter(Era::clue),
            Codec.INT.optionalFieldOf("order", 0).forGetter(Era::order)
    ).apply(instance, Era::new));
}
