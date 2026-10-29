package com.chronogenesis.mixin;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.chronogenesis.entity.PrecursorGuards;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.Vec3;

/**
 * A guard Warden (the Deep Kingdom's, see {@link PrecursorGuards}) does not sense, hunt or darken anyone it holds no
 * grudge against.
 */
@Mixin(Warden.class)
public abstract class WardenMixin {
    @Inject(method = "canTargetEntity", at = @At("HEAD"), cancellable = true)
    private void chronogenesis$guard(@Nullable Entity entity, CallbackInfoReturnable<Boolean> cir) {
        Warden self = (Warden) (Object) this;
        if (entity != null && PrecursorGuards.isGuard(self) && !PrecursorGuards.mayTarget(self, entity)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "applyDarknessAround", at = @At("HEAD"), cancellable = true)
    private static void chronogenesis$calm(ServerLevel level, Vec3 pos, @Nullable Entity source, int radius, CallbackInfo ci) {
        if (source != null && PrecursorGuards.calm(source)) {
            ci.cancel();
        }
    }
}
