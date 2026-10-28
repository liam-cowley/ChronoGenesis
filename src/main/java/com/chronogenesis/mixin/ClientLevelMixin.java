package com.chronogenesis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.chronogenesis.time.TimeZones;

import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Whoever stands in a chunk with its own time sees its own sky: the sun and moon (and the moon's phase) follow
 * the chunk's clock, not the world's. Only what reads the time of day is changed; the world's clock still ticks
 * on underneath, so nothing drifts.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Shadow
    public abstract ClientLevel.ClientLevelData getLevelData();

    /** Overrides {@code LevelAccessor.dayTime()}, which the time of day and the moon phase are read from. */
    public long dayTime() {
        return getLevelData().getDayTime() - TimeZones.clientDayOffset();
    }
}
