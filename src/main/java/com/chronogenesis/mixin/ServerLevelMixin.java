package com.chronogenesis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.chronogenesis.time.TimeZones;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;

/**
 * Random and scheduled ticks in a chunk with its own time. Random ticks (crops, leaves, ice) run as often as the
 * chunk's clock says. A scheduled tick (flowing water, redstone) that comes due in a stopped chunk waits, a tick
 * at a time, until the chunk moves again; in a slowed chunk its delay was already stretched ({@code LevelTicksMixin}).
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "tickChunk", at = @At("HEAD"), cancellable = true)
    private void chronogenesis$randomTicks(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        ServerLevel self = (ServerLevel) (Object) this;
        int ticks = TimeZones.ticksAt(self, chunk.getPos().toLong());
        if (ticks == 1) {
            return;
        }
        if (ticks == 0) {
            ci.cancel();
            return;
        }
        if (TimeZones.beginExtra()) {
            try {
                for (int i = 1; i < ticks; i++) {
                    self.tickChunk(chunk, randomTickSpeed);
                }
            } finally {
                TimeZones.endExtra();
            }
        }
    }

    @Inject(method = "tickBlock", at = @At("HEAD"), cancellable = true)
    private void chronogenesis$blockWaits(BlockPos pos, Block block, CallbackInfo ci) {
        ServerLevel self = (ServerLevel) (Object) this;
        if (TimeZones.stoppedAt(self, pos)) {
            self.scheduleTick(pos, block, 1);
            ci.cancel();
        }
    }

    @Inject(method = "tickFluid", at = @At("HEAD"), cancellable = true)
    private void chronogenesis$fluidWaits(BlockPos pos, Fluid fluid, CallbackInfo ci) {
        ServerLevel self = (ServerLevel) (Object) this;
        if (TimeZones.stoppedAt(self, pos)) {
            self.scheduleTick(pos, fluid, 1);
            ci.cancel();
        }
    }
}
