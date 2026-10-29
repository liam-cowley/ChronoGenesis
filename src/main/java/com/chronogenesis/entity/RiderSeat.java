package com.chronogenesis.entity;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The invisible thing a player sits on in a Rider's Seat. In a seat that is part of a Titanosaur's cargo it rides
 * where the seat is on the beast, and the sitter's walking keys steer the beast. It goes when they get up, or when the
 * seat is gone.
 */
public class RiderSeat extends Entity {
    /** The beast whose cargo the seat is part of, and where the seat sits in the cargo. */
    @Nullable
    private Titanosaur carrier;
    private BlockPos seatOffset = BlockPos.ZERO;

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
        if (!seated() || (getPassengers().isEmpty() && tickCount > 5)) {
            ejectPassengers();
            discard();
            return;
        }
        if (carrier != null && getFirstPassenger() instanceof Player rider && (rider.zza != 0 || rider.xxa != 0)) {
            carrier.steer(rider.zza, rider.xxa);
        }
    }

    /** Whether its seat block is still there: in its beast's hold, or (outside any cargo) under it. */
    private boolean seated() {
        if (carrier == null) {
            return level().getBlockState(blockPosition()).is(ModBlocks.RIDERS_SEAT.get());
        }
        BlockPos harness = carrier.cargo().harness();
        return carrier.isAlive() && harness != null && level().getBlockState(harness.offset(seatOffset)).is(ModBlocks.RIDERS_SEAT.get());
    }

    /** Its block is part of a beast's cargo: it rides with the beast. */
    void carriedBy(Titanosaur beast, BlockPos offset) {
        carrier = beast;
        seatOffset = offset;
        beast.addSeat(this);
        followCarrier(beast);
    }

    @Nullable
    public Titanosaur carrier() {
        return carrier;
    }

    public BlockPos seatOffset() {
        return seatOffset;
    }

    /** Keeps to its block on the beast. */
    void followCarrier(Titanosaur beast) {
        Vec3 at = beast.cargo().toWorld(new Vec3(seatOffset.getX() + 0.5, seatOffset.getY() + 0.3, seatOffset.getZ() + 0.5),
                beast.position(), beast.getYRot());
        float turned = beast.getYRot() - getYRot();
        setPos(at.x, at.y, at.z);
        setYRot(beast.getYRot());
        if (getFirstPassenger() instanceof Player rider && Math.abs(turned) > 1.0E-3 && tickCount > 1) {
            rider.setYRot(rider.getYRot() + turned);
        }
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
