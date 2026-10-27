package com.chronogenesis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.chronogenesis.client.AwarenessSight;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Lets the Awareness power make entities glow for the local player only. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void chronogenesis$awarenessGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (AwarenessSight.shouldGlow(entity)) {
            cir.setReturnValue(true);
        }
    }
}
