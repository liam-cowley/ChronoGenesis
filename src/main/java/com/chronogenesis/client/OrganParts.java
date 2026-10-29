package com.chronogenesis.client;

import com.chronogenesis.ChronoGenesis;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * The living parts of the machines, drawn animated (core-systems.md section 2, "Looks"): flesh masses, eyes, iron
 * straps, tendons, amethyst shards, sacs, brain folds, teeth, brass, tentacle segments. Each is one box centred on
 * its origin, drawn from {@code textures/entity/organ.png} (painted by {@code tools/textures.py}), scaled and tinted
 * by whoever draws it. Positions are in pixels (a sixteenth of a block), as model parts are.
 */
public final class OrganParts {
    public static final ResourceLocation TEXTURE = ChronoGenesis.id("textures/entity/organ.png");
    public static final int WHITE = 0xFFFFFFFF;

    /** 8 px of flesh (tinted). */
    public static final ModelPart FLESH = box(0, 0, 8, 8, 8);
    /** An eye looking along -z: 4 x 4, a pupil. */
    public static final ModelPart EYE = box(32, 0, 4, 4, 1);
    /** A band of riveted iron, 10 x 1 x 10. */
    public static final ModelPart STRAP = box(0, 16, 10, 1, 10);
    /** A tendon running along z, 1 x 1 x 10. */
    public static final ModelPart TENDON = box(0, 28, 1, 1, 10);
    /** An amethyst shard, 2 x 6 x 2. */
    public static final ModelPart SHARD = box(24, 28, 2, 6, 2);
    /** A translucent-looking sac, 6 x 6 x 6 (tinted). */
    public static final ModelPart SAC = box(32, 28, 6, 6, 6);
    /** Folds of brain, 8 x 8 x 8 (tinted). */
    public static final ModelPart GYRI = box(0, 40, 8, 8, 8);
    /** A row of teeth, 6 x 2 x 1. */
    public static final ModelPart TEETH = box(32, 40, 6, 2, 1);
    /** A brass rod, 2 x 6 x 2. */
    public static final ModelPart BRASS = box(32, 44, 2, 6, 2);
    /** A tentacle's segment running along y, 2 x 4 x 2 (tinted). */
    public static final ModelPart SEGMENT = box(40, 44, 2, 4, 2);

    private OrganParts() {}

    private static ModelPart box(int u, int v, float w, float h, float d) {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("box", CubeListBuilder.create().texOffs(u, v).addBox(-w / 2, -h / 2, -d / 2, w, h, d),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64).bakeRoot().getChild("box");
    }

    public static VertexConsumer buffer(MultiBufferSource buffers) {
        return buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
    }

    /** Draws a part at ({@code x, y, z}) px, scaled by ({@code sx, sy, sz}), tinted {@code color} (ARGB). */
    public static void draw(ModelPart part, PoseStack pose, VertexConsumer buffer, int light, float x, float y, float z,
            float sx, float sy, float sz, int color) {
        pose.pushPose();
        pose.translate(x / 16.0f, y / 16.0f, z / 16.0f);
        pose.scale(sx, sy, sz);
        part.render(pose, buffer, light, OverlayTexture.NO_OVERLAY, color);
        pose.popPose();
    }

    public static void draw(ModelPart part, PoseStack pose, VertexConsumer buffer, int light, float x, float y, float z, float s, int color) {
        draw(part, pose, buffer, light, x, y, z, s, s, s, color);
    }

    /** An eye at ({@code x, y, z}) px turned to look at {@code target} (world), blinking now and then. */
    public static void eye(PoseStack pose, VertexConsumer buffer, int light, float x, float y, float z, float s,
            Vec3 from, Vec3 target, float time, long seed, int color) {
        pose.pushPose();
        pose.translate(x / 16.0f, y / 16.0f, z / 16.0f);
        Vec3 d = target.subtract(from);
        float yaw = (float) Mth.atan2(d.x, d.z);
        float pitch = (float) Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z));
        pose.mulPose(Axis.YP.rotation(yaw + Mth.PI));
        pose.mulPose(Axis.XP.rotation(pitch));
        float blink = blink(time, seed);
        pose.scale(s, s * blink, s);
        EYE.render(pose, buffer, light, OverlayTexture.NO_OVERLAY, color);
        pose.popPose();
    }

    /** 1 open, falling to a slit for a few ticks every few seconds. */
    public static float blink(float time, long seed) {
        float period = 70 + (seed & 63);
        float t = (time + (seed >> 6 & 127)) % period;
        return t < 4 ? Math.max(0.1f, Math.abs(t - 2) / 2.0f) : 1.0f;
    }

    /** A heartbeat: two quick swells, then rest. 0 at rest, about 1 at the peak. */
    public static float beat(float time, float period) {
        float t = (time % period) / period;
        float first = Math.max(0, 1 - Math.abs(t - 0.08f) / 0.08f);
        float second = Math.max(0, 1 - Math.abs(t - 0.28f) / 0.1f) * 0.6f;
        return first + second;
    }

    /** Where the local player's eyes are (what eyes look at), or a point in front if there is none. */
    public static Vec3 watcher(Vec3 fallback) {
        Player player = Minecraft.getInstance().player;
        return player == null ? fallback : player.getEyePosition();
    }

    /** {@code rgb} with full alpha, scaled towards black by {@code light} (0 to 1). */
    public static int tint(int rgb, float light) {
        return FastColor.ARGB32.color(255, (int) (FastColor.ARGB32.red(rgb) * light), (int) (FastColor.ARGB32.green(rgb) * light),
                (int) (FastColor.ARGB32.blue(rgb) * light));
    }

    /** Mixes two RGB colours, {@code t} of the way from {@code a} to {@code b}. */
    public static int mix(int a, int b, float t) {
        t = Mth.clamp(t, 0, 1);
        return FastColor.ARGB32.color(255,
                (int) Mth.lerp(t, FastColor.ARGB32.red(a), FastColor.ARGB32.red(b)),
                (int) Mth.lerp(t, FastColor.ARGB32.green(a), FastColor.ARGB32.green(b)),
                (int) Mth.lerp(t, FastColor.ARGB32.blue(a), FastColor.ARGB32.blue(b)));
    }

    /**
     * A tentacle: {@code segments} segments chained from the current pose's origin along +y, each bending a little
     * more than the last in a travelling wave, tapering.
     */
    public static void tentacle(PoseStack pose, VertexConsumer buffer, int light, int segments, float thickness, float time,
            float phase, float sway, int color) {
        pose.pushPose();
        for (int i = 0; i < segments; i++) {
            float taper = thickness * (1.0f - 0.6f * i / segments);
            pose.mulPose(Axis.ZP.rotation(Mth.sin(time * 0.08f + phase + i * 0.7f) * sway));
            pose.mulPose(Axis.XP.rotation(Mth.cos(time * 0.06f + phase * 1.3f + i * 0.5f) * sway * 0.8f));
            pose.pushPose();
            pose.translate(0, 2.0f / 16.0f * taper, 0);
            pose.scale(taper, taper, taper);
            SEGMENT.render(pose, buffer, light, OverlayTexture.NO_OVERLAY, color);
            pose.popPose();
            pose.translate(0, 3.6f / 16.0f * taper, 0);
        }
        pose.popPose();
    }
}
