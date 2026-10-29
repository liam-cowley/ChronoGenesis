package com.chronogenesis.entity;

import java.util.Iterator;
import java.util.List;

import org.joml.Vector3f;

import com.chronogenesis.player.TemporalExposure;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * One of the Unbegotten's area attacks, marked on the arena before it lands (a raid boss's telegraph): a ring (or a
 * disc, when {@code inner} is 0) of violet motes on the floor that thickens as the fuse burns down, then a blast that
 * hurts everyone standing in it (and fills them with Temporal Exposure). Step out of the marks.
 */
public final class UnbegottenBlast {
    private static final DustParticleOptions MARK = new DustParticleOptions(new Vector3f(0.55f, 0.2f, 0.75f), 1.6f);
    private static final DustParticleOptions HOT = new DustParticleOptions(new Vector3f(0.65f, 1.0f, 0.5f), 2.0f);

    final Vec3 centre;
    final double inner;
    final double outer;
    final int fuseTotal;
    int fuse;
    final float damage;
    final int exposure;

    public UnbegottenBlast(Vec3 centre, double inner, double outer, int fuse, float damage, int exposure) {
        this.centre = centre;
        this.inner = inner;
        this.outer = outer;
        this.fuseTotal = fuse;
        this.fuse = fuse;
        this.damage = damage;
        this.exposure = exposure;
    }

    /** Burns every pending blast down by a tick: marks drawn, the ripe ones go off. */
    public static void tick(List<UnbegottenBlast> blasts, ServerLevel level, LivingEntity source) {
        for (Iterator<UnbegottenBlast> it = blasts.iterator(); it.hasNext(); ) {
            UnbegottenBlast blast = it.next();
            blast.fuse--;
            if (blast.fuse > 0) {
                if (blast.fuse % 4 == 0) {
                    blast.mark(level);
                }
            } else {
                blast.burst(level, source);
                it.remove();
            }
        }
    }

    private void mark(ServerLevel level) {
        boolean late = fuse < fuseTotal / 3;
        DustParticleOptions dust = late ? HOT : MARK;
        for (double r : inner > 0 ? new double[] {inner, outer} : new double[] {outer}) {
            int points = Math.max(8, (int) (r * 4));
            for (int i = 0; i < points; i++) {
                double a = Math.PI * 2 * i / points;
                level.sendParticles(dust, centre.x + Math.cos(a) * r, centre.y + 0.15, centre.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
        }
        // The ground between the rings simmers, thicker as the fuse burns down.
        int inside = (int) ((outer * outer - inner * inner) * (late ? 0.5 : 0.2));
        for (int i = 0; i < inside; i++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double r = Math.sqrt(Mth.lerp(level.random.nextDouble(), inner * inner, outer * outer));
            level.sendParticles(dust, centre.x + Math.cos(a) * r, centre.y + 0.15, centre.z + Math.sin(a) * r, 1, 0, 0.05, 0, 0);
        }
    }

    private void burst(ServerLevel level, LivingEntity source) {
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0f, 0.5f);
        int puffs = (int) Math.min(60, (outer * outer - inner * inner) * 0.4);
        for (int i = 0; i < puffs; i++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double r = Math.sqrt(Mth.lerp(level.random.nextDouble(), inner * inner, outer * outer));
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, centre.x + Math.cos(a) * r, centre.y + 0.3, centre.z + Math.sin(a) * r,
                    4, 0.2, 0.6, 0.2, 0.15);
        }
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(centre, centre).inflate(outer + 1, 4, outer + 1),
                player -> player.isAlive() && !player.isSpectator() && !player.isCreative())) {
            double d = Math.sqrt(Mth.square(player.getX() - centre.x) + Mth.square(player.getZ() - centre.z));
            if (d >= inner && d <= outer && Math.abs(player.getY() - centre.y) < 4) {
                player.hurt(source.damageSources().indirectMagic(source, source), damage);
                if (exposure > 0) {
                    TemporalExposure.expose(player, exposure);
                }
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true));
            }
        }
    }
}
