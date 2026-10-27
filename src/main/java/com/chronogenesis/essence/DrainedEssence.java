package com.chronogenesis.essence;

import java.util.EnumMap;
import java.util.Map;

import com.mojang.serialization.Codec;

import net.minecraft.util.ExtraCodecs;

/**
 * How much of each aspect has been taken from a mob. Stored as an entity data attachment.
 * <p>
 * Storing what was <em>drained</em> (rather than what is left) means an untouched mob needs no
 * data at all, and changes to essence profiles apply to existing mobs automatically.
 * Drained essence does not come back on its own (core-systems.md section 1); Temporal Fields
 * will reduce these numbers later.
 */
public record DrainedEssence(Map<Aspect, Integer> drained) {
    public static final Codec<DrainedEssence> CODEC = Codec.unboundedMap(Aspect.CODEC, ExtraCodecs.NON_NEGATIVE_INT)
            .xmap(DrainedEssence::new, DrainedEssence::drained);

    public static final DrainedEssence NONE = new DrainedEssence(Map.of());

    public int get(Aspect aspect) {
        return drained.getOrDefault(aspect, 0);
    }

    public boolean isEmpty() {
        return drained.values().stream().allMatch(v -> v == 0);
    }

    public DrainedEssence with(Aspect aspect, int amount) {
        Map<Aspect, Integer> copy = new EnumMap<>(Aspect.class);
        copy.putAll(drained);
        if (amount <= 0) {
            copy.remove(aspect);
        } else {
            copy.put(aspect, amount);
        }
        return new DrainedEssence(Map.copyOf(copy));
    }
}
