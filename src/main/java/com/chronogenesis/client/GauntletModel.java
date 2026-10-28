package com.chronogenesis.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Essence Manipulator as a 3D gauntlet: a leather glove (fingers up, curled towards the palm) with a brass knuckle
 * guard and back plate, a riveted copper cuff flaring from the wrist to a brass rim, and an eldritch eye set in the
 * back of the hand. Five tendrils grow out of
 * its seams (between the fingers, the wrist, the eye's socket, the cuff's edge): chains of tapering segments that
 * writhe, each on its own phase, and now and then draw back into their slits.
 * <p>
 * Texture: {@code textures/item/essence_manipulator_gauntlet.png} (64x64, painted by {@code tools/textures.py} for
 * these boxes). Units are model pixels, y down, centred on the origin.
 */
public final class GauntletModel {
    private static final int SEGMENTS = 4;

    /** A tendril: where it grows out, which way, and its phase. */
    private record Tendril(ModelPart root, List<ModelPart> segments, float baseX, float baseZ, float phase) {}

    private final ModelPart root;
    private final List<Tendril> tendrils = new ArrayList<>();

    public GauntletModel() {
        root = createLayer().bakeRoot();
        float[][] anchors = {
                // x, y, z, base xRot, base zRot, phase
                {-1.5f, -4.0f, -0.6f, 0.45f, -0.2f, 0.0f},                // between the first fingers
                {1.6f, -4.0f, 0.6f, -0.35f, 0.25f, 1.7f},                 // between the last fingers
                {2.6f, 2.8f, -1.0f, 0.0f, Mth.HALF_PI, 3.1f},              // the wrist seam, outwards
                {0.0f, -0.5f, -2.4f, Mth.HALF_PI, 0.0f, 4.4f},             // the eye's socket, backwards
                {-3.6f, 7.0f, 1.0f, 0.2f, -Mth.HALF_PI, 5.6f},             // the cuff's edge
        };
        for (int i = 0; i < anchors.length; i++) {
            ModelPart tendril = root.getChild("tendril_" + i);
            List<ModelPart> segments = new ArrayList<>();
            ModelPart part = tendril;
            segments.add(part);
            for (int s = 1; s < SEGMENTS; s++) {
                part = part.getChild("segment");
                segments.add(part);
            }
            tendrils.add(new Tendril(tendril, segments, anchors[i][3], anchors[i][4], anchors[i][5]));
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // The cuff (copper, riveted), flaring from the wrist to a brass rim at the opening.
        root.addOrReplaceChild("wrist", CubeListBuilder.create().texOffs(24, 21).addBox(-2.5f, 0, -2, 5, 2, 4), PartPose.offset(0, 2, 0));
        root.addOrReplaceChild("cuff", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5f, 0, -3.5f, 7, 5, 7), PartPose.offset(0, 4, 0));
        root.addOrReplaceChild("rim", CubeListBuilder.create().texOffs(28, 6).addBox(-4, 0, -4, 8, 1, 8), PartPose.offset(0, 9, 0));
        for (float[] rivet : new float[][] {{-2, 5.5f}, {2, 5.5f}, {-2, 8}, {2, 8}}) {
            root.addOrReplaceChild("rivet_" + (int) rivet[0] + "_" + (int) rivet[1],
                    CubeListBuilder.create().texOffs(36, 15).addBox(-0.5f, -0.5f, -0.4f, 1, 1, 1), PartPose.offset(rivet[0], rivet[1], -3.6f));
        }
        // The hand (leather), its brass back plate with the eye set in it, and the knuckle guard.
        root.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(0, 13).addBox(-3, -5, -1.5f, 6, 5, 3), PartPose.offset(0, 2, 0));
        root.addOrReplaceChild("plate", CubeListBuilder.create().texOffs(44, 21).addBox(-2.5f, -4, -0.5f, 5, 4, 0.5f),
                PartPose.offset(0, 1.5f, -1.5f));
        root.addOrReplaceChild("eye", CubeListBuilder.create().texOffs(18, 13).addBox(-1, -1, -0.5f, 2, 2, 1), PartPose.offset(0, -0.5f, -2.0f));
        root.addOrReplaceChild("knuckles", CubeListBuilder.create().texOffs(28, 0).addBox(-3.5f, -1, -2, 7, 1.5f, 4), PartPose.offset(0, -3, 0));
        // Four fingers, each in two segments, curled towards the palm; the thumb forward on the palm side.
        float[] fingers = {-2.25f, -0.75f, 0.75f, 2.25f};
        float[] lengths = {2.6f, 3.0f, 3.0f, 2.4f};
        for (int i = 0; i < fingers.length; i++) {
            PartDefinition finger = root.addOrReplaceChild("finger_" + i,
                    CubeListBuilder.create().texOffs(0, 21).addBox(-0.75f, -lengths[i], -0.75f, 1.5f, lengths[i], 1.5f),
                    PartPose.offsetAndRotation(fingers[i], -4, 0, -0.3f, 0, (i - 1.5f) * -0.06f));
            finger.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(8, 21).addBox(-0.6f, -2.2f, -0.6f, 1.2f, 2.2f, 1.2f),
                    PartPose.offsetAndRotation(0, -lengths[i], 0, -0.55f, 0, 0));
        }
        PartDefinition thumb = root.addOrReplaceChild("thumb",
                CubeListBuilder.create().texOffs(16, 21).addBox(-0.75f, -3, -0.75f, 1.5f, 3, 1.5f),
                PartPose.offsetAndRotation(-2.8f, -0.8f, 0.9f, -0.5f, 0, -0.55f));
        thumb.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(8, 21).addBox(-0.6f, -2, -0.6f, 1.2f, 2, 1.2f),
                PartPose.offsetAndRotation(0, -3, 0, -0.35f, 0, 0.25f));
        // The tendrils: tapering chains, rooted in the seams (posed every frame).
        float[][] anchors = {{-1.5f, -4.0f, -0.6f}, {1.6f, -4.0f, 0.6f}, {2.6f, 2.8f, -1.0f}, {0.0f, -0.5f, -2.4f}, {-3.6f, 7.0f, 1.0f}};
        for (int i = 0; i < anchors.length; i++) {
            PartDefinition part = root.addOrReplaceChild("tendril_" + i, segment(0), PartPose.offset(anchors[i][0], anchors[i][1], anchors[i][2]));
            for (int s = 1; s < SEGMENTS; s++) {
                part = part.addOrReplaceChild("segment", segment(s), PartPose.offset(0, -1.8f, 0));
            }
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** One link of a tendril, thinner towards the tip; the last is the green tip. */
    private static CubeListBuilder segment(int index) {
        float thick = 1.0f - 0.2f * index;
        return CubeListBuilder.create().texOffs(index == SEGMENTS - 1 ? 8 : 0, 32)
                .addBox(-thick / 2, -2, -thick / 2, thick, 2, thick);
    }

    /** Poses the tendrils for a moment in time (ticks, with the partial tick). */
    public void animate(float time) {
        for (Tendril tendril : tendrils) {
            float phase = tendril.phase;
            // Now and then they draw back into their slits.
            float reach = 0.35f + 0.65f * Mth.clamp(0.5f + 0.9f * Mth.sin(time * 0.045f + phase), 0.0f, 1.0f);
            tendril.root.xScale = reach;
            tendril.root.yScale = reach;
            tendril.root.zScale = reach;
            for (int s = 0; s < tendril.segments.size(); s++) {
                ModelPart part = tendril.segments.get(s);
                float wave = 0.32f + 0.08f * s;
                part.xRot = (s == 0 ? tendril.baseX : 0) + Mth.sin(time * 0.16f + phase + s * 0.9f) * wave;
                part.zRot = (s == 0 ? tendril.baseZ : 0) + Mth.cos(time * 0.12f + phase * 1.3f + s * 0.8f) * wave;
            }
        }
    }

    public void render(PoseStack poseStack, VertexConsumer buffer, int light, int overlay) {
        root.render(poseStack, buffer, light, overlay);
    }
}
