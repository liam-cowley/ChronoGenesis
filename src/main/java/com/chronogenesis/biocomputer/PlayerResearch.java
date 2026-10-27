package com.chronogenesis.biocomputer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * What a player has found out: every Clue ever noticed, the ones already spent on research, and the
 * research topics completed. A spent clue stays known, so witnessing the same thing again gives nothing.
 */
public record PlayerResearch(Set<ResourceLocation> known, Set<ResourceLocation> spent, Set<ResourceLocation> completed) {
    public static final PlayerResearch EMPTY = new PlayerResearch(Set.of(), Set.of(), Set.of());

    private static final Codec<Set<ResourceLocation>> SET = ResourceLocation.CODEC.listOf()
            .xmap(Set::copyOf, List::copyOf);

    public static final Codec<PlayerResearch> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SET.optionalFieldOf("known", Set.of()).forGetter(PlayerResearch::known),
            SET.optionalFieldOf("spent", Set.of()).forGetter(PlayerResearch::spent),
            SET.optionalFieldOf("completed", Set.of()).forGetter(PlayerResearch::completed)
    ).apply(instance, PlayerResearch::new));

    public static final StreamCodec<ByteBuf, PlayerResearch> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    /** Known and not yet spent. */
    public boolean hasClue(ResourceLocation clue) {
        return known.contains(clue) && !spent.contains(clue);
    }

    public boolean isCompleted(ResourceLocation topic) {
        return completed.contains(topic);
    }

    public PlayerResearch withKnown(ResourceLocation clue) {
        return new PlayerResearch(plus(known, List.of(clue)), spent, completed);
    }

    public PlayerResearch withSpent(List<ResourceLocation> clues) {
        return new PlayerResearch(known, plus(spent, clues), completed);
    }

    public PlayerResearch withCompleted(ResourceLocation topic) {
        return new PlayerResearch(known, spent, plus(completed, List.of(topic)));
    }

    private static Set<ResourceLocation> plus(Set<ResourceLocation> set, List<ResourceLocation> extra) {
        Set<ResourceLocation> copy = new HashSet<>(set);
        copy.addAll(extra);
        return Set.copyOf(copy);
    }
}
