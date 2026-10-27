package com.chronogenesis.client;

import com.chronogenesis.essence.EssenceMoteOptions;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A glowing mote of essence leaving a creature. It first drifts up and out of the body, then is pulled ever
 * harder towards its target, curving in, and shrinks as it is swallowed. Full-bright, tinted by the essence.
 */
public class EssenceMoteParticle extends TextureSheetParticle {
    /** Ticks spent drifting out of the body before the pull takes over. */
    private static final int DRIFT_TICKS = 6;
    private static final double MAX_SPEED = 0.55;

    private final Vec3 target;
    private final float baseSize;
    /** Which way it curls as it is pulled in, so a stream of motes twists rather than travelling in a line. */
    private final double curl;

    protected EssenceMoteParticle(ClientLevel level, double x, double y, double z, EssenceMoteOptions options, SpriteSet sprites) {
        super(level, x, y, z);
        this.target = options.target();
        int color = options.color();
        // Lift the colour a little so dark essences (Density) still glow.
        this.rCol = Mth.lerp(0.25f, (color >> 16 & 0xFF) / 255f, 1f);
        this.gCol = Mth.lerp(0.25f, (color >> 8 & 0xFF) / 255f, 1f);
        this.bCol = Mth.lerp(0.25f, (color & 0xFF) / 255f, 1f);
        this.alpha = 0.95f;
        this.baseSize = 0.06f + random.nextFloat() * 0.06f;
        this.quadSize = baseSize;
        this.lifetime = 60;
        this.hasPhysics = false;
        this.gravity = 0;
        this.curl = (random.nextBoolean() ? 1 : -1) * (0.04 + random.nextDouble() * 0.05);
        // Seeping out of the body: a slow upward ooze.
        this.xd = (random.nextDouble() - 0.5) * 0.04;
        this.yd = 0.03 + random.nextDouble() * 0.03;
        this.zd = (random.nextDouble() - 0.5) * 0.04;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        Vec3 here = new Vec3(x, y, z);
        Vec3 toTarget = target.subtract(here);
        double distance = toTarget.length();
        if (distance < 0.2) {
            remove(); // swallowed
            return;
        }
        if (age > DRIFT_TICKS) {
            // The pull grows stronger every tick; a sideways curl makes the stream spiral in.
            double speed = Math.min(MAX_SPEED, 0.08 + (age - DRIFT_TICKS) * 0.03);
            Vec3 pull = toTarget.normalize().scale(speed);
            Vec3 side = toTarget.cross(new Vec3(0, 1, 0));
            if (side.lengthSqr() > 1.0e-4) {
                pull = pull.add(side.normalize().scale(curl * Math.min(1.0, distance)));
            }
            double steer = Math.min(1.0, 0.25 + (age - DRIFT_TICKS) * 0.05);
            xd += (pull.x - xd) * steer;
            yd += (pull.y - yd) * steer;
            zd += (pull.z - zd) * steer;
        }
        // Never overshoot the mouth.
        double step = Math.sqrt(xd * xd + yd * yd + zd * zd);
        double scale = step > distance ? distance / step : 1.0;
        x += xd * scale;
        y += yd * scale;
        z += zd * scale;
        // Shrinks as it nears the mouth.
        quadSize = baseSize * (float) Math.min(1.0, 0.35 + distance * 0.5);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0; // glows in the dark
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<EssenceMoteOptions> {
        @Override
        public Particle createParticle(EssenceMoteOptions options, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new EssenceMoteParticle(level, x, y, z, options, sprites);
        }
    }
}
