package com.chronogenesis.organism;

import java.util.Locale;

import net.minecraft.util.StringRepresentable;

/** What an organism core looks like and whether it lives. Updated by the core as it scans its body. */
public enum CorePhase implements StringRepresentable {
    /** Just seeded: a culture on a brass cradle inside its frame, no flesh grown yet. */
    SEED,
    /** Has grown flesh and is fed: the eye is open. */
    AWAKE,
    /** Has flesh but less than a quarter of its capacity stored: emaciated, eye half shut. */
    HUNGRY,
    /** Starved to death. It will not grow again. */
    NECROTIC;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
