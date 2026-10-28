package com.chronogenesis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.chronogenesis.time.TimeZones;

import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;

/** A tick scheduled inside a slowed chunk waits longer, by the chunk's rate (see {@link TimeZones#stretch}). */
@Mixin(LevelTicks.class)
public abstract class LevelTicksMixin<T> {
    @ModifyVariable(method = "schedule", at = @At("HEAD"), argsOnly = true)
    private ScheduledTick<T> chronogenesis$stretch(ScheduledTick<T> tick) {
        @SuppressWarnings("unchecked")
        LevelTicks<T> self = (LevelTicks<T>) (Object) this;
        return TimeZones.stretch(self, tick);
    }
}
