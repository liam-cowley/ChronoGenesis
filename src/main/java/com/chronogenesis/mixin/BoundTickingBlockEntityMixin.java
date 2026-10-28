package com.chronogenesis.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.chronogenesis.time.TimeZones;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;

/**
 * Block entities in a chunk with its own time (Slow Time, Time Freeze) tick as often as the chunk's clock says:
 * not at all while it is stopped, now and then while slowed, several times a tick while it catches up.
 */
@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public abstract class BoundTickingBlockEntityMixin {
    @Shadow
    @Final
    private BlockEntity blockEntity;

    @Shadow
    @Final
    @SuppressWarnings("rawtypes")
    private BlockEntityTicker ticker;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void chronogenesis$localTime(CallbackInfo ci) {
        Level level = blockEntity.getLevel();
        if (level != null && TimeZones.ticksAt(level, blockEntity.getBlockPos()) == 0) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    @SuppressWarnings("unchecked")
    private void chronogenesis$catchUp(CallbackInfo ci) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }
        int ticks = TimeZones.ticksAt(level, blockEntity.getBlockPos());
        for (int i = 1; i < ticks && !blockEntity.isRemoved(); i++) {
            ticker.tick(level, blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity);
        }
    }
}
