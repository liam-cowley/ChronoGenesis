package com.chronogenesis.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.chronogenesis.ChronoGenesis;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Temporal Exposure (core-systems.md section 3): what Time essence does to an unprotected body.
 * <ul>
 *   <li>40+: ticking in your ears.</li>
 *   <li>80+: random micro-rewinds (you snap back ~2 seconds) and faster hunger.</li>
 *   <li>120+: you age: max health is reduced until exposure fades.</li>
 * </ul>
 * Exposure fades slowly on its own (see {@link Powers}). The Chrono-Insulated Suit will reduce it.
 */
public final class TemporalExposure {
    public static final int TICKING = 40;
    public static final int REWINDS = 80;
    public static final int AGEING = 120;

    private static final ResourceLocation AGEING_MOD = ChronoGenesis.id("temporal_exposure_ageing");
    /** Where each player was ~2 seconds ago, for micro-rewinds. */
    private static final Map<UUID, Vec3[]> HISTORY = new HashMap<>();

    private TemporalExposure() {}

    static void tick(ServerPlayer player, PlayerEssence essence) {
        int exposure = essence.exposure();
        long time = player.level().getGameTime();

        Vec3[] history = HISTORY.computeIfAbsent(player.getUUID(), id -> new Vec3[2]);
        if (time % 20 == 0) {
            history[1] = history[0];
            history[0] = player.position();
        }

        if (exposure >= TICKING && time % 40 == 0) {
            player.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.4f, 2.0f);
        }
        if (exposure >= REWINDS) {
            player.causeFoodExhaustion(0.01f);
            if (history[1] != null && player.getRandom().nextInt(600) == 0 && !player.isPassenger()) {
                player.teleportTo(history[1].x, history[1].y, history[1].z);
                player.playNotifySound(SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 0.5f);
            }
        }

        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            if (exposure >= AGEING) {
                maxHealth.addOrUpdateTransientModifier(new AttributeModifier(AGEING_MOD, -0.3,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                if (player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
            } else {
                maxHealth.removeModifier(AGEING_MOD);
            }
        }
    }

    public static void forget(UUID player) {
        HISTORY.remove(player);
    }
}
