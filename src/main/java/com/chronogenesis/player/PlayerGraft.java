package com.chronogenesis.player;

import java.util.Locale;

import com.chronogenesis.registry.ModItems;
import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;

/**
 * Grafts for a body (core-systems.md section 5): pieces of the Unbegotten, left when it dies, grown into whoever cuts
 * them into themselves ({@link PlayerGrafts}). Each changes the body for good, and shows on it.
 */
public enum PlayerGraft implements StringRepresentable {
    /** A third eye, in the forehead: it sees in the dark, and nothing blinds it. */
    UNBLINKING_EYE,
    /** Tendrils from the spine, writhing: they grip walls, and you climb. */
    DORSAL_TENDRILS,
    /** A tentacle wound round the right arm, its tip past the hand: it reaches further. */
    GRASPING_TENTACLE,
    /** Plates of the Unbegotten's hide over the chest and shoulders: hard as horn. */
    UNBEGOTTEN_HIDE;

    public static final Codec<PlayerGraft> CODEC = StringRepresentable.fromEnum(PlayerGraft::values);
    public static final StreamCodec<ByteBuf, PlayerGraft> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], PlayerGraft::ordinal);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Item item() {
        return switch (this) {
            case UNBLINKING_EYE -> ModItems.UNBLINKING_EYE.get();
            case DORSAL_TENDRILS -> ModItems.DORSAL_TENDRILS.get();
            case GRASPING_TENTACLE -> ModItems.GRASPING_TENTACLE.get();
            case UNBEGOTTEN_HIDE -> ModItems.UNBEGOTTEN_HIDE.get();
        };
    }
}
