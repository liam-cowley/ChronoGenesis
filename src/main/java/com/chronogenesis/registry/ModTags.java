package com.chronogenesis.registry;

import com.chronogenesis.ChronoGenesis;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

public final class ModTags {
    /** Mobs that carry Time essence early on: phantoms, endermen, the undead. Babies always do. */
    public static final TagKey<EntityType<?>> TIME_BEARERS = TagKey.create(Registries.ENTITY_TYPE, ChronoGenesis.id("time_bearers"));

    public static final TagKey<Item> NEEDLES = TagKey.create(Registries.ITEM, ChronoGenesis.id("needles"));

    private ModTags() {}
}
