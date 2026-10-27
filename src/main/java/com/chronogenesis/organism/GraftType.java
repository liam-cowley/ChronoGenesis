package com.chronogenesis.organism;

import java.util.Locale;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/** What a graft does. See core-systems.md section 2. */
public enum GraftType implements StringRepresentable {
    /** Opens the stored essence: fill containers by hand, or feed a jug placed in front of it. */
    TAP,
    /** Drains the organism's aspect from mobs in range: the basis of every farm. */
    LEECH_TENDRIL,
    /** Infuses the player with the organism's essence (mana for powers). */
    ENHANCING_GLAND;

    public static final Codec<GraftType> CODEC = StringRepresentable.fromEnum(GraftType::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
