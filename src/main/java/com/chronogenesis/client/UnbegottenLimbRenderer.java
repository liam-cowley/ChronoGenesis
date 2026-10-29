package com.chronogenesis.client;

import com.chronogenesis.entity.UnbegottenLimb;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A limb of the Unbegotten, drawn from its organ parts ({@link OrganParts}) along a curve: a <b>tentacle</b> towers out
 * of the void, swaying, eyes opening along it; winding up it rears back from where it will land; slammed, it lies
 * across the arena to that point; sweeping, it swings round its root. An <b>eye</b> is a great eyeball on a stalk,
 * turned on whoever it watches, burning bright while it charges its gaze.
 */
public class UnbegottenLimbRenderer extends EntityRenderer<UnbegottenLimb> {
    private static final int FLESH = 0x6E2E7E;
    private static final int TIP = 0x6AE09A;
    private static final int SEGMENTS = 24;

    public UnbegottenLimbRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override
    public boolean shouldRender(UnbegottenLimb limb, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(limb.getBoundingBox().inflate(16));
    }

    @Override
    public void render(UnbegottenLimb limb, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        VertexConsumer buffer = OrganParts.buffer(buffers);
        float time = limb.tickCount + partialTick;
        long seed = limb.getUUID().getLeastSignificantBits();
        Vec3 base = limb.getPosition(partialTick);
        Vec3 aim = Vec3.atBottomCenterOf(limb.aim()).subtract(base);
        float t = limb.clientActionTicks + partialTick;
        if (limb.eye()) {
            renderEye(limb, pose, buffer, light, time, seed, base, aim);
            return;
        }
        Vec3 sway = new Vec3(Mth.sin(time * 0.03f + seed) * 2.0, 0, Mth.cos(time * 0.025f + seed) * 2.0);
        Vec3 tip = sway.add(0, 16, 0);
        Vec3 bend = new Vec3(0, 9, 0);
        Vec3 flat = new Vec3(aim.x, 0, aim.z);
        switch (limb.action()) {
            case UnbegottenLimb.WINDING -> {
                // Rearing back from where it will fall.
                Vec3 back = flat.lengthSqr() > 1.0E-4 ? flat.normalize().scale(-4.0 * Math.min(1.0f, t / 20.0f)) : Vec3.ZERO;
                tip = tip.add(back).add(0, 3.0 * Math.min(1.0f, t / 20.0f), 0);
            }
            case UnbegottenLimb.SLAMMED -> {
                float fall = Math.min(1.0f, t / 4.0f);
                Vec3 lying = aim.add(0, 0.6, 0);
                tip = tip.lerp(lying, fall);
                bend = bend.lerp(aim.scale(0.35).add(0, 7.0, 0), fall);
            }
            case UnbegottenLimb.SWEEPING -> {
                float swing = Mth.clamp(t / 24.0f, 0, 1);
                double a = Math.atan2(flat.z, flat.x) + (swing - 0.5) * 2.4;
                tip = new Vec3(Math.cos(a) * 9.0, 7.0 - 3.0 * swing, Math.sin(a) * 9.0);
                bend = new Vec3(tip.x * 0.3, 8.0, tip.z * 0.3);
            }
            default -> { }
        }
        chain(pose, buffer, light, base, Vec3.ZERO, bend, tip, 1.6f, 0.3f, time, seed, true);
    }

    private void renderEye(UnbegottenLimb limb, PoseStack pose, VertexConsumer buffer, int light, float time, long seed, Vec3 base, Vec3 aim) {
        Vec3 top = new Vec3(Mth.sin(time * 0.04f + seed) * 0.8, 9.0, Mth.cos(time * 0.035f + seed) * 0.8);
        chain(pose, buffer, light, base, Vec3.ZERO, new Vec3(0, 5, 0), top, 0.7f, 0.5f, time, seed, false);
        boolean charging = limb.action() == UnbegottenLimb.WINDING;
        int glow = charging ? LightTexture.FULL_BRIGHT : light;
        float size = 2.2f + (charging ? 0.15f * Mth.sin(time * 0.6f) : 0);
        OrganParts.draw(OrganParts.FLESH, pose, buffer, light, (float) top.x * 16, (float) top.y * 16 + 14, (float) top.z * 16,
                size * 2.2f, FLESH | 0xFF000000);
        Vec3 from = base.add(top).add(0, 0.875, 0);
        Vec3 target = limb.aim().equals(net.minecraft.core.BlockPos.ZERO) ? OrganParts.watcher(from.add(0, 0, 4)) : base.add(aim).add(0, 1.6, 0);
        OrganParts.eye(pose, buffer, glow, (float) top.x * 16, (float) top.y * 16 + 14, (float) top.z * 16,
                size * 3.2f, from, target, time, seed, charging ? 0xFFB0FF90 : OrganParts.WHITE);
    }

    /**
     * Segments along a curve from {@code p0} through {@code p1} towards {@code p2} (blocks, relative to the limb),
     * tapering from {@code thick} to {@code thick * end}, flesh shading to its glowing tip; eyes open along a tentacle.
     */
    private static void chain(PoseStack pose, VertexConsumer buffer, int light, Vec3 base, Vec3 p0, Vec3 p1, Vec3 p2, float thick, float end,
            float time, long seed, boolean eyes) {
        Vec3 last = p0;
        for (int i = 1; i <= SEGMENTS; i++) {
            float u = (float) i / SEGMENTS;
            Vec3 next = bezier(p0, p1, p2, u);
            // A ripple runs along it.
            next = next.add(Mth.sin(time * 0.1f + i * 0.5f + seed) * 0.15 * u, 0, Mth.cos(time * 0.09f + i * 0.4f) * 0.15 * u);
            Vec3 d = next.subtract(last);
            double length = d.length();
            if (length > 1.0E-4) {
                float width = thick * Mth.lerp(u, 1.0f, end);
                int color = OrganParts.mix(FLESH, TIP, Math.max(0, (u - 0.7f) / 0.3f));
                pose.pushPose();
                Vec3 mid = last.add(d.scale(0.5));
                pose.translate(mid.x, mid.y, mid.z);
                pose.mulPose(Axis.YP.rotation((float) Mth.atan2(d.x, d.z)));
                pose.mulPose(Axis.XP.rotation((float) Math.acos(Mth.clamp(d.y / length, -1, 1))));
                pose.scale(width * 8.0f, (float) length * 4.0f * 1.15f, width * 8.0f);
                OrganParts.SEGMENT.render(pose, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
                pose.popPose();
                if (eyes && i % 6 == 3) {
                    Vec3 at = mid.add(new Vec3(-d.z, 0, d.x).normalize().scale(width * 0.55));
                    OrganParts.eye(pose, buffer, light, (float) at.x * 16, (float) at.y * 16, (float) at.z * 16, width * 2.2f,
                            base.add(at), OrganParts.watcher(base.add(at).add(0, 0, 4)), time, seed + i, OrganParts.WHITE);
                }
            }
            last = next;
        }
    }

    private static Vec3 bezier(Vec3 a, Vec3 b, Vec3 c, float u) {
        return a.scale((1 - u) * (1 - u)).add(b.scale(2 * (1 - u) * u)).add(c.scale(u * u));
    }

    @Override
    public ResourceLocation getTextureLocation(UnbegottenLimb limb) {
        return OrganParts.TEXTURE;
    }
}
