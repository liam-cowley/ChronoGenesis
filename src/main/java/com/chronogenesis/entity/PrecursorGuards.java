package com.chronogenesis.entity;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.registry.ModAttachments;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * What the Precursors' kingdoms kept against the thing that was coming for them: the Deep Kingdom's Wardens, the
 * Tide Kingdom's Guardians, the Hearth's iron golems (core-systems.md section 5). A guard (tagged {@link #TAG}, or a
 * {@link PrecursorGolem}) is friendly to everyone who keeps the peace: it will not hunt, sense, roar at or darken a
 * player. Hurt it, or one of the Precursors, and the guards hold a grudge against that player ({@link #GRUDGE} ticks):
 * every guard near enough turns on them.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class PrecursorGuards {
    /** The scoreboard tag of a guard (set by the city templates and the restored cities). */
    public static final String TAG = "chronogenesis.guard";
    /** How long a grudge lasts. */
    public static final int GRUDGE = 20 * 120;
    /** How far the alarm carries. */
    private static final double ALARM = 48.0;

    private PrecursorGuards() {}

    public static void makeGuard(Entity entity) {
        entity.addTag(TAG);
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
    }

    public static boolean isGuard(Entity entity) {
        return entity instanceof PrecursorGolem || entity.getTags().contains(TAG);
    }

    /** Whether the guards hold a grudge against this player (they hurt a guard or a Precursor not long ago). */
    public static boolean offender(Player player) {
        return player.getData(ModAttachments.PRECURSOR_GRUDGE) > player.level().getGameTime();
    }

    /** What a guard may go for: players the guards hold a grudge against, and monsters; never the folk or each other. */
    public static boolean mayTarget(Entity guard, Entity target) {
        if (target instanceof Player player) {
            return offender(player);
        }
        if (target instanceof Precursor || isGuard(target)) {
            return false;
        }
        return target instanceof Enemy;
    }

    /** A guard with nobody to go for: no darkness from it. */
    public static boolean calm(Entity guard) {
        return isGuard(guard) && !(guard instanceof Mob mob && mob.getTarget() instanceof Player);
    }

    /** That player hurt a guard or a Precursor: the guards near enough remember. */
    public static void grudge(Player player, LivingEntity victim) {
        if (!offender(player)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.chronogenesis.precursor_guard.grudge")
                    .withStyle(net.minecraft.ChatFormatting.RED), true);
        }
        player.setData(ModAttachments.PRECURSOR_GRUDGE, player.level().getGameTime() + GRUDGE);
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        for (Mob guard : level.getEntitiesOfClass(Mob.class, victim.getBoundingBox().inflate(ALARM), PrecursorGuards::isGuard)) {
            if (guard instanceof Warden warden) {
                warden.increaseAngerAt(player, 150, false);
            } else {
                if (guard instanceof NeutralMob neutral) {
                    neutral.setPersistentAngerTarget(player.getUUID());
                    neutral.startPersistentAngerTimer();
                }
                guard.setTarget(player);
            }
        }
    }

    @SubscribeEvent
    static void onHurt(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (!victim.level().isClientSide() && event.getSource().getEntity() instanceof Player player && !player.isCreative()
                && (victim instanceof Precursor || isGuard(victim))) {
            grudge(player, victim);
        }
    }

    @SubscribeEvent
    static void onTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target != null && isGuard(event.getEntity()) && !mayTarget(event.getEntity(), target)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Warden warden) || warden.level().isClientSide() || warden.tickCount % 40 != 0
                || !isGuard(warden)) {
            return;
        }
        // A guard does not go back into the earth, and lets go of anyone it holds no grudge against.
        warden.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 1200L);
        for (Player player : warden.level().players()) {
            if (!offender(player) && player.distanceToSqr(warden) < 64 * 64) {
                warden.clearAnger(player);
                if (warden.getTarget() == player) {
                    warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                }
                if (warden.getBrain().getMemory(MemoryModuleType.ROAR_TARGET).filter(t -> t == player).isPresent()) {
                    warden.getBrain().eraseMemory(MemoryModuleType.ROAR_TARGET);
                }
            }
        }
    }
}
