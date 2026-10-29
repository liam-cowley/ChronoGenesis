package com.chronogenesis.timetravel;

import java.util.Collection;
import java.util.List;
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
 * @param routes    other ways in: any one route (a memory and/or a lesson) will do, so eras branch rather than queue.
 *                  Empty: only {@code memory} and {@code lesson} count
 * @param cost      drams of Time, drawn from its wired organisms
 * @param minutes   how long the shift holds before time snaps the traveller back
 * @param clue      noticed on arrival
 * @param toxicAir  its air burns the lungs (the Hadean): only Time-warded goggles filter it
 * @param lens      whether the Temporal Lens can look into it once visited (not Before Time: there is nothing to see)
 */
public record Era(Component title, Component description, Item icon, ResourceKey<Level> dimension, int stage,
                  Optional<ResourceLocation> lesson, Optional<ResourceLocation> memory, List<Route> routes, int cost, int minutes,
                  Optional<ResourceLocation> clue, int order, boolean toxicAir, boolean lens) {
    /** One way into an era: a memory it must hold and/or a lesson it must have learned. */
    public record Route(Optional<ResourceLocation> memory, Optional<ResourceLocation> lesson) {
        public static final Codec<Route> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.optionalFieldOf("memory").forGetter(Route::memory),
                ResourceLocation.CODEC.optionalFieldOf("lesson").forGetter(Route::lesson)
        ).apply(instance, Route::new));

        public boolean open(Collection<ResourceLocation> lessons, Collection<ResourceLocation> memories) {
            return memory.map(memories::contains).orElse(true) && lesson.map(lessons::contains).orElse(true);
        }
    }

    public static final Codec<Era> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("title").forGetter(Era::title),
            ComponentSerialization.CODEC.fieldOf("description").forGetter(Era::description),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("icon").forGetter(Era::icon),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Era::dimension),
            Codec.intRange(0, 16).optionalFieldOf("stage", 1).forGetter(Era::stage),
            ResourceLocation.CODEC.optionalFieldOf("lesson").forGetter(Era::lesson),
            ResourceLocation.CODEC.optionalFieldOf("requires_memory").forGetter(Era::memory),
            Route.CODEC.listOf().optionalFieldOf("routes", List.of()).forGetter(Era::routes),
            Codec.intRange(0, 100_000).optionalFieldOf("cost", 0).forGetter(Era::cost),
            Codec.intRange(1, 1440).optionalFieldOf("minutes", 5).forGetter(Era::minutes),
            ResourceLocation.CODEC.optionalFieldOf("clue").forGetter(Era::clue),
            Codec.INT.optionalFieldOf("order", 0).forGetter(Era::order),
            Codec.BOOL.optionalFieldOf("toxic_air", false).forGetter(Era::toxicAir),
            Codec.BOOL.optionalFieldOf("lens", true).forGetter(Era::lens)
    ).apply(instance, Era::new));
}
