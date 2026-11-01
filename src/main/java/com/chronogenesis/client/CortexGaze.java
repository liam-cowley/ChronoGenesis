package com.chronogenesis.client;

import org.joml.Vector3f;

import com.chronogenesis.registry.ModBlocks;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Where one Chronocortex is looking, kept from frame to frame (client only). While the local player is in its line
 * of sight (nothing that blocks light between them: its own lobes, the jar and glass do not count) its eyes follow
 * them without pause, it grows intent, and, if they come close, its tentacles reach. If they stare back, its pupils
 * narrow to slits and its lids squint. Out of sight, its eyes wander. The floating brain of stages 0-1 turns in its
 * jar to face whoever it watches; on the grown body of stages 2-3 the great eye crawls round under the skin.
 */
final class CortexGaze {
    /** How far it can see, in blocks. */
    private static final double SIGHT = 48;
    /** Ticks between two line-of-sight checks. */
    private static final int CHECK = 4;
    /** Where the great eye is, roughly, above the block's middle (blocks), by stage. */
    private static final float[] EYE_HEIGHT = {0.26f, 0.4f, 0.95f, 0.15f};

    private final Vector3f look = new Vector3f(0, 0, 48);
    private final RandomSource random = RandomSource.create();
    private float last = Float.NaN;
    private float checked = Float.NEGATIVE_INFINITY;
    private boolean sees;
    private float alert;
    private float stare;
    private float reach;
    private float bodyYaw;
    private float socketYaw;
    private float socketPitch = 0.15f;
    private float nextWander;
    private float wanderYaw;
    private float wanderPitch;

    CortexBody.Look update(Level level, BlockPos pos, int stage, float time, float partialTick) {
        float dt = Float.isNaN(last) ? 0 : Mth.clamp(time - last, 0, 20);
        last = time;
        Vec3 centre = Vec3.atCenterOf(pos);
        Player player = Minecraft.getInstance().player;
        Vec3 eye = centre.add(0, EYE_HEIGHT[Math.min(3, stage)], 0);
        Vec3 seen = player == null ? null : player.getEyePosition(partialTick);
        if (stage == 3 && seen != null) {
            // The Cerebrum's eye is on its surface, the side facing whoever it looks for.
            Vec3 toward = seen.subtract(centre);
            if (toward.lengthSqr() > 1e-4) {
                eye = centre.add(toward.normalize().scale(Math.min(2.6, toward.length() * 0.5)));
            }
        }
        if (time - checked >= CHECK || time < checked) {
            checked = time;
            sees = seen != null && !player.isSpectator() && seen.distanceTo(eye) < SIGHT && clear(level, eye, seen);
        }

        Vector3f target;
        if (sees) {
            target = new Vector3f((float) (seen.x - centre.x) * 16, (float) (seen.y - centre.y) * 16, (float) (seen.z - centre.z) * 16);
        } else {
            if (time >= nextWander || time < nextWander - 400) {
                nextWander = time + 30 + random.nextInt(70);
                wanderYaw = (stage <= 1 ? bodyYaw : socketYaw) + (random.nextFloat() - 0.5f) * 2.4f;
                wanderPitch = (random.nextFloat() - 0.35f) * 0.9f;
            }
            float cp = Mth.cos(wanderPitch);
            target = new Vector3f(Mth.sin(wanderYaw) * cp, Mth.sin(wanderPitch), Mth.cos(wanderYaw) * cp).mul(96);
        }
        // Eyes jump to what they look at (a tenth of a second or so), but never jitter.
        look.lerp(target, 1 - (float) Math.exp(-dt * (sees ? 0.6f : 0.25f)));

        alert = approach(alert, sees ? 1 : 0, dt * (sees ? 0.08f : 0.01f));
        boolean staring = false;
        float distance = 0;
        if (sees) {
            Vec3 toEye = eye.subtract(seen);
            distance = (float) toEye.length();
            staring = distance > 0.01 && player.getViewVector(partialTick).dot(toEye.scale(1 / distance)) > 0.985;
        }
        stare = approach(stare, staring ? 1 : 0, dt * 0.05f);
        float near = stage == 3 ? 9 : stage == 2 ? 5 : 3;
        reach = approach(reach, sees ? Mth.clamp(1 - (distance - near * 0.4f) / near, 0, 1) : 0, dt * 0.03f);

        float yaw = (float) Mth.atan2(look.x, look.z);
        float horizontal = (float) Math.sqrt(look.x * look.x + look.z * look.z);
        float pitch = Mth.clamp((float) Mth.atan2(look.y - EYE_HEIGHT[Math.min(3, stage)] * 16, horizontal), -0.5f, 0.75f);
        bodyYaw = turn(bodyYaw, yaw, dt * 0.05f);
        socketYaw = turn(socketYaw, yaw, dt * (stage == 3 ? 0.012f : 0.02f));
        socketPitch = approach(socketPitch, pitch, dt * 0.008f);
        return new CortexBody.Look(new Vector3f(look), alert, stare, bodyYaw, socketYaw, socketPitch, reach);
    }

    /** Whether light could pass from {@code from} to {@code to}: blocks that cast no shadow (glass, its own lobes) do not count. */
    private static boolean clear(Level level, Vec3 from, Vec3 to) {
        return BlockGetter.traverseBlocks(from, to, level, (getter, pos) -> {
            BlockState state = getter.getBlockState(pos);
            if (state.isAir() || state.is(ModBlocks.CORTEX_LOBE.get()) || state.is(ModBlocks.CHRONOCORTEX.get())) {
                return null;
            }
            return state.getOcclusionShape(getter, pos).clip(from, to, pos) != null ? Boolean.FALSE : null;
        }, getter -> Boolean.TRUE);
    }

    private static float approach(float from, float to, float step) {
        return from < to ? Math.min(to, from + step) : Math.max(to, from - step);
    }

    /** An angle turned towards another by at most {@code step}, the short way round. */
    private static float turn(float from, float to, float step) {
        float delta = Mth.wrapDegrees((to - from) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
        return from + Mth.clamp(delta, -step, step);
    }
}
