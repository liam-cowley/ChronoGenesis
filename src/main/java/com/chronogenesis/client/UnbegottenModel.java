package com.chronogenesis.client;

import java.util.ArrayList;
import java.util.List;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.entity.Unbegotten;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Unbegotten: a mass of flesh under a ridged crown, one great eye in its face (its pupil follows you), a ringed
 * maw beneath, eight tentacles hanging from round the maw that sway and curl, and five eye-stalks on its crown.
 * Drawn at twice its model size ({@link UnbegottenRenderer}). Texture {@code textures/entity/unbegotten.png}
 * (128x64), its glowing parts in {@code unbegotten_eyes.png}.
 */
public class UnbegottenModel extends HierarchicalModel<Unbegotten> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ChronoGenesis.id("unbegotten"), "main");
    private static final int TENTACLES = 8;
    private static final int SEGMENTS = 6;
    private static final int STALKS = 5;
    private static final int STALK_SEGMENTS = 3;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart pupil;
    private final ModelPart maw;
    private final List<List<ModelPart>> tentacles = new ArrayList<>();
    private final List<List<ModelPart>> stalks = new ArrayList<>();

    public UnbegottenModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.pupil = body.getChild("pupil");
        this.maw = body.getChild("maw");
        for (int i = 0; i < TENTACLES; i++) {
            tentacles.add(chain(body.getChild("tentacle_" + i), SEGMENTS));
        }
        for (int i = 0; i < STALKS; i++) {
            stalks.add(chain(body.getChild("stalk_" + i), STALK_SEGMENTS));
        }
    }

    private static List<ModelPart> chain(ModelPart first, int length) {
        List<ModelPart> parts = new ArrayList<>();
        ModelPart part = first;
        parts.add(part);
        for (int s = 1; s < length; s++) {
            part = part.getChild("segment");
            parts.add(part);
        }
        return parts;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-8, -8, -8, 16, 16, 16), PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("crown", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-9, -10, -6, 18, 3, 12)
                .texOffs(0, 32).addBox(-6, -12, -4, 12, 2, 8), PartPose.ZERO);
        body.addOrReplaceChild("eye", CubeListBuilder.create().texOffs(64, 0).addBox(-5, -4, -9, 10, 8, 1), PartPose.ZERO);
        body.addOrReplaceChild("pupil", CubeListBuilder.create().texOffs(64, 10).addBox(-1.5f, -3, -9.6f, 3, 6, 1), PartPose.ZERO);
        body.addOrReplaceChild("maw", CubeListBuilder.create().texOffs(0, 47).addBox(-6, 8, -6, 12, 3, 12), PartPose.ZERO);
        for (int i = 0; i < TENTACLES; i++) {
            double a = Math.PI * 2 * i / TENTACLES;
            PartDefinition part = body.addOrReplaceChild("tentacle_" + i, tentacleSegment(0),
                    PartPose.offset((float) Math.cos(a) * 6.5f, 9, (float) Math.sin(a) * 6.5f));
            for (int s = 1; s < SEGMENTS; s++) {
                part = part.addOrReplaceChild("segment", tentacleSegment(s), PartPose.offset(0, 4, 0));
            }
        }
        for (int i = 0; i < STALKS; i++) {
            double a = Math.PI * 2 * i / STALKS + 0.4;
            PartDefinition part = body.addOrReplaceChild("stalk_" + i, CubeListBuilder.create().texOffs(96, 32)
                    .addBox(-0.6f, -3, -0.6f, 1.2f, 3, 1.2f), PartPose.offset((float) Math.cos(a) * 4, -11, (float) Math.sin(a) * 3));
            for (int s = 1; s < STALK_SEGMENTS; s++) {
                CubeListBuilder segment = s == STALK_SEGMENTS - 1
                        ? CubeListBuilder.create().texOffs(104, 32).addBox(-1, -2.5f, -1, 2, 2, 2)
                        : CubeListBuilder.create().texOffs(96, 32).addBox(-0.6f, -3, -0.6f, 1.2f, 3, 1.2f);
                part = part.addOrReplaceChild("segment", segment, PartPose.offset(0, -3, 0));
            }
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** A link of a tentacle, thinner towards the end; the last is its green, glowing tip. */
    private static CubeListBuilder tentacleSegment(int index) {
        float thick = 3.0f - 0.38f * index;
        return CubeListBuilder.create().texOffs(index == SEGMENTS - 1 ? 80 : 64, 32)
                .addBox(-thick / 2, 0, -thick / 2, thick, 4, thick);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(Unbegotten entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch) {
        body.y = Mth.sin(ageInTicks * 0.05f) * 1.2f;
        // Once the maw has opened, it gapes; inhaling, it gapes wide.
        float gape = entity.phase() >= Unbegotten.PHASE_MAW ? (entity.inhaling() ? 3.5f : 1.2f + 0.6f * Mth.sin(ageInTicks * 0.1f)) : 0;
        maw.y = gape;
        // The pupil follows whoever it looks at.
        pupil.x = Mth.clamp(netHeadYaw / 18.0f, -3.0f, 3.0f);
        pupil.y = Mth.clamp(headPitch / 25.0f, -1.0f, 1.0f);
        for (int i = 0; i < tentacles.size(); i++) {
            double a = Math.PI * 2 * i / TENTACLES;
            float phase = i * 0.8f;
            List<ModelPart> chain = tentacles.get(i);
            for (int s = 0; s < chain.size(); s++) {
                ModelPart part = chain.get(s);
                float wave = 0.16f + 0.05f * s;
                float outX = s == 0 ? (float) Math.sin(a) * 0.45f : 0;
                float outZ = s == 0 ? -(float) Math.cos(a) * 0.45f : 0;
                part.xRot = outX + Mth.sin(ageInTicks * 0.09f + phase + s * 0.7f) * wave;
                part.zRot = outZ + Mth.cos(ageInTicks * 0.07f + phase * 1.3f + s * 0.6f) * wave;
            }
        }
        for (int i = 0; i < stalks.size(); i++) {
            List<ModelPart> chain = stalks.get(i);
            for (int s = 0; s < chain.size(); s++) {
                chain.get(s).xRot = Mth.sin(ageInTicks * 0.12f + i + s * 0.9f) * 0.35f;
                chain.get(s).zRot = Mth.cos(ageInTicks * 0.1f + i * 1.7f + s * 0.8f) * 0.35f;
            }
        }
    }
}
