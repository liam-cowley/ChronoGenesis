package com.chronogenesis;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common (server-authoritative) config. Values are read lazily via {@code .get()}
 * and are only valid after the config has loaded, never in static initialisers.
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.push("temporal_essence");
    }

    public static final ModConfigSpec.IntValue MAX_ENTITY_ESSENCE = BUILDER
            .comment("Maximum temporal essence a single living entity can hold.")
            .defineInRange("maxEntityEssence", 1000, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue ESSENCE_REGEN_TICKS = BUILDER
            .comment("Ticks between each point of natural essence regeneration on a living entity. 0 disables regeneration.")
            .defineInRange("essenceRegenTicks", 200, 0, Integer.MAX_VALUE);

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
