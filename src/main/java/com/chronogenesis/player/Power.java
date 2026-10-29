package com.chronogenesis.player;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.Aspect;
import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * Every power a player can have (core-systems.md section 3). A power is either <b>passive</b> (switched on and
 * left running, burning its pool slowly; several can run at once) or <b>active</b> (one is selected, and using
 * it fires it once, or switches a lasting effect such as Slow Time on and off).
 * <p>
 * Single-essence powers come with the power level of their essence. <b>Composite</b> powers mix two essences:
 * they are learned at an Enhancing Gland fitted with a Confluence Needle, on an organism wired by veins to an
 * organism of the other essence, and they draw on both pools.
 * <p>
 * Strength (set with the Essence Manipulator) goes from 1 to {@link #maxStrength}: the lowest power level of its
 * essences, at most 3. Stronger costs more.
 */
public enum Power implements StringRepresentable {
    IRON_FISTS(Kind.PASSIVE, 1, Aspect.DENSITY),
    /**
     * Density, drawn the other way: the body made light. You take more harm from blows, but fall slowly, jump many
     * blocks high, and can jump again in mid-air.
     */
    FEATHERWEIGHT(Kind.PASSIVE, 2, Aspect.DENSITY),
    GLOWING_SIGHT(Kind.PASSIVE, 1, Aspect.AWARENESS),
    QUICKENING(Kind.PASSIVE, 1, Aspect.SPEED),
    SHRINK(Kind.PASSIVE, 1, Aspect.SIZE),
    GROW(Kind.PASSIVE, 1, Aspect.SIZE),
    REWIND(Kind.ACTIVE, 1, Aspect.TIME),
    SLOW_TIME(Kind.ACTIVE, 2, Aspect.TIME),
    TIME_FREEZE(Kind.ACTIVE, 3, Aspect.TIME),

    // Composites: two essences, one power. Each pair has exactly one.
    /** Density + Speed. "A heavy thing moving fast is hard to stop." (p = m v): a charge that bowls creatures over. */
    IMPETUS(Kind.ACTIVE, 1, Aspect.DENSITY, Aspect.SPEED),
    /** Density + Size. "Weight is heaviness spread over size." (m = rho V): drop like a stone, a shockwave where you land. */
    GROUND_SLAM(Kind.ACTIVE, 1, Aspect.DENSITY, Aspect.SIZE),
    /** Density + Awareness: the weight of your gaze. Hostile creatures that hunt you grow sluggish and weak. */
    DREAD(Kind.PASSIVE, 1, Aspect.DENSITY, Aspect.AWARENESS),
    /** Density + Time. "Heavy things keep time still.": projectiles crawl as they near you. */
    DILATION(Kind.PASSIVE, 1, Aspect.DENSITY, Aspect.TIME),
    /** Speed + Size: small and quick. You take steps up whole blocks and land soft from any fall you could survive. */
    SCURRY(Kind.PASSIVE, 1, Aspect.SPEED, Aspect.SIZE),
    /** Speed + Awareness: you see it coming and move first. Arrows and other projectiles glance off. */
    REFLEXES(Kind.PASSIVE, 1, Aspect.SPEED, Aspect.AWARENESS),
    /** Speed + Time: a bubble where everything moves at half speed for a few seconds, projectiles too. */
    STUTTER(Kind.ACTIVE, 1, Aspect.SPEED, Aspect.TIME),
    /** Size + Awareness: you become too small to be noticed. Invisible, and the creatures hunting you forget you. */
    OVERLOOK(Kind.ACTIVE, 1, Aspect.SIZE, Aspect.AWARENESS),
    /** Size + Time: your body's time runs back to before the wound. Heals, and washes out harmful effects. */
    REGROWTH(Kind.ACTIVE, 1, Aspect.SIZE, Aspect.TIME),
    /** Awareness + Time: you glimpse the next blow before it lands, and are not there when it does. */
    PREMONITION(Kind.PASSIVE, 1, Aspect.AWARENESS, Aspect.TIME),

    // Eldritch: one for each essence, at the depth only a gland driven by the Unbegotten's heart reaches (level 4).
    /** Density: a point of crushing weight where you look; creatures fall into it, and are crushed. */
    COLLAPSE(Kind.ACTIVE, 4, Aspect.DENSITY),
    /** Speed: you step through the gap between two moments, and are where you were looking. */
    SLIP(Kind.ACTIVE, 4, Aspect.SPEED),
    /** Size: tentacles burst out of the ground round you, flinging and holding what they catch. */
    WRITHE(Kind.ACTIVE, 4, Aspect.SIZE),
    /** Awareness: a thousand eyes open in you. Everything living near glows through stone; nothing blinds you. */
    THOUSAND_EYES(Kind.PASSIVE, 4, Aspect.AWARENESS),
    /** Time: you look at something and it lives an aeon: it withers, slows, and ages to nothing. */
    AEON(Kind.ACTIVE, 4, Aspect.TIME);

    public enum Kind {
        /** Switched on and left running. */
        PASSIVE,
        /** Selected, then used. */
        ACTIVE
    }

    public static final int MAX_STRENGTH = 3;

    public static final Codec<Power> CODEC = StringRepresentable.fromEnum(Power::values);
    public static final StreamCodec<ByteBuf, Power> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Power::ordinal);

    private final Kind kind;
    private final int minLevel;
    private final List<Aspect> aspects;

    Power(Kind kind, int minLevel, Aspect... aspects) {
        this.kind = kind;
        this.minLevel = minLevel;
        this.aspects = List.of(aspects);
    }

    public Kind kind() {
        return kind;
    }

    public boolean isPassive() {
        return kind == Kind.PASSIVE;
    }

    /** The power level its essence must reach (single-essence powers). */
    public int minLevel() {
        return minLevel;
    }

    public List<Aspect> aspects() {
        return aspects;
    }

    public boolean isComposite() {
        return aspects.size() > 1;
    }

    /** An Eldritch power: woken only by a gland with the Unbegotten's heart in it. */
    public boolean isEldritch() {
        return minLevel >= 4;
    }

    /** The composite power that mixes these two essences, or null if they are the same. */
    @Nullable
    public static Power composite(Aspect a, Aspect b) {
        for (Power power : values()) {
            if (power.isComposite() && power.aspects.contains(a) && power.aspects.contains(b) && a != b) {
                return power;
            }
        }
        return null;
    }

    /** Whether the player has this power: their level is high enough, and composites have been learned. */
    public boolean knownBy(PlayerEssence essence) {
        for (Aspect aspect : aspects) {
            if (essence.level(aspect) < minLevel) {
                return false;
            }
        }
        return !isComposite() || essence.composites().contains(this);
    }

    /** How strong it can be set: the lowest power level of its essences (at most 3). Time Freeze has one strength. */
    public int maxStrength(PlayerEssence essence) {
        if (this == TIME_FREEZE) {
            return 1;
        }
        int level = MAX_STRENGTH;
        for (Aspect aspect : aspects) {
            level = Math.min(level, essence.level(aspect));
        }
        return Math.max(1, level);
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "power.chronogenesis." + getSerializedName();
    }

    public Component displayName() {
        return Component.translatable(translationKey());
    }

    public Component description() {
        return Component.translatable(translationKey() + ".desc");
    }

    /** Its icon on the wheel and HUD: {@code textures/gui/power/<name>.png}. */
    public ResourceLocation icon() {
        return ChronoGenesis.id("textures/gui/power/" + getSerializedName() + ".png");
    }
}
