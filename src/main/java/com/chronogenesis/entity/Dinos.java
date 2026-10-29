package com.chronogenesis.entity;

import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.essence.EssenceItems;
import com.chronogenesis.item.EternalHourglassItem;
import com.chronogenesis.registry.ModEntities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The dinosaurs that can be brought back (a fossil fed to the Chronocortex, its egg hatched with Time): what each eats,
 * and how their young grow. A hatchling is a baby for {@link #GROW_UP} ticks (a day); the right food brings it on
 * ({@link #FOOD_BONUS}: leaves for the plant-eaters, meat for the meat-eaters), and Time faster still
 * ({@link #TIME_BONUS} a dram). It remembers who hatched it, and never turns on them.
 */
public final class Dinos {
    /** How long a hatchling takes to grow up on its own. */
    public static final int GROW_UP = 24000;
    public static final int FOOD_BONUS = 1800;
    public static final int TIME_BONUS = 600;
    /** Drams of Time taken from a container in one feeding. */
    public static final int TIME_FEED = 4;

    public enum Diet { PLANTS, MEAT }

    /** A creature that hatches and grows: its age (negative while young) and who hatched it. */
    public interface Hatchling {
        int age();

        void setAge(int age);

        @Nullable
        UUID hatcher();

        void setHatcher(@Nullable UUID hatcher);

        default boolean young() {
            return age() < 0;
        }
    }

    private Dinos() {}

    public static Map<EntityType<?>, Diet> diets() {
        return Map.of(ModEntities.RAPTOR.get(), Diet.MEAT, ModEntities.TITANOSAUR.get(), Diet.PLANTS);
    }

    @Nullable
    public static Diet diet(EntityType<?> type) {
        return diets().get(type);
    }

    public static boolean isDino(EntityType<?> type) {
        return diet(type) != null;
    }

    public static boolean food(Diet diet, ItemStack stack) {
        return diet == Diet.PLANTS ? stack.is(ItemTags.LEAVES) : stack.is(ItemTags.MEAT);
    }

    /** A young one is fed (its food, or Time from a container). Returns PASS if it is not something it takes. */
    public static InteractionResult feed(Mob dino, Hatchling young, Player player, InteractionHand hand) {
        if (!young.young()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        Diet diet = diet(dino.getType());
        int bonus = 0;
        if (diet != null && food(diet, stack)) {
            bonus = FOOD_BONUS;
            if (!dino.level().isClientSide()) {
                stack.consume(1, player);
            }
        } else if (EssenceItems.get(stack) != null && EssenceItems.get(stack).aspect() == Aspect.TIME) {
            if (!dino.level().isClientSide()) {
                EssenceContents taken = EternalHourglassItem.carried(player) ? new EssenceContents(Aspect.TIME, TIME_FEED)
                        : EssenceItems.drainHeld(player, hand, Aspect.TIME, TIME_FEED);
                bonus = taken == null ? 0 : taken.amount() * TIME_BONUS;
            } else {
                bonus = 1;
            }
        }
        if (bonus <= 0) {
            return InteractionResult.PASS;
        }
        if (dino.level() instanceof ServerLevel level) {
            grow(dino, young, bonus);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, dino.getX(), dino.getY(0.7), dino.getZ(), 8, 0.4, 0.4, 0.4, 0.0);
            level.playSound(null, dino.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.8f, 1.4f);
        }
        return InteractionResult.sidedSuccess(dino.level().isClientSide());
    }

    /** Ages a young one by {@code ticks}; grown up, it is its full size. */
    public static void grow(Mob dino, Hatchling young, int ticks) {
        boolean was = young.young();
        young.setAge(Math.min(0, young.age() + ticks));
        if (was && !young.young()) {
            dino.refreshDimensions();
        }
    }

    /** Every tick, a young one grows a little. */
    public static void tick(Mob dino, Hatchling young) {
        if (!dino.level().isClientSide() && young.young()) {
            grow(dino, young, 1);
        }
    }

    /** A young one, freshly hatched. */
    public static void hatchling(Mob dino, Hatchling young, @Nullable Player hatcher) {
        young.setAge(-GROW_UP);
        young.setHatcher(hatcher == null ? null : hatcher.getUUID());
        dino.setPersistenceRequired();
        dino.refreshDimensions();
    }
}
