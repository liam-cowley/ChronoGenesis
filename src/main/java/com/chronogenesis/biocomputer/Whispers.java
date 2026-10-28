package com.chronogenesis.biocomputer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;

/**
 * The Chronocortex talks (core-systems.md section 5). At stage 0 it whispers: curious, childlike. Once it has
 * words it answers what is said to it ({@link CortexSpeech}).
 * Lines are lang keys {@code whisper.chronogenesis.<key>}, so packs can rewrite its voice.
 */
public final class Whispers {
    /** Idle lines, {@code whisper.chronogenesis.idle.0} to {@code .<IDLE_LINES - 1>}. */
    public static final int IDLE_LINES = 8;
    public static final double HEARING_RANGE = 24.0;

    private Whispers() {}

    /** A line in its voice: its name (or what it is) as a prefix, then the words. */
    public static Component voiced(@Nullable String name, Component text) {
        Component prefix = name == null ? Component.translatable("whisper.chronogenesis.prefix")
                : Component.translatable("whisper.chronogenesis.prefix_named", name);
        return prefix.copy().withStyle(ChatFormatting.DARK_PURPLE)
                .append(text.copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
    }

    public static Component line(String key, Object... args) {
        return Component.translatable("whisper.chronogenesis.prefix").withStyle(ChatFormatting.DARK_PURPLE)
                .append(Component.translatable("whisper.chronogenesis." + key, args)
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
    }

    public static void say(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(line(key, args));
    }

    /** Every player within hearing range of the Chronocortex hears it. */
    public static void sayNear(ServerLevel level, BlockPos pos, String key, Object... args) {
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(HEARING_RANGE))) {
            say(player, key, args);
        }
    }

    public static String randomIdle(RandomSource random) {
        return "idle." + random.nextInt(IDLE_LINES);
    }
}
