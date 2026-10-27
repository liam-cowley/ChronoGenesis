package com.chronogenesis.essence;

import java.util.EnumMap;
import java.util.Map;

import com.mojang.serialization.Codec;

/**
 * How much of each aspect has been drained from a living entity (a data attachment, saved with the
 * entity). Storing what was taken rather than what is left means a datapack can change a profile
 * without corrupting old mobs. Drained essence does not come back by itself.
 */
public record EntityEssence(Map<Aspect, Integer> drained) {
    public static final EntityEssence NONE = new EntityEssence(Map.of());
    public static final Codec<EntityEssence> CODEC = Codec.unboundedMap(Aspect.CODEC, Codec.INT)
            .xmap(EntityEssence::new, EntityEssence::drained);

    public int drained(Aspect aspect) {
        return drained.getOrDefault(aspect, 0);
    }

    public EntityEssence withDrained(Aspect aspect, int amount) {
        Map<Aspect, Integer> copy = new EnumMap<>(Aspect.class);
        copy.putAll(drained);
        if (amount <= 0) {
            copy.remove(aspect);
        } else {
            copy.put(aspect, amount);
        }
        return new EntityEssence(copy);
    }

    public boolean isEmpty() {
        return drained.isEmpty();
    }
}
