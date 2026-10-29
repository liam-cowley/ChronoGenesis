package com.chronogenesis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.chronogenesis.entity.CargoHold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

/**
 * Reach to a block in a Titanosaur's hold ({@link CargoHold}) is measured to where the block is on the beast: so a
 * chest on its back stays open while its user stands by it.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "canInteractWithBlock", at = @At("HEAD"), cancellable = true)
    private void chronogenesis$cargoReach(BlockPos pos, double distance, CallbackInfoReturnable<Boolean> cir) {
        Boolean reach = CargoHold.canReach((Player) (Object) this, pos, distance);
        if (reach != null) {
            cir.setReturnValue(reach);
        }
    }
}
