package com.chronogenesis.client;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.entity.Mammoth;
import com.chronogenesis.entity.Smilodon;
import com.chronogenesis.entity.WoollyRhino;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Ice Age's creatures as models: the Mammoth (a domed head, a three-jointed trunk, tusks curling up, a skirt of
 * long hair), the Smilodon (a long low cat, the sabres hanging below its jaw) and the Woolly Rhinoceros (a shoulder
 * hump, two horns). Textures painted by {@code tools/textures.py} to these boxes.
 */
public final class IceAgeModels {
    private IceAgeModels() {}

    /** Four legs, front pair and back pair walking opposite. */
    static void walk(ModelPart[] legs, float limbSwing, float amount, float speed) {
        float stride = Mth.cos(limbSwing * speed) * 0.7f * amount;
        legs[0].xRot = stride;
        legs[3].xRot = stride;
        legs[1].xRot = -stride;
        legs[2].xRot = -stride;
    }

    static void legs(PartDefinition root, int u, int v, float w, float h, float x, float y, float z) {
        float[][] at = {{-x, -z}, {x, -z}, {-x, z}, {x, z}};
        for (int i = 0; i < 4; i++) {
            root.addOrReplaceChild("leg_" + i, CubeListBuilder.create().texOffs(u, v).addBox(-w / 2, 0, -w / 2, w, h, w),
                    PartPose.offset(at[i][0], y, at[i][1]));
        }
    }

    // ---------------------------------------------------------------- the Mammoth

    public static class MammothModel extends HierarchicalModel<Mammoth> {
        public static final ModelLayerLocation LAYER = new ModelLayerLocation(ChronoGenesis.id("mammoth"), "main");
        private final ModelPart root;
        private final ModelPart head;
        private final ModelPart[] trunk = new ModelPart[3];
        private final ModelPart tail;
        private final ModelPart[] legs = new ModelPart[4];
        private final ModelPart[] ears = new ModelPart[2];

        public MammothModel(ModelPart root) {
            this.root = root;
            this.head = root.getChild("head");
            trunk[0] = head.getChild("trunk_0");
            trunk[1] = trunk[0].getChild("trunk_1");
            trunk[2] = trunk[1].getChild("trunk_2");
            ears[0] = head.getChild("ear_0");
            ears[1] = head.getChild("ear_1");
            tail = root.getChild("tail");
            for (int i = 0; i < 4; i++) {
                legs[i] = root.getChild("leg_" + i);
            }
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-9, -9, -14, 18, 18, 28)
                    .texOffs(0, 46).addBox(-9.5f, 6, -14.5f, 19, 5, 29), PartPose.offset(0, -1, 0));
            PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 80).addBox(-6, -7, -8, 12, 13, 10)
                    .texOffs(44, 80).addBox(-4, -11, -6, 8, 4, 7), PartPose.offsetAndRotation(0, -4, -14, 0.25f, 0, 0));
            PartDefinition t0 = head.addOrReplaceChild("trunk_0", CubeListBuilder.create().texOffs(76, 80).addBox(-2, 0, -2, 4, 8, 4),
                    PartPose.offsetAndRotation(0, 4, -7, 0.1f, 0, 0));
            PartDefinition t1 = t0.addOrReplaceChild("trunk_1", CubeListBuilder.create().texOffs(76, 92).addBox(-1.5f, 0, -1.5f, 3, 7, 3),
                    PartPose.offsetAndRotation(0, 7.5f, 0, 0.15f, 0, 0));
            t1.addOrReplaceChild("trunk_2", CubeListBuilder.create().texOffs(92, 80).addBox(-1, 0, -1, 2, 6, 2),
                    PartPose.offsetAndRotation(0, 6.5f, 0, -0.35f, 0, 0));
            for (int side = 0; side < 2; side++) {
                float sx = side == 0 ? -1 : 1;
                PartDefinition tusk = head.addOrReplaceChild("tusk_" + side, CubeListBuilder.create().texOffs(96, 46).addBox(-1, -1, -8, 2, 2, 8),
                        PartPose.offsetAndRotation(3.5f * sx, 4, -6, -0.9f, 0.25f * sx, 0));
                tusk.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(96, 56).addBox(-0.75f, -0.75f, -6, 1.5f, 1.5f, 6),
                        PartPose.offsetAndRotation(0, 0, -7.5f, -0.9f, -0.35f * sx, 0));
                head.addOrReplaceChild("ear_" + side, CubeListBuilder.create().texOffs(100, 0).addBox(side == 0 ? -1 : 0, -3, 0, 1, 7, 5),
                        PartPose.offsetAndRotation(6 * sx, -3, -2, 0, 0.5f * sx, 0));
            }
            root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(24, 104).addBox(-1, 0, -1, 2, 9, 2),
                    PartPose.offsetAndRotation(0, -6, 14, 0.3f, 0, 0));
            legs(root, 0, 104, 6, 14, 5.5f, 10, 9);
            return LayerDefinition.create(mesh, 128, 128);
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void setupAnim(Mammoth entity, float limbSwing, float amount, float age, float headYaw, float headPitch) {
            walk(legs, limbSwing, amount, 0.45f);
            head.yRot = headYaw * Mth.DEG_TO_RAD * 0.4f;
            head.xRot = 0.25f + headPitch * Mth.DEG_TO_RAD * 0.3f + Mth.cos(limbSwing * 0.45f) * 0.05f * amount;
            // The trunk feels about: a slow curl travelling down it.
            trunk[0].xRot = 0.1f + Mth.sin(age * 0.06f) * 0.12f;
            trunk[1].xRot = 0.15f + Mth.sin(age * 0.06f - 0.8f) * 0.2f;
            trunk[2].xRot = -0.35f + Mth.sin(age * 0.06f - 1.6f) * 0.3f;
            trunk[0].zRot = Mth.sin(age * 0.04f) * 0.1f;
            ears[0].yRot = -0.5f - Mth.sin(age * 0.09f) * 0.15f;
            ears[1].yRot = 0.5f + Mth.sin(age * 0.09f) * 0.15f;
            tail.zRot = Mth.sin(age * 0.1f) * 0.2f;
        }
    }

    // ---------------------------------------------------------------- the Smilodon

    public static class SmilodonModel extends HierarchicalModel<Smilodon> {
        public static final ModelLayerLocation LAYER = new ModelLayerLocation(ChronoGenesis.id("smilodon"), "main");
        private final ModelPart root;
        private final ModelPart head;
        private final ModelPart jaw;
        private final ModelPart tail;
        private final ModelPart[] legs = new ModelPart[4];

        public SmilodonModel(ModelPart root) {
            this.root = root;
            this.head = root.getChild("head");
            this.jaw = head.getChild("jaw");
            this.tail = root.getChild("tail");
            for (int i = 0; i < 4; i++) {
                legs[i] = root.getChild("leg_" + i);
            }
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -4, -9, 8, 8, 18), PartPose.offset(0, 11, 0));
            PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 26).addBox(-3.5f, -3, -6, 7, 6, 7)
                    .texOffs(36, 26).addBox(-3, -5, -2, 2, 2, 1).texOffs(36, 26).addBox(1, -5, -2, 2, 2, 1), PartPose.offset(0, 9, -9));
            PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(42, 26).addBox(-2.5f, 0, -5, 5, 2, 5),
                    PartPose.offset(0, 2, -1));
            for (int side = 0; side < 2; side++) {
                head.addOrReplaceChild("sabre_" + side, CubeListBuilder.create().texOffs(28, 26).addBox(-0.5f, 0, -0.5f, 1, 5, 1),
                        PartPose.offsetAndRotation(side == 0 ? -1.6f : 1.6f, 2, -5.3f, -0.15f, 0, 0));
            }
            root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(12, 40).addBox(-1, 0, -1, 2, 9, 2),
                    PartPose.offsetAndRotation(0, 8, 9, 0.9f, 0, 0));
            legs(root, 0, 40, 3, 10, 2.5f, 14, 6.5f);
            return LayerDefinition.create(mesh, 64, 64);
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void setupAnim(Smilodon entity, float limbSwing, float amount, float age, float headYaw, float headPitch) {
            walk(legs, limbSwing, amount, 0.66f);
            head.yRot = headYaw * Mth.DEG_TO_RAD;
            head.xRot = headPitch * Mth.DEG_TO_RAD;
            // A slow snarl when it is after something.
            jaw.xRot = entity.isAggressive() ? 0.35f + Mth.sin(age * 0.3f) * 0.1f : 0.0f;
            tail.yRot = Mth.sin(age * 0.12f) * 0.35f;
            tail.xRot = 0.9f + Mth.sin(age * 0.07f) * 0.1f;
        }
    }

    // ---------------------------------------------------------------- the Woolly Rhinoceros

    public static class RhinoModel extends HierarchicalModel<WoollyRhino> {
        public static final ModelLayerLocation LAYER = new ModelLayerLocation(ChronoGenesis.id("woolly_rhino"), "main");
        private final ModelPart root;
        private final ModelPart head;
        private final ModelPart tail;
        private final ModelPart[] legs = new ModelPart[4];

        public RhinoModel(ModelPart root) {
            this.root = root;
            this.head = root.getChild("head");
            this.tail = root.getChild("tail");
            for (int i = 0; i < 4; i++) {
                legs[i] = root.getChild("leg_" + i);
            }
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-7, -7, -12, 14, 14, 24)
                    .texOffs(76, 0).addBox(-5, -10, -10, 10, 4, 10), PartPose.offset(0, 7, 0));
            PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 38).addBox(-4, -4, -10, 8, 8, 10, new CubeDeformation(0.2f)),
                    PartPose.offsetAndRotation(0, 7, -12, 0.45f, 0, 0));
            head.addOrReplaceChild("horn_0", CubeListBuilder.create().texOffs(36, 38).addBox(-1, -7, -1, 2, 7, 2),
                    PartPose.offsetAndRotation(0, -2, -9, -0.5f, 0, 0));
            head.addOrReplaceChild("horn_1", CubeListBuilder.create().texOffs(44, 38).addBox(-1, -4, -1, 2, 4, 2),
                    PartPose.offsetAndRotation(0, -3.5f, -5.5f, -0.3f, 0, 0));
            root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(76, 14).addBox(-1, 0, -1, 2, 6, 2),
                    PartPose.offsetAndRotation(0, 3, 12, 0.4f, 0, 0));
            legs(root, 52, 38, 5, 10, 4.5f, 14, 8);
            return LayerDefinition.create(mesh, 128, 64);
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void setupAnim(WoollyRhino entity, float limbSwing, float amount, float age, float headYaw, float headPitch) {
            walk(legs, limbSwing, amount, 0.6f);
            head.yRot = headYaw * Mth.DEG_TO_RAD * 0.5f;
            // Head down to charge.
            head.xRot = (entity.isAggressive() ? 0.8f : 0.45f) + headPitch * Mth.DEG_TO_RAD * 0.3f;
            tail.zRot = Mth.sin(age * 0.15f) * 0.3f;
        }
    }
}
