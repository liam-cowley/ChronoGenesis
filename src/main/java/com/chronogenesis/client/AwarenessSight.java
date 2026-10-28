package com.chronogenesis.client;

import com.chronogenesis.player.PlayerEssence;
import com.chronogenesis.player.Power;
import com.chronogenesis.player.Powers;
import com.chronogenesis.registry.ModAttachments;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Glowing Sight (Awareness): creatures (and players who are not sneaking) within 20 blocks glow, for you only. */
public final class AwarenessSight {
    private AwarenessSight() {}

    public static boolean shouldGlow(Entity entity) {
        LocalPlayer self = Minecraft.getInstance().player;
        if (self == null || entity == self || !(entity instanceof LivingEntity)) {
            return false;
        }
        PlayerEssence essence = self.getData(ModAttachments.PLAYER_ESSENCE);
        if (!essence.isOn(Power.GLOWING_SIGHT)) {
            return false;
        }
        int strength = essence.strength(Power.GLOWING_SIGHT);
        // Strength 2 sees through sneaking.
        if (entity instanceof Player other && other.isShiftKeyDown() && strength < 2) {
            return false;
        }
        double radius = Powers.AWARENESS_RADIUS + 10.0 * (strength - 1);
        return entity.distanceToSqr(self) <= radius * radius;
    }
}
