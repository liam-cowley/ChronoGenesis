package com.chronogenesis.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.registry.ModItems;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

/**
 * A Titanosaur (the Jurassic Age): a sauropod the size of a house, slow and peaceful until hurt. Buckle a Dino
 * Harness onto it and its back becomes ground to build on; walking, it carries what is built there as one moving
 * structure ({@link DinoCargo}, solid through its {@link CargoPart}s). From a Rider's Seat in the cargo, a player
 * steers it (forward and back to walk, left and right to turn). Hatched from an egg ({@link Dinos}), it is a calf a
 * while first.
 */
public class Titanosaur extends PathfinderMob implements Dinos.Hatchling {
    /** The harness sits this many blocks above its feet: above its back, clear of its body when it steps up. */
    public static final int BACK = 6;
    private static final int STEER_HOLD = 3;
    private static final EntityDataAccessor<CompoundTag> CARGO = SynchedEntityData.defineId(Titanosaur.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(Titanosaur.class, EntityDataSerializers.INT);

    private final DinoCargo cargo = new DinoCargo(this);
    private final CargoPart[] parts = new CargoPart[CargoPart.COUNT];
    private final List<RiderSeat> seats = new ArrayList<>();
    private float steerForward;
    private float steerTurn;
    private int steerTicks;
    @Nullable
    private Vec3 lastGood;
    private float lastGoodYaw;
    @Nullable
    private Vec3 lastPos;
    private float lastYaw;
    @Nullable
    private UUID hatcher;

    public Titanosaur(EntityType<? extends Titanosaur> type, Level level) {
        super(type, level);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = new CargoPart(this);
        }
        setId(ENTITY_COUNTER.getAndAdd(parts.length + 1) + 1);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 220.0)
                .add(Attributes.MOVEMENT_SPEED, 0.16)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CARGO, new CompoundTag());
        builder.define(AGE, 0);
    }

    public DinoCargo cargo() {
        return cargo;
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        for (int i = 0; i < parts.length; i++) {
            parts[i].setId(id + i + 1);
        }
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return parts;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6) {
            @Override
            public boolean canUse() {
                // Harnessed, it goes only where it is steered.
                return !cargo.harnessed() && super.canUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ModItems.DINO_HARNESS.get()) && !cargo.harnessed()) {
            if (young()) {
                return InteractionResult.PASS;
            }
            if (!level().isClientSide()) {
                if (cargo.buckle(player, stack)) {
                    setPersistenceRequired();
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide());
        }
        InteractionResult fed = Dinos.feed(this, this, player, hand);
        return fed != InteractionResult.PASS ? fed : super.mobInteract(player, hand);
    }

    /** From a Rider's Seat: walk ({@code forward}, -1 to 1) and turn ({@code turn}, positive = left). */
    public void steer(float forward, float turn) {
        steerForward = Mth.clamp(forward, -0.5f, 1.0f);
        steerTurn = Mth.clamp(turn, -1.0f, 1.0f);
        steerTicks = STEER_HOLD;
        getNavigation().stop();
    }

    public boolean steered() {
        return steerTicks > 0;
    }

    @Override
    public void travel(Vec3 input) {
        if (steerTicks > 0 && isAlive()) {
            float yaw = getYRot() - steerTurn * 3.0f;
            setYRot(yaw);
            yBodyRot = yaw;
            yHeadRot = yaw;
            setSpeed((float) getAttributeValue(Attributes.MOVEMENT_SPEED));
            super.travel(new Vec3(0.0, input.y, steerForward));
        } else {
            super.travel(input);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            if (steerTicks > 0) {
                steerTicks--;
            }
            Dinos.tick(this, this);
            if (cargo.harnessed()) {
                cargo.tick(steered());
            }
            if (cargo.carrying()) {
                // A carried cargo cannot pass through the land: the beast stops where it would.
                if (lastGood != null && cargo.blocked(level(), position(), getYRot())) {
                    setPos(lastGood);
                    setYRot(lastGoodYaw);
                    yBodyRot = lastGoodYaw;
                    setDeltaMovement(Vec3.ZERO);
                } else {
                    lastGood = position();
                    lastGoodYaw = getYRot();
                }
                for (RiderSeat seat : seats) {
                    seat.followCarrier(this);
                }
            }
            seats.removeIf(seat -> seat.isRemoved() || seat.carrier() != this);
        }
        placeParts();
        carryRiders();
        lastPos = position();
        lastYaw = getYRot();
    }

    /** The carried cargo's boxes follow the beast. */
    private void placeParts() {
        List<AABB> boxes = cargo.carrying() ? CargoPart.boxes(cargo, position(), getYRot()) : List.of();
        for (int i = 0; i < parts.length; i++) {
            if (i < boxes.size()) {
                parts[i].place(boxes.get(i));
            } else {
                parts[i].clear();
            }
        }
    }

    /**
     * Whatever stands on the carried cargo is moved and turned with it (on the server, creatures; on a client, its own
     * player, who moves themselves).
     */
    private void carryRiders() {
        if (!cargo.carrying() || lastPos == null) {
            return;
        }
        float turned = getYRot() - lastYaw;
        Vec3 moved = position().subtract(lastPos);
        if (moved.lengthSqr() < 1.0E-8 && Math.abs(turned) < 1.0E-4) {
            return;
        }
        List<AABB> boxes = new ArrayList<>();
        AABB all = null;
        for (CargoPart part : parts) {
            if (part.active()) {
                boxes.add(part.getBoundingBox());
                all = all == null ? part.getBoundingBox() : all.minmax(part.getBoundingBox());
            }
        }
        if (all == null) {
            return;
        }
        for (Entity entity : level().getEntities(this, all.inflate(0.5, 1.5, 0.5), e -> !(e instanceof PartEntity<?>) && !e.isPassenger())) {
            boolean local = level().isClientSide() ? entity instanceof Player player && player.isLocalPlayer() : !(entity instanceof Player);
            if (!local || !standingOn(entity, boxes)) {
                continue;
            }
            Vec3 relative = entity.position().subtract(lastPos);
            Vec3 at = position().add(DinoCargo.rotate(relative, turned));
            entity.setPos(at.x, at.y, at.z);
            entity.setYRot(entity.getYRot() + turned);
            entity.setYHeadRot(entity.getYHeadRot() + turned);
        }
    }

    private static boolean standingOn(Entity entity, List<AABB> boxes) {
        AABB feet = entity.getBoundingBox();
        for (AABB box : boxes) {
            if (feet.maxX > box.minX && feet.minX < box.maxX && feet.maxZ > box.minZ && feet.minZ < box.maxZ
                    && feet.minY >= box.maxY - 0.2 && feet.minY <= box.maxY + 0.6) {
                return true;
            }
        }
        return false;
    }

    /** Drawn while any of it (its carried cargo too) is in view. */
    @Override
    public AABB getBoundingBoxForCulling() {
        AABB box = super.getBoundingBoxForCulling();
        for (CargoPart part : parts) {
            if (part.active()) {
                box = box.minmax(part.getBoundingBox());
            }
        }
        return box;
    }

    /** The cargo was lifted or set down: tell the clients what to draw. */
    void cargoChanged() {
        entityData.set(CARGO, cargo.writeCarried(false));
        if (!cargo.carrying()) {
            lastGood = null;
        }
    }

    void addSeat(RiderSeat seat) {
        if (!seats.contains(seat)) {
            seats.add(seat);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key.equals(CARGO) && level().isClientSide()) {
            cargo.readCarried(entityData.get(CARGO), level().holderLookup(Registries.BLOCK));
        }
        if (key.equals(AGE)) {
            refreshDimensions();
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide()) {
            cargo.collapse();
        }
        super.die(source);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !cargo.harnessed() && hatcher == null && super.removeWhenFarAway(distance);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    // ---------------------------------------------------------------- growing up

    @Override
    public int age() {
        return entityData.get(AGE);
    }

    @Override
    public void setAge(int age) {
        entityData.set(AGE, age);
    }

    @Nullable
    @Override
    public UUID hatcher() {
        return hatcher;
    }

    @Override
    public void setHatcher(@Nullable UUID hatcher) {
        this.hatcher = hatcher;
    }

    @Override
    public boolean isBaby() {
        return young();
    }

    /** A calf is a quarter of its mother's size (still taller than a man). */
    @Override
    public float getAgeScale() {
        return young() ? 0.3f : 1.0f;
    }

    // ---------------------------------------------------------------- saving

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        CompoundTag cargoTag = new CompoundTag();
        cargo.save(cargoTag);
        tag.put("Cargo", cargoTag);
        tag.putInt("Age", age());
        if (hatcher != null) {
            tag.putUUID("Hatcher", hatcher);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        cargo.load(tag.getCompound("Cargo"));
        setAge(tag.getInt("Age"));
        hatcher = tag.hasUUID("Hatcher") ? tag.getUUID("Hatcher") : null;
        if (level() instanceof ServerLevel) {
            entityData.set(CARGO, cargo.writeCarried(false));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return young() ? 1.1f : 0.4f;
    }

    @Override
    protected float getSoundVolume() {
        return young() ? 0.8f : 2.0f;
    }
}
