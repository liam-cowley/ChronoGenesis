package com.chronogenesis.entity;

import java.util.UUID;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A Raptor (the Jurassic Age): man-high, quick, hunting in packs of two or three. It leaps at its prey. Guards the
 * temples' halls, and hunts the fernlands by day as well as by night. Hatched from an egg ({@link Dinos}), it
 * never turns on whoever hatched it.
 */
public class Raptor extends Monster implements Dinos.Hatchling {
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(Raptor.class, EntityDataSerializers.INT);
    @Nullable
    private UUID hatcher;

    public Raptor(EntityType<? extends Raptor> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(AGE, 0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 18.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 28.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.45f));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.35, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        // A hatchling hunts nothing yet; grown, it never hunts whoever hatched it.
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                player -> !young() && !player.getUUID().equals(hatcher)));
    }

    @Override
    public void tick() {
        super.tick();
        Dinos.tick(this, this);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult fed = Dinos.feed(this, this, player, hand);
        return fed != InteractionResult.PASS ? fed : super.mobInteract(player, hand);
    }

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

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key.equals(AGE)) {
            refreshDimensions();
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return hatcher == null && super.removeWhenFarAway(distance);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Age", age());
        if (hatcher != null) {
            tag.putUUID("Hatcher", hatcher);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setAge(tag.getInt("Age"));
        hatcher = tag.hasUUID("Hatcher") ? tag.getUUID("Hatcher") : null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.55f + random.nextFloat() * 0.15f;
    }
}
