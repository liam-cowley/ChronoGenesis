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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The invisible thing a player sits on in a Rider's Seat. While a player sits on a seat that is part of a
 * Titanosaur's cargo, their walking keys steer the beast; while the cargo is carried (lifted out of the world), the
 * seat rides with it, and is set down with it. It goes when they get up, or when the seat is gone.
 */
public class RiderSeat extends Entity {
    private static final double SEARCH = 48.0;
    @Nullable
    private Titanosaur beast;
    /** The beast carrying this seat's block, while its cargo is lifted; and where the block sits in the cargo. */
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
        boolean held = carrier != null && carrier.isAlive() && carrier.cargo().carrying();
        if (!held && carrier != null) {
            carrier = null;
        }
        if ((!held && !level().getBlockState(blockPosition()).is(ModBlocks.RIDERS_SEAT.get())) || (getPassengers().isEmpty() && tickCount > 5)) {
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

    /** Its block was lifted into a carried cargo: it rides with the beast now. */
    void carriedBy(Titanosaur beast, BlockPos offset) {
        carrier = beast;
        seatOffset = offset;
        beast.addSeat(this);
    }

    @Nullable
    public Titanosaur carrier() {
        return carrier;
    }

    public BlockPos seatOffset() {
        return seatOffset;
    }

    /** Keeps to its block in the carried cargo. */
    void followCarrier(Titanosaur beast) {
        Vec3 at = beast.cargo().toWorld(new Vec3(seatOffset.getX() + 0.5, seatOffset.getY() + 0.3, seatOffset.getZ() + 0.5),
                beast.position(), beast.getYRot());
        float turned = beast.getYRot() - getYRot();
        setPos(at.x, at.y, at.z);
        setYRot(beast.getYRot());
        if (getFirstPassenger() instanceof Player rider && Math.abs(turned) > 1.0E-3) {
            rider.setYRot(rider.getYRot() + turned);
        }
    }

    /** The cargo was set down: back onto its block. */
    void setDown(BlockPos pos) {
        carrier = null;
        setPos(pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5);
    }

    /** The beast whose cargo this seat is part of. */
    @Nullable
    private Titanosaur beast() {
        if (carrier != null) {
            return carrier;
        }
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
