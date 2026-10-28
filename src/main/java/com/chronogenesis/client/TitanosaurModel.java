package com.chronogenesis.client;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.entity.Titanosaur;

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
 * A Titanosaur: a barrel of a body on four pillar legs, a long neck raised high, a small head, a long tail held off
 * the ground. Drawn at 2.4x ({@link TitanosaurRenderer}); its back stays below the harness, so cargo sits clear of
 * it. Texture: {@code textures/entity/titanosaur.png}, 128x160, painted by {@code tools/textures.py}.
 */
public class TitanosaurModel extends HierarchicalModel<Titanosaur> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ChronoGenesis.id("titanosaur"), "main");
    private static final float NECK_RAISE = -0.85f;

    private final ModelPart root;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart[] legs = new ModelPart[4];

    public TitanosaurModel(ModelPart root) {
        this.root = root;
        this.neck = root.getChild("neck");
        this.head = neck.getChild("head");
        this.tail = root.getChild("tail");
        for (int i = 0; i < 4; i++) {
            legs[i] = root.getChild("leg_" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-11, -8, -17, 22, 16, 34), PartPose.offset(0, 2, 0));
        int[][] legPositions = {{-8, -11}, {8, -11}, {-8, 11}, {8, 11}};
        for (int i = 0; i < 4; i++) {
            root.addOrReplaceChild("leg_" + i, CubeListBuilder.create().texOffs(0, 50).addBox(-3, 0, -3, 6, 14, 6),
                    PartPose.offset(legPositions[i][0], 10, legPositions[i][1]));
        }
        PartDefinition neck = root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(0, 70).addBox(-4, -4, -26, 8, 8, 26),
                PartPose.offsetAndRotation(0, 0, -15, NECK_RAISE, 0, 0));
        neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(68, 70).addBox(-4, -3, -9, 8, 7, 10),
                PartPose.offsetAndRotation(0, 0, -26, -NECK_RAISE, 0, 0));
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 104).addBox(-4, -4, 0, 8, 8, 24),
                PartPose.offsetAndRotation(0, 0, 16, -0.25f, 0, 0));
        tail.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(64, 104).addBox(-2.5f, -2.5f, 0, 5, 5, 22),
                PartPose.offsetAndRotation(0, 0, 24, -0.15f, 0, 0));
        return LayerDefinition.create(mesh, 128, 160);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(Titanosaur entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float stride = Mth.cos(limbSwing * 0.4f) * 0.5f * limbSwingAmount;
        legs[0].xRot = stride;
        legs[3].xRot = stride;
        legs[1].xRot = -stride;
        legs[2].xRot = -stride;
        neck.xRot = NECK_RAISE + Mth.sin(ageInTicks * 0.04f) * 0.04f;
        neck.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.3f;
        head.xRot = -NECK_RAISE + headPitch * Mth.DEG_TO_RAD * 0.5f;
        tail.yRot = Mth.sin(ageInTicks * 0.05f) * 0.12f + Mth.cos(limbSwing * 0.4f) * 0.1f * limbSwingAmount;
    }
}
