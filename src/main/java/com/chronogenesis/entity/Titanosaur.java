package com.chronogenesis.entity;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.registry.ModItems;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.phys.Vec3;

/**
 * A Titanosaur (the Jurassic Age): a sauropod the size of a house, slow and peaceful until hurt. Buckle a Dino
 * Harness onto it and its back becomes ground to build on ({@link DinoCargo}); from a Rider's Seat in the cargo, a
 * player steers it (forward and back to walk, left and right to turn).
 */
public class Titanosaur extends PathfinderMob {
    /** The harness sits this many blocks above its feet: above its back, clear of its body when it steps up. */
    public static final int BACK = 6;
    private static final int STEER_HOLD = 3;

    private final DinoCargo cargo = new DinoCargo(this);
    private float steerForward;
    private float steerTurn;
    private int steerTicks;
    @Nullable
    private Vec3 lastGood;
    private float lastGoodYaw;

    public Titanosaur(EntityType<? extends Titanosaur> type, Level level) {
        super(type, level);
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

    public DinoCargo cargo() {
        return cargo;
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
            if (!level().isClientSide()) {
                if (cargo.buckle(player, stack)) {
                    setPersistenceRequired();
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide());
        }
        return super.mobInteract(player, hand);
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
        if (level().isClientSide()) {
            return;
        }
        if (steerTicks > 0) {
            steerTicks--;
        }
        if (!cargo.harnessed()) {
            return;
        }
        if (lastGood == null) {
            lastGood = position();
            lastGoodYaw = getYRot();
        }
        if (cargo.follow()) {
            lastGood = position();
            lastGoodYaw = getYRot();
        } else {
            // Its cargo would hit something: it cannot go there.
            setPos(lastGood);
            setYRot(lastGoodYaw);
            yBodyRot = lastGoodYaw;
            setDeltaMovement(Vec3.ZERO);
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
        return !cargo.harnessed() && super.removeWhenFarAway(distance);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        CompoundTag cargoTag = new CompoundTag();
        cargo.save(cargoTag);
        tag.put("Cargo", cargoTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        cargo.load(tag.getCompound("Cargo"));
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
        return 0.4f;
    }

    @Override
    protected float getSoundVolume() {
        return 2.0f;
    }
}
