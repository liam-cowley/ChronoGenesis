package com.chronogenesis.entity;

import java.util.List;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.biocomputer.Research;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A Precursor: one of the people who built the first cities, long before the villagers (the Age of the Precursors,
 * core-systems.md section 5). Tall, robed, grey-skinned, with amethyst eyes and a gold circlet; peaceful.
 * <ul>
 *   <li>They keep to their city: each remembers where it first stood and wanders within reach of it, busily,
 *       stopping to look at one another (glyphs drift between them as they talk) and at strangers.</li>
 *   <li>Spoken to, they answer in their own tongue. Once a Chronocortex holds the memory of their age (an offshoot
 *       planted in it, linked back in the present), whoever linked it understands what they say.</li>
 * </ul>
 */
public class Precursor extends PathfinderMob {
    /** Lines they say: {@code precursor.chronogenesis.say.<n>} is the translation. */
    public static final int LINES = 12;
    /** Lines only a kingdom's own folk say: {@code precursor.chronogenesis.say.<kingdom>.<n>}. */
    public static final int KINGDOM_LINES = 10;
    public static final List<String> KINGDOMS = List.of("sun", "tide", "deep", "verdant", "hearth");
    /** Synced: each kingdom dresses its own way (client/PrecursorRenderer). */
    private static final EntityDataAccessor<String> KINGDOM = SynchedEntityData.defineId(Precursor.class, EntityDataSerializers.STRING);
    /** Understanding them: granted (as a completed topic) to whoever links the memory of their age. */
    public static final ResourceLocation TONGUE = ChronoGenesis.id("memory/precursor_era");
    private static final int HOME_RANGE = 20;

    private static final String[] SYLLABLES = {"ka", "rhu", "ten", "ish", "vo", "ael", "dun", "sa", "mir", "oth",
            "qe", "lun", "zha", "ir", "tol", "ume"};

    public Precursor(EntityType<? extends Precursor> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(KINGDOM, "");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 0.75));
        goalSelector.addGoal(2, new MoveTowardsRestrictionGoal(this, 0.6));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0f, 0.04f));
        // Busy: they set off somewhere every second or two, not every few seconds like other creatures.
        WaterAvoidingRandomStrollGoal stroll = new WaterAvoidingRandomStrollGoal(this, 0.45);
        stroll.setInterval(30);
        goalSelector.addGoal(4, stroll);
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Precursor.class, 8.0f, 0.1f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server) {
            if (!hasRestriction()) {
                restrictTo(blockPosition(), HOME_RANGE);
            }
            // Talking: glyphs drift from one to another when they stand together.
            if (tickCount % 30 == 0 && random.nextInt(3) == 0
                    && !server.getEntitiesOfClass(Precursor.class, getBoundingBox().inflate(3.0), other -> other != this).isEmpty()) {
                server.sendParticles(ParticleTypes.ENCHANT, getX(), getEyeY() + 0.4, getZ(), 6, 0.3, 0.2, 0.3, 0.4);
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide()) {
            // Mostly their own kingdom's worries; sometimes what every Precursor says.
            String kingdom = kingdom();
            int kingdomIndex = KINGDOMS.indexOf(kingdom);
            boolean own = kingdomIndex >= 0 && random.nextFloat() < 0.6f;
            int line = own ? random.nextInt(KINGDOM_LINES) : random.nextInt(LINES);
            String key = own ? "precursor.chronogenesis.say." + kingdom + "." + line : "precursor.chronogenesis.say." + line;
            getLookControl().setLookAt(player);
            playSound(SoundEvents.VILLAGER_AMBIENT, 1.0f, 0.65f);
            player.displayClientMessage(Component.translatable("precursor.chronogenesis.speaker").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(tongue(own ? 100 + 10 * kingdomIndex + line : line))
                            .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC)), false);
            if (Research.has(player, TONGUE)) {
                player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), false);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    /** A line in their tongue: the same sounds every time for the same meaning. */
    public static String tongue(int line) {
        RandomSource sounds = RandomSource.create(7919L * (line + 1));
        StringBuilder out = new StringBuilder();
        int words = 3 + sounds.nextInt(4);
        for (int w = 0; w < words; w++) {
            if (w > 0) {
                out.append(sounds.nextInt(5) == 0 ? "'" : " ");
            }
            int syllables = 1 + sounds.nextInt(3);
            for (int s = 0; s < syllables; s++) {
                out.append(SYLLABLES[sounds.nextInt(SYLLABLES.length)]);
            }
        }
        out.setCharAt(0, Character.toUpperCase(out.charAt(0)));
        return out.append(sounds.nextBoolean() ? "." : "?").toString();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    /** Lower than a villager's: an older voice. */
    @Override
    public float getVoicePitch() {
        return 0.65f + (random.nextFloat() - 0.5f) * 0.1f;
    }

    /** Which kingdom's city it lives in (set by its city's template): they talk about their own troubles. */
    public String kingdom() {
        return entityData.get(KINGDOM);
    }

    public void setKingdom(String kingdom) {
        entityData.set(KINGDOM, kingdom);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Kingdom", kingdom());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setKingdom(tag.getString("Kingdom"));
    }

    /** Where it calls home (its first standing place), for tests. */
    public BlockPos home() {
        return getRestrictCenter();
    }
}
