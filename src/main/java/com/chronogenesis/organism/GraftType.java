package com.chronogenesis.organism;

import java.util.Locale;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/** What a graft does. See core-systems.md sections 2 and 4. */
public enum GraftType implements StringRepresentable {
    /** Opens the stored essence: fill containers by hand, feed a jug in front of it, or push through veins. */
    TAP,
    /** Receives essence arriving through veins (the input to a Tap's output). */
    MAW,
    /** Drains the organism's aspect from mobs in range: the basis of every farm. */
    LEECH_TENDRIL,
    /** Infuses the player with the organism's essence (mana for powers). */
    ENHANCING_GLAND,
    /** Holds Time still. A Time organism without enough of these grows unstable and bursts. */
    STASIS_BLADDER,
    /** Racetrack start, on a Speed organism: sends pulses of Speed down the veins. */
    STARTING_GATE,
    /** Racetrack finish, on a Time organism: the time a pulse spent travelling condenses here. */
    FINISH;

    public static final Codec<GraftType> CODEC = StringRepresentable.fromEnum(GraftType::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
