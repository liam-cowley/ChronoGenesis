package com.chronogenesis;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common (server-authoritative) config. Values are read lazily via {@code .get()}
 * and are only valid after the config has loaded, never in static initialisers.
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.push("essence");
    }

    public static final ModConfigSpec.IntValue SYRINGE_COOLDOWN_TICKS = BUILDER
            .comment("Ticks between two draws with a Chrono Syringe.")
            .defineInRange("syringeCooldownTicks", 10, 0, 200);

    static {
        BUILDER.pop();
        BUILDER.push("organisms");
    }

    public static final ModConfigSpec.IntValue ORGANISM_GROWTH_COST = BUILDER
            .comment("Drams an organism spends to grow one block of flesh into its armature.")
            .defineInRange("growthCost", 2, 1, 64);

    public static final ModConfigSpec.IntValue ORGANISM_BASE_CAPACITY = BUILDER
            .comment("Drams an organism core can store on its own.")
            .defineInRange("baseCapacity", 16, 1, 4096);

    public static final ModConfigSpec.IntValue ORGANISM_CAPACITY_PER_FLESH = BUILDER
            .comment("Extra storage per block of grown flesh.")
            .defineInRange("capacityPerFlesh", 8, 0, 4096);

    public static final ModConfigSpec.IntValue ORGANISM_UPKEEP_INTERVAL = BUILDER
            .comment("Ticks between feedings. Each feeding burns 1 dram plus 1 per 8 flesh blocks; a starving organism eats its own flesh.")
            .defineInRange("upkeepIntervalTicks", 1200, 20, 72000);

    public static final ModConfigSpec.IntValue LEECH_RANGE = BUILDER
            .comment("How far (in blocks) a Leech Tendril reaches for creatures to drain.")
            .defineInRange("leechRange", 5, 1, 16);

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
