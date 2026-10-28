package com.chronogenesis.entity;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.registry.ModBlocks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * The invisible thing a player sits on in a Rider's Seat. While a player sits on a seat that is part of a
 * Titanosaur's cargo, their walking keys steer the beast. It goes when they get up, or when the seat is gone.
 */
public class RiderSeat extends Entity {
    private static final double SEARCH = 48.0;
    @Nullable
    private Titanosaur beast;

    public RiderSeat(EntityType<? extends RiderSeat> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (!level().getBlockState(blockPosition()).is(ModBlocks.RIDERS_SEAT.get()) || (getPassengers().isEmpty() && tickCount > 5)) {
            ejectPassengers();
            discard();
            return;
        }
        if (getFirstPassenger() instanceof Player rider) {
            Titanosaur steered = beast();
            if (steered != null && (rider.zza != 0 || rider.xxa != 0)) {
                steered.steer(rider.zza, rider.xxa);
            }
        }
    }

    /** The beast whose cargo this seat is part of. */
    @Nullable
    private Titanosaur beast() {
        if (beast != null && beast.isAlive() && beast.cargo().contains(blockPosition())) {
            return beast;
        }
        beast = null;
        for (Titanosaur candidate : level().getEntitiesOfClass(Titanosaur.class, new AABB(blockPosition()).inflate(SEARCH),
                candidate -> candidate.cargo().contains(blockPosition()))) {
            beast = candidate;
        }
        return beast;
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide()) {
            discard();
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
