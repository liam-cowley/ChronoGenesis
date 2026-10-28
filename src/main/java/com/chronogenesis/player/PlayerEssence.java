package com.chronogenesis.player;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.chronogenesis.Config;
import com.chronogenesis.essence.Aspect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

/**
 * A player's essence state (core-systems.md section 3): one mana pool per aspect, the power level reached for
 * each aspect, the passive powers switched on, the active power selected, the strength set for each power,
 * the composite powers learned, and Temporal Exposure.
 * Immutable: every change returns a new instance, stored back with {@code setData}.
 */
public record PlayerEssence(
        Map<Aspect, Integer> pools,
        Map<Aspect, Integer> levels,
        List<Power> passives,
        Power selected,
        Map<Power, Integer> strengths,
        List<Power> composites,
        int exposure) {

    private static final Codec<Map<Aspect, Integer>> AMOUNTS = Codec.unboundedMap(Aspect.CODEC, ExtraCodecs.NON_NEGATIVE_INT);

    public static final Codec<PlayerEssence> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AMOUNTS.optionalFieldOf("pools", Map.of()).forGetter(PlayerEssence::pools),
            AMOUNTS.optionalFieldOf("levels", Map.of()).forGetter(PlayerEssence::levels),
            Power.CODEC.listOf().optionalFieldOf("passives", List.of()).forGetter(PlayerEssence::passives),
            Power.CODEC.optionalFieldOf("selected_power", Power.REWIND).forGetter(PlayerEssence::selected),
            Codec.unboundedMap(Power.CODEC, ExtraCodecs.POSITIVE_INT).optionalFieldOf("strengths", Map.of())
                    .forGetter(PlayerEssence::strengths),
            Power.CODEC.listOf().optionalFieldOf("composites", List.of()).forGetter(PlayerEssence::composites),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("exposure", 0).forGetter(PlayerEssence::exposure)
    ).apply(instance, PlayerEssence::new));

    public static final StreamCodec<ByteBuf, PlayerEssence> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public static final PlayerEssence EMPTY = new PlayerEssence(Map.of(), Map.of(), List.of(), Power.REWIND, Map.of(), List.of(), 0);

    public int pool(Aspect aspect) {
        return pools.getOrDefault(aspect, 0);
    }

    public int level(Aspect aspect) {
        return levels.getOrDefault(aspect, 0);
    }

    /** Pool capacity: grows with the power level for that aspect. */
    public int max(Aspect aspect) {
        return level(aspect) * Config.POOL_PER_LEVEL.getAsInt();
    }

    /** Whether a passive power is switched on. */
    public boolean isOn(Power power) {
        return passives.contains(power);
    }

    public boolean knows(Power power) {
        return power.knownBy(this);
    }

    /** The strength the player set for a power, kept within what they can reach now (full strength by default). */
    public int strength(Power power) {
        int max = power.maxStrength(this);
        return Math.min(max, strengths.getOrDefault(power, max));
    }

    public PlayerEssence withPool(Aspect aspect, int amount) {
        return new PlayerEssence(put(pools, aspect, Math.max(0, amount)), levels, passives, selected, strengths, composites, exposure);
    }

    public PlayerEssence withLevel(Aspect aspect, int level) {
        return new PlayerEssence(pools, put(levels, aspect, level), passives, selected, strengths, composites, exposure);
    }

    public PlayerEssence withPassive(Power power, boolean on) {
        Set<Power> set = passives.isEmpty() ? EnumSet.noneOf(Power.class) : EnumSet.copyOf(passives);
        if (on) {
            set.add(power);
        } else {
            set.remove(power);
        }
        return new PlayerEssence(pools, levels, List.copyOf(set), selected, strengths, composites, exposure);
    }

    public PlayerEssence withSelected(Power power) {
        return new PlayerEssence(pools, levels, passives, power, strengths, composites, exposure);
    }

    public PlayerEssence withStrength(Power power, int strength) {
        Map<Power, Integer> copy = new EnumMap<>(Power.class);
        copy.putAll(strengths);
        copy.put(power, Math.max(1, strength));
        return new PlayerEssence(pools, levels, passives, selected, Map.copyOf(copy), composites, exposure);
    }

    public PlayerEssence withComposite(Power power) {
        if (composites.contains(power)) {
            return this;
        }
        Set<Power> set = composites.isEmpty() ? EnumSet.noneOf(Power.class) : EnumSet.copyOf(composites);
        set.add(power);
        return new PlayerEssence(pools, levels, passives, selected, strengths, List.copyOf(set), exposure);
    }

    public PlayerEssence withExposure(int value) {
        return new PlayerEssence(pools, levels, passives, selected, strengths, composites, Math.max(0, value));
    }

    private static Map<Aspect, Integer> put(Map<Aspect, Integer> map, Aspect aspect, int value) {
        Map<Aspect, Integer> copy = new EnumMap<>(Aspect.class);
        copy.putAll(map);
        if (value == 0) {
            copy.remove(aspect);
        } else {
            copy.put(aspect, value);
        }
        return Map.copyOf(copy);
    }
}
