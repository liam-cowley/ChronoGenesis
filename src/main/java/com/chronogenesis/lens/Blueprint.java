package com.chronogenesis.lens;

import com.chronogenesis.ChronoGenesis;
import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * The machines the scientist before you drew, and the old papers they learned from (a castle's ledger, an
 * observatory's star chart, the drowned specimen house's notes). A blueprint, read through the Lens or found intact,
 * becomes the clue {@code chronogenesis:blueprint_<name>}, which research topics accept as an alternative to
 * witnessed clues.
 */
public enum Blueprint implements StringRepresentable {
    /** The Umbilical Locket. */
    LOCKET("locket"),
    /** The Temporal Field. */
    FIELD("field"),
    /** The Ruined Castle's ledger: the lord's wagers on a race whose winner came home before he set out (a Law). */
    LEDGER("ledger"),
    /** The Collapsed Observatory's star chart: a pendulum's swings counted against the stars, slowing. */
    PENDULUM("pendulum"),
    /** The Drowned Specimen House's notes on gills: a graft that breathes for its host. */
    GILLS("gills");

    public static final Codec<Blueprint> CODEC = StringRepresentable.fromEnum(Blueprint::values);
    public static final StreamCodec<ByteBuf, Blueprint> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Blueprint::ordinal);

    private final String name;

    Blueprint(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public ResourceLocation clue() {
        return ChronoGenesis.id("blueprint_" + name);
    }
}
