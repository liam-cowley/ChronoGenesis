package com.chronogenesis.registry;

import com.chronogenesis.ChronoGenesis;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class ModTags {
    public static final class Entities {
        /** Mobs that hold Time essence as adults: phantoms, endermen, undead. */
        public static final TagKey<EntityType<?>> TIME_BEARERS = TagKey.create(Registries.ENTITY_TYPE, ChronoGenesis.id("time_bearers"));

        private Entities() {}
    }

    public static final class Items {
        /** Pieces of the Chrono-Insulated Suit: each one worn blocks a quarter of Temporal Exposure. */
        public static final TagKey<net.minecraft.world.item.Item> CHRONO_INSULATED =
                TagKey.create(Registries.ITEM, ChronoGenesis.id("chrono_insulated"));

        private Items() {}
    }

    private ModTags() {}
}
