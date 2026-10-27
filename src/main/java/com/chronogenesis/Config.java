package com.chronogenesis;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common (server-authoritative) config. Values are read lazily via {@code .get()}
 * and are only valid after the config has loaded, never in static initialisers.
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.push("organisms");
    }

    public static final ModConfigSpec.IntValue GROWTH_COST = BUILDER
            .comment("Drams of essence an organism spends to grow one block of flesh into its frame.")
            .defineInRange("growthCost", 4, 1, 1000);

    public static final ModConfigSpec.IntValue UPKEEP_INTERVAL = BUILDER
            .comment("Ticks between an organism's meals. Each meal costs 1 dram per 8 blocks of flesh.")
            .defineInRange("upkeepInterval", 1200, 20, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue MAX_ORGANISM_SIZE = BUILDER
            .comment("Largest number of flesh blocks a single organism can grow to.")
            .defineInRange("maxOrganismSize", 512, 1, 4096);

    static {
        BUILDER.pop();
        BUILDER.push("time");
    }

    public static final ModConfigSpec.IntValue TEAR_STRESS_THRESHOLD = BUILDER
            .comment("Temporal Stress in a chunk at which a Time Tear opens. Each dram of Time extracted adds 4.")
            .defineInRange("tearStressThreshold", 40, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue STRESS_DECAY_TICKS = BUILDER
            .comment("Ticks for one point of Temporal Stress to fade from a chunk.")
            .defineInRange("stressDecayTicks", 600, 1, Integer.MAX_VALUE);

    static {
        BUILDER.pop();
        BUILDER.push("powers");
    }

    public static final ModConfigSpec.IntValue POOL_PER_LEVEL = BUILDER
            .comment("Size of a player's essence pool per power level, in drams.")
            .defineInRange("poolPerLevel", 64, 1, 10000);

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
