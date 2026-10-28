package com.chronogenesis.player;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.chronogenesis.essence.Aspect;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * Rewind, the first Time power: you return to where you stood a few seconds ago (5 per strength), facing the way
 * you faced. Only your place is rewound: wounds, hunger and what you carry stay as they are now.
 * The server keeps every player's trail for the longest rewind there is.
 */
public final class Rewind {
    public static final int SECONDS_PER_STRENGTH = 5;
    /** Drams of Time per strength. */
    public static final int COST = 4;
    private static final int KEPT = SECONDS_PER_STRENGTH * Power.MAX_STRENGTH * 20 + 1;
    /** A trail shorter than a second is too short to rewind. */
    private static final int MIN_TRAIL = 20;

    private record Mark(Vec3 pos, float yRot, float xRot) {}

    private static final Map<UUID, ArrayDeque<Mark>> TRAILS = new HashMap<>();

    private Rewind() {}

    /** Each tick: where the player is now. */
    public static void record(ServerPlayer player) {
        ArrayDeque<Mark> trail = TRAILS.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
        if (player.isPassenger() || player.isSleeping()) {
            trail.clear();
            return;
        }
        trail.addLast(new Mark(player.position(), player.getYRot(), player.getXRot()));
        while (trail.size() > KEPT) {
            trail.removeFirst();
        }
    }

    static void use(ServerPlayer player, int strength) {
        ArrayDeque<Mark> trail = TRAILS.get(player.getUUID());
        if (trail == null || trail.size() < MIN_TRAIL) {
            player.displayClientMessage(Component.translatable("message.chronogenesis.power.rewind.too_soon")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (!Powers.spend(player, Aspect.TIME, COST * strength)) {
            Powers.empty(player, Aspect.TIME);
            return;
        }
        int back = Math.min(trail.size() - 1, SECONDS_PER_STRENGTH * 20 * strength);
        // The trail, oldest first: walk from the newest back to the mark, leaving a wake of reversed sparks.
        Mark target = null;
        int index = 0;
        int from = trail.size() - 1 - back;
        ServerLevel level = player.serverLevel();
        for (Iterator<Mark> it = trail.iterator(); it.hasNext(); index++) {
            Mark mark = it.next();
            if (index == from) {
                target = mark;
            }
            if (index >= from && index % 4 == 0) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, mark.pos.x, mark.pos.y + 1.0, mark.pos.z, 2, 0.2, 0.4, 0.2, 0.0);
            }
        }
        if (target == null) {
            return;
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8f, 1.8f);
        if (player instanceof FakePlayer) {
            // A fake player has no client to answer the teleport: place it directly.
            player.moveTo(target.pos.x, target.pos.y, target.pos.z, target.yRot, target.xRot);
        } else {
            player.teleportTo(level, target.pos.x, target.pos.y, target.pos.z, target.yRot, target.xRot);
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 0.5f);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, target.pos.x, target.pos.y + 1.0, target.pos.z, 40, 0.3, 0.8, 0.3, 0.05);
        Powers.expose(player, 3 * strength);
        // The trail behind you is spent: you are back at its start.
        trail.clear();
    }

    static void forget(UUID player) {
        TRAILS.remove(player);
    }
}
