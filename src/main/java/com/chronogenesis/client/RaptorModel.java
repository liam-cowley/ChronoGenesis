package com.chronogenesis.client;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.entity.Raptor;

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
 * A Raptor: a lean body held level over two long legs, a curved neck, a long head, a stiff tail for balance, small
 * clawed arms. The texture layout ({@code textures/entity/raptor.png}, 64x64) is painted by {@code tools/textures.py}
 * from the same boxes.
 */
public class RaptorModel extends HierarchicalModel<Raptor> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ChronoGenesis.id("raptor"), "main");

    private final ModelPart root;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public RaptorModel(ModelPart root) {
        this.root = root;
        this.neck = root.getChild("neck");
        this.head = neck.getChild("head");
        this.tail = root.getChild("tail");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -3, -6, 6, 6, 12), PartPose.offset(0, 12, 0));
        PartDefinition neck = root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(36, 0).addBox(-2, -8, -2, 4, 8, 4),
                PartPose.offsetAndRotation(0, 11, -5, 0.4f, 0, 0));
        neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 18).addBox(-2.5f, -3, -7, 5, 5, 7),
                PartPose.offsetAndRotation(0, -7, 0, -0.4f, 0, 0));
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(24, 18).addBox(-2, -2, 0, 4, 4, 10),
                PartPose.offsetAndRotation(0, 11, 5, -0.15f, 0, 0));
        tail.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(0, 32).addBox(-1, -1, 0, 2, 2, 9),
                PartPose.offsetAndRotation(0, 0, 10, 0.1f, 0, 0));
        for (int side : new int[] {-1, 1}) {
            PartDefinition leg = root.addOrReplaceChild(side < 0 ? "right_leg" : "left_leg",
                    CubeListBuilder.create().texOffs(22, 32).addBox(-1.5f, 0, -2, 3, 6, 4), PartPose.offset(3 * side, 12, 1));
            leg.addOrReplaceChild("shin", CubeListBuilder.create().texOffs(36, 32).addBox(-1, 0, -1, 2, 6, 2), PartPose.offset(0, 6, 1));
            root.addOrReplaceChild(side < 0 ? "right_arm" : "left_arm",
                    CubeListBuilder.create().texOffs(44, 32).addBox(-0.5f, 0, -0.5f, 1, 4, 1),
                    PartPose.offsetAndRotation(2.5f * side, 13, -5, -0.6f, 0, 0));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(Raptor entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.6f;
        neck.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.4f;
        head.xRot = -0.4f + headPitch * Mth.DEG_TO_RAD;
        float stride = Mth.cos(limbSwing * 0.6662f) * 1.2f * limbSwingAmount;
        rightLeg.xRot = stride;
        leftLeg.xRot = -stride;
        tail.yRot = Mth.sin(ageInTicks * 0.15f) * 0.15f + Mth.cos(limbSwing * 0.6662f) * 0.2f * limbSwingAmount;
    }
}
