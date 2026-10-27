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
 * A player's essence state (core-systems.md section 3): one mana pool per aspect, the power level
 * reached for each aspect, which powers are switched on, and Temporal Exposure.
 * Immutable: every change returns a new instance, stored back with {@code setData}.
 */
public record PlayerEssence(
        Map<Aspect, Integer> pools,
        Map<Aspect, Integer> levels,
        List<Aspect> active,
        Aspect selected,
        int sizeMode,
        int exposure) {

    private static final Codec<Map<Aspect, Integer>> AMOUNTS = Codec.unboundedMap(Aspect.CODEC, ExtraCodecs.NON_NEGATIVE_INT);

    public static final Codec<PlayerEssence> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AMOUNTS.optionalFieldOf("pools", Map.of()).forGetter(PlayerEssence::pools),
            AMOUNTS.optionalFieldOf("levels", Map.of()).forGetter(PlayerEssence::levels),
            Aspect.CODEC.listOf().optionalFieldOf("active", List.of()).forGetter(PlayerEssence::active),
            Aspect.CODEC.optionalFieldOf("selected", Aspect.DENSITY).forGetter(PlayerEssence::selected),
            Codec.INT.optionalFieldOf("size_mode", 0).forGetter(PlayerEssence::sizeMode),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("exposure", 0).forGetter(PlayerEssence::exposure)
    ).apply(instance, PlayerEssence::new));

    public static final StreamCodec<ByteBuf, PlayerEssence> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public static final PlayerEssence EMPTY = new PlayerEssence(Map.of(), Map.of(), List.of(), Aspect.DENSITY, 0, 0);

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

    public boolean isActive(Aspect aspect) {
        return active.contains(aspect);
    }

    public PlayerEssence withPool(Aspect aspect, int amount) {
        return new PlayerEssence(put(pools, aspect, Math.max(0, amount)), levels, active, selected, sizeMode, exposure);
    }

    public PlayerEssence withLevel(Aspect aspect, int level) {
        return new PlayerEssence(pools, put(levels, aspect, level), active, selected, sizeMode, exposure);
    }

    public PlayerEssence withActive(Aspect aspect, boolean on) {
        Set<Aspect> set = active.isEmpty() ? EnumSet.noneOf(Aspect.class) : EnumSet.copyOf(active);
        if (on) {
            set.add(aspect);
        } else {
            set.remove(aspect);
        }
        return new PlayerEssence(pools, levels, List.copyOf(set), selected, sizeMode, exposure);
    }

    public PlayerEssence withSelected(Aspect aspect) {
        return new PlayerEssence(pools, levels, active, aspect, sizeMode, exposure);
    }

    public PlayerEssence withSizeMode(int mode) {
        return new PlayerEssence(pools, levels, active, selected, mode, exposure);
    }

    public PlayerEssence withExposure(int value) {
        return new PlayerEssence(pools, levels, active, selected, sizeMode, Math.max(0, value));
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
