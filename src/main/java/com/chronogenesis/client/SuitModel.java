package com.chronogenesis.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * The Chrono-Insulated Suit as worn, one model per piece (each only adds to the parts its slot shows):
 * <ul>
 *   <li><b>Goggles:</b> a leather aviator cap with a strap, two brass-rimmed smoked lenses standing out on barrels,
 *       a pressure canister at the temple and a copper aerial with a glass bulb at the back.</li>
 *   <li><b>Lab Coat:</b> coat and sleeves with an upturned collar and tails, brass pauldrons and cuffs, a Time
 *       canister on the back with a hose over the shoulder, a pressure gauge on the chest, a vial in the breast pocket.</li>
 *   <li><b>Trousers:</b> oiled leather, a buckled belt, copper knee pads, a tool pouch on the thigh.</li>
 *   <li><b>Galoshes:</b> rubber boots with thick soles, brass toe caps and a buckled strap.</li>
 * </ul>
 * A <b>Time-warded</b> piece grows tendrils from its seams (like the Essence Manipulator), which writhe and now and
 * then draw back in; the coat's canister glows. Texture: {@code textures/models/armor/chrono_suit.png} (128x128,
 * painted by {@code tools/textures.py} for these boxes).
 */
public final class SuitModel extends HumanoidModel<LivingEntity> {
    private static final int SEGMENTS = 4;

    /** A tendril: its chain of parts (the first is the root), the way it grows out, and its phase. */
    private record Tendril(List<ModelPart> segments, float baseX, float baseZ, float phase) {}

    /** Where a tendril grows out of a piece: parent part, root position, base tilt, phase. */
    private record Spot(String parent, String name, float x, float y, float z, float xRot, float zRot, float phase) {}

    private final List<Tendril> tendrils = new ArrayList<>();
    /** Parts shown only when warded (the glowing canister), and the ones they replace. */
    private final List<ModelPart> wardedParts = new ArrayList<>();
    private final List<ModelPart> plainParts = new ArrayList<>();

    public SuitModel(EquipmentSlot slot) {
        super(createLayer(slot).bakeRoot());
        for (Spot spot : spots(slot)) {
            ModelPart part = part(spot.parent).getChild(spot.name);
            List<ModelPart> segments = new ArrayList<>();
            segments.add(part);
            for (int s = 1; s < SEGMENTS; s++) {
                part = part.getChild("segment");
                segments.add(part);
            }
            tendrils.add(new Tendril(segments, spot.xRot, spot.zRot, spot.phase));
        }
        if (slot == EquipmentSlot.CHEST) {
            wardedParts.add(body.getChild("canister_charged"));
            plainParts.add(body.getChild("canister"));
        }
    }

    private ModelPart part(String name) {
        return switch (name) {
            case "head" -> head;
            case "body" -> body;
            case "right_arm" -> rightArm;
            case "left_arm" -> leftArm;
            case "right_leg" -> rightLeg;
            default -> leftLeg;
        };
    }

    private static List<Spot> spots(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> List.of(
                    new Spot("head", "tendril_0", -1.5f, -6.5f, 4.6f, -1.2f, -0.2f, 0.3f),      // the cap's back seam
                    new Spot("head", "tendril_1", 1.8f, -7.6f, 4.4f, -0.9f, 0.35f, 2.1f),
                    new Spot("head", "tendril_2", -5.8f, -6.0f, 0.0f, 0.0f, -1.3f, 4.0f));    // the canister's valve
            case CHEST -> List.of(
                    new Spot("body", "tendril_0", -1.0f, 0.8f, 5.0f, -0.5f, -0.35f, 0.0f),      // out of the canister
                    new Spot("body", "tendril_1", 1.0f, 0.8f, 5.4f, -0.7f, 0.4f, 1.9f),
                    new Spot("body", "tendril_2", -3.0f, 19.5f, 2.6f, -2.6f, -0.2f, 3.3f),     // the coat's tails
                    new Spot("body", "tendril_3", 3.0f, 19.5f, 2.6f, -2.6f, 0.2f, 5.0f),
                    new Spot("right_arm", "tendril_0", -4.3f, -3.0f, 0.0f, 0.0f, -1.2f, 0.9f),  // the pauldrons' seams
                    new Spot("left_arm", "tendril_0", 4.3f, -3.0f, 0.0f, 0.0f, 1.2f, 2.7f),
                    new Spot("right_arm", "tendril_1", -2.0f, 10.0f, 2.8f, -2.0f, -0.5f, 4.4f)); // the cuff
            case LEGS -> List.of(
                    new Spot("right_leg", "tendril_0", -1.0f, 5.5f, -3.2f, 1.3f, -0.4f, 0.6f),  // the knee pads
                    new Spot("left_leg", "tendril_0", 1.0f, 5.5f, -3.2f, 1.3f, 0.4f, 3.2f),
                    new Spot("body", "tendril_0", 3.6f, 10.5f, 0.0f, 0.2f, 1.4f, 5.1f));       // under the belt
            case FEET -> List.of(
                    new Spot("right_leg", "tendril_0", -2.6f, 5.0f, 0.5f, -0.3f, -0.9f, 1.2f),  // the boots' tops
                    new Spot("left_leg", "tendril_0", 2.6f, 5.0f, 0.5f, -0.3f, 0.9f, 4.1f));
            default -> List.of();
        };
    }

    public static LayerDefinition createLayer(EquipmentSlot slot) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5, 2, 0));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5, 2, 0));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9f, 12, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9f, 12, 0));
        switch (slot) {
            case HEAD -> goggles(head);
            case CHEST -> coat(body, rightArm, leftArm);
            case LEGS -> trousers(body, rightLeg, leftLeg);
            case FEET -> galoshes(rightLeg, leftLeg);
            default -> { }
        }
        for (Spot spot : spots(slot)) {
            PartDefinition parent = switch (spot.parent) {
                case "head" -> head;
                case "body" -> body;
                case "right_arm" -> rightArm;
                case "left_arm" -> leftArm;
                case "right_leg" -> rightLeg;
                default -> leftLeg;
            };
            PartDefinition part = parent.addOrReplaceChild(spot.name, segment(0), PartPose.offset(spot.x, spot.y, spot.z));
            for (int s = 1; s < SEGMENTS; s++) {
                part = part.addOrReplaceChild("segment", segment(s), PartPose.offset(0, -1.8f, 0));
            }
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static CubeListBuilder box(int u, int v, float x, float y, float z, float w, float h, float d, float grow) {
        return CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, w, h, d, new CubeDeformation(grow));
    }

    private static CubeListBuilder mirrored(int u, int v, float x, float y, float z, float w, float h, float d, float grow) {
        return CubeListBuilder.create().texOffs(u, v).mirror().addBox(x, y, z, w, h, d, new CubeDeformation(grow));
    }

    private static void goggles(PartDefinition head) {
        head.addOrReplaceChild("cap", box(0, 0, -4, -8, -4, 8, 8, 8, 0.45f), PartPose.ZERO);
        head.addOrReplaceChild("strap", box(32, 0, -4, -5.5f, -4, 8, 2, 8, 0.75f), PartPose.ZERO);
        head.addOrReplaceChild("barrel_right", box(64, 0, -3.5f, -6, -6.2f, 3, 3, 2, 0), PartPose.ZERO);
        head.addOrReplaceChild("barrel_left", mirrored(64, 0, 0.5f, -6, -6.2f, 3, 3, 2, 0), PartPose.ZERO);
        head.addOrReplaceChild("lens_right", box(74, 0, -3.75f, -6.25f, -6.8f, 3.5f, 3.5f, 1, 0), PartPose.ZERO);
        head.addOrReplaceChild("lens_left", mirrored(74, 0, 0.25f, -6.25f, -6.8f, 3.5f, 3.5f, 1, 0), PartPose.ZERO);
        head.addOrReplaceChild("bridge", box(56, 48, -0.5f, -5.2f, -5.6f, 1, 1, 1, 0), PartPose.ZERO);
        head.addOrReplaceChild("canister", box(84, 0, -5.8f, -7, -1.5f, 2, 2, 3, 0), PartPose.ZERO);
        head.addOrReplaceChild("aerial", box(94, 0, -0.5f, -12, 1.5f, 1, 4, 1, 0), PartPose.ZERO);
        head.addOrReplaceChild("bulb", box(98, 0, -1, -14, 1, 2, 2, 2, 0), PartPose.ZERO);
    }

    private static void coat(PartDefinition body, PartDefinition rightArm, PartDefinition leftArm) {
        body.addOrReplaceChild("coat", box(0, 16, -4, 0, -2, 8, 12, 4, 1.0f), PartPose.ZERO);
        body.addOrReplaceChild("tails", box(24, 16, -4, 12, 1.6f, 8, 7, 1, 0.9f), PartPose.ZERO);
        body.addOrReplaceChild("collar", box(42, 16, -4.5f, -1, -2.5f, 9, 2, 5, 0.9f), PartPose.ZERO);
        body.addOrReplaceChild("canister", box(0, 32, -2, 1, 3, 4, 6, 3, 0), PartPose.ZERO);
        body.addOrReplaceChild("canister_charged", box(14, 32, -2, 1, 3, 4, 6, 3, 0), PartPose.ZERO);
        body.addOrReplaceChild("caps", CubeListBuilder.create().texOffs(28, 32).addBox(-2.5f, 0.2f, 2.6f, 5, 1, 4)
                .texOffs(28, 32).addBox(-2.5f, 6.8f, 2.6f, 5, 1, 4), PartPose.ZERO);
        body.addOrReplaceChild("hose", CubeListBuilder.create().texOffs(46, 32).addBox(-3, -2.1f, -3, 1, 1, 7)
                .texOffs(62, 32).addBox(-3, -2.1f, 3.5f, 1, 3.6f, 1), PartPose.ZERO);
        body.addOrReplaceChild("pocket", box(70, 32, 1, 6, -3.3f, 3, 3, 1, 0), PartPose.ZERO);
        body.addOrReplaceChild("vial", box(66, 32, 2, 4.5f, -3.1f, 1, 2.5f, 1, 0), PartPose.ZERO);
        body.addOrReplaceChild("gauge", box(78, 32, -3.5f, 2.5f, -3.6f, 2, 2, 1, 0), PartPose.ZERO);
        rightArm.addOrReplaceChild("sleeve", box(0, 48, -3, -2, -2, 4, 12, 4, 1.0f), PartPose.ZERO);
        rightArm.addOrReplaceChild("pauldron", box(16, 48, -3.5f, -3.5f, -2.5f, 5, 2, 5, 0.85f), PartPose.ZERO);
        rightArm.addOrReplaceChild("cuff", box(36, 48, -3.5f, 8, -2.5f, 5, 1.5f, 5, 0.85f), PartPose.ZERO);
        leftArm.addOrReplaceChild("sleeve", mirrored(0, 48, -1, -2, -2, 4, 12, 4, 1.0f), PartPose.ZERO);
        leftArm.addOrReplaceChild("pauldron", mirrored(16, 48, -1.5f, -3.5f, -2.5f, 5, 2, 5, 0.85f), PartPose.ZERO);
        leftArm.addOrReplaceChild("cuff", mirrored(36, 48, -1.5f, 8, -2.5f, 5, 1.5f, 5, 0.85f), PartPose.ZERO);
    }

    private static void trousers(PartDefinition body, PartDefinition rightLeg, PartDefinition leftLeg) {
        body.addOrReplaceChild("belt", box(16, 64, -4, 10, -2, 8, 2, 4, 0.6f), PartPose.ZERO);
        body.addOrReplaceChild("buckle", box(40, 64, -1, 9.8f, -2.9f, 2, 2, 1, 0.1f), PartPose.ZERO);
        rightLeg.addOrReplaceChild("leg", box(0, 64, -2, 0, -2, 4, 12, 4, 0.5f), PartPose.ZERO);
        rightLeg.addOrReplaceChild("knee", box(46, 64, -2, 4, -2.9f, 4, 3, 1, 0.3f), PartPose.ZERO);
        rightLeg.addOrReplaceChild("pouch", box(56, 64, -3.5f, 1, -1.5f, 2, 3, 3, 0), PartPose.ZERO);
        leftLeg.addOrReplaceChild("leg", mirrored(0, 64, -2, 0, -2, 4, 12, 4, 0.5f), PartPose.ZERO);
        leftLeg.addOrReplaceChild("knee", mirrored(46, 64, -2, 4, -2.9f, 4, 3, 1, 0.3f), PartPose.ZERO);
    }

    private static void galoshes(PartDefinition rightLeg, PartDefinition leftLeg) {
        rightLeg.addOrReplaceChild("boot", box(0, 80, -2, 5, -2, 4, 7, 4, 0.9f), PartPose.ZERO);
        leftLeg.addOrReplaceChild("boot", mirrored(0, 80, -2, 5, -2, 4, 7, 4, 0.9f), PartPose.ZERO);
        for (PartDefinition leg : List.of(rightLeg, leftLeg)) {
            leg.addOrReplaceChild("sole", box(16, 80, -2.5f, 10.6f, -3.4f, 5, 2, 6, 0.35f), PartPose.ZERO);
            leg.addOrReplaceChild("toe", box(38, 80, -2, 9, -3.5f, 4, 1.5f, 2, 0.25f), PartPose.ZERO);
            leg.addOrReplaceChild("strap", box(50, 80, -2.5f, 6.5f, -2.5f, 5, 1, 5, 0.6f), PartPose.ZERO);
        }
    }

    /** One link of a tendril, thinner towards the tip; the last is the green tip. */
    private static CubeListBuilder segment(int index) {
        float thick = 1.0f - 0.2f * index;
        return CubeListBuilder.create().texOffs(index == SEGMENTS - 1 ? 8 : 0, 96)
                .addBox(-thick / 2, -2, -thick / 2, thick, 2, thick);
    }

    /** Shows the tendrils (and the glowing canister) if warded, and poses them for a moment in time (ticks). */
    public void animate(float time, boolean warded) {
        for (ModelPart part : wardedParts) {
            part.visible = warded;
        }
        for (ModelPart part : plainParts) {
            part.visible = !warded;
        }
        for (Tendril tendril : tendrils) {
            ModelPart root = tendril.segments.get(0);
            root.visible = warded;
            if (!warded) {
                continue;
            }
            float phase = tendril.phase;
            // Now and then they draw back into their seams.
            float reach = 0.3f + 0.7f * Mth.clamp(0.5f + 0.9f * Mth.sin(time * 0.04f + phase), 0.0f, 1.0f);
            root.xScale = reach;
            root.yScale = reach;
            root.zScale = reach;
            for (int s = 0; s < tendril.segments.size(); s++) {
                ModelPart part = tendril.segments.get(s);
                float wave = 0.3f + 0.09f * s;
                part.xRot = (s == 0 ? tendril.baseX : 0) + Mth.sin(time * 0.15f + phase + s * 0.9f) * wave;
                part.zRot = (s == 0 ? tendril.baseZ : 0) + Mth.cos(time * 0.11f + phase * 1.3f + s * 0.8f) * wave;
            }
        }
    }
}
