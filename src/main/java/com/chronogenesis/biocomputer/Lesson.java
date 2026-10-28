package com.chronogenesis.biocomputer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Something the Chronocortex can be taught (core-systems.md section 5): show it the thing, and it learns what
 * the thing means. Data-driven: {@code data/<ns>/chronogenesis/lesson/<name>.json}. Synced so its screen can list them.
 * <p>
 * Lessons are how it grows aware. Each one costs room in its mind (lobes), and gives it awareness; together
 * with its lobes, awareness decides its stage. Some lessons change what it can do; those are checked in code by id
 * ({@link Lessons}): {@code words} lets it speak, {@code self} gives it a name.
 *
 * @param item      what it must be shown (used up)
 * @param stage     the stage it must have reached to understand this
 * @param capacity  lobes it needs to hold the lesson
 * @param awareness how much more aware it becomes
 * @param learned   what it says when it understands
 */
public record Lesson(Component title, Component description, Ingredient item, int stage, int capacity, int awareness,
                     Component learned, int order) {
    public static final Codec<Lesson> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("title").forGetter(Lesson::title),
            ComponentSerialization.CODEC.fieldOf("description").forGetter(Lesson::description),
            Ingredient.CODEC_NONEMPTY.fieldOf("item").forGetter(Lesson::item),
            Codec.intRange(0, 16).optionalFieldOf("stage", 0).forGetter(Lesson::stage),
            Codec.intRange(0, 10_000).optionalFieldOf("capacity", 0).forGetter(Lesson::capacity),
            Codec.intRange(0, 100).optionalFieldOf("awareness", 1).forGetter(Lesson::awareness),
            ComponentSerialization.CODEC.fieldOf("learned").forGetter(Lesson::learned),
            Codec.INT.optionalFieldOf("order", 0).forGetter(Lesson::order)
    ).apply(instance, Lesson::new));
}
