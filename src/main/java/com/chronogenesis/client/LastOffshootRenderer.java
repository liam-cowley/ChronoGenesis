package com.chronogenesis.client;

import com.chronogenesis.eldritch.LastOffshootBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Last Offshoot as it wakes: the bud in its jar swells out of the glass into a brain, then into something that is
 * no longer one: folds heaped on folds, going from grey-pink to bruise violet to eldritch green, more tendrils and
 * longer, eyes opening in a ring, and at the last a maw. Asleep, it is only the bud (the block model).
 */
public class LastOffshootRenderer implements BlockEntityRenderer<LastOffshootBlockEntity> {
    private static final int PINK = 0xD8AEA8;
    private static final int VIOLET = 0x9A5AB4;
    private static final int ELDRITCH = 0x6AE09A;

    public LastOffshootRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(LastOffshootBlockEntity offshoot, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float g = offshoot.growth(partialTick);
        if (g <= 0 || offshoot.getLevel() == null) {
            return;
        }
        float time = offshoot.getLevel().getGameTime() + partialTick;
        long seed = offshoot.getBlockPos().asLong();
        VertexConsumer buffer = OrganParts.buffer(buffers);
        Vec3 centre = Vec3.atCenterOf(offshoot.getBlockPos());
        Vec3 watcher = OrganParts.watcher(centre.add(0, 0, -4));
        int flesh = g < 0.5f ? OrganParts.mix(PINK, VIOLET, g * 2) : OrganParts.mix(VIOLET, ELDRITCH, (g - 0.5f) * 2);
        // It throbs faster as it nears the end.
        float breath = 1 + (0.05f + 0.1f * g) * Mth.sin(time * (0.05f + 0.3f * g));
        float crown = 8 + 36 * g;
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        int lumps = 3 + (int) (34 * g);
        for (int i = 0; i < lumps; i++) {
            float a = i * 2.39996f + (seed & 7);
            float ring = Mth.sqrt(i / (float) lumps);
            float r = (2 + 30 * g) * ring;
            float y = crown - 4 + Mth.cos(i * 1.7f) * (1 + 10 * g);
            float s = (0.5f + 2.2f * g) * (1.0f - 0.3f * ring) * (breath + 0.05f * Mth.sin(time * 0.08f + i));
            float wander = g * 4 * Mth.sin(time * 0.03f + i * 0.9f);
            OrganParts.draw(OrganParts.GYRI, pose, buffer, light, Mth.cos(a) * r + wander, y, Mth.sin(a) * r - wander, s, flesh);
        }
        int tendrils = 2 + (int) (14 * g);
        for (int i = 0; i < tendrils; i++) {
            float a = i * Mth.TWO_PI / tendrils + (seed & 3);
            pose.pushPose();
            pose.translate(Mth.cos(a) * (2 + 20 * g) / 16.0f, (crown - 6) / 16.0f, Mth.sin(a) * (2 + 20 * g) / 16.0f);
            pose.mulPose(Axis.YP.rotation(-a));
            pose.mulPose(Axis.ZP.rotation(i % 3 == 0 ? -1.2f : -2.1f - 0.4f * Mth.sin(i)));
            OrganParts.tentacle(pose, buffer, light, 3 + (int) (9 * g), 0.8f + 2.0f * g, time, i * 1.37f, 0.2f + 0.2f * g, flesh);
            pose.popPose();
        }
        int eyes = (int) (16 * g);
        for (int i = 0; i < eyes; i++) {
            float a = time * 0.006f + i * Mth.TWO_PI / Math.max(1, eyes);
            float r = 6 + 26 * g;
            float y = crown + Mth.sin(time * 0.03f + i * 1.3f) * (2 + 6 * g);
            OrganParts.eye(pose, buffer, light, Mth.cos(a) * r, y, Mth.sin(a) * r, 1.0f + 2.5f * g, centre, watcher, time,
                    seed + i * 7919L, OrganParts.WHITE);
        }
        if (g > 0.75f) {
            float open = (g - 0.75f) * 4 * (0.4f + 0.3f * Mth.sin(time * 0.1f));
            Vec3 d = watcher.subtract(centre);
            pose.pushPose();
            pose.translate(0, crown / 16.0f, 0);
            pose.mulPose(Axis.YP.rotation((float) Mth.atan2(d.x, d.z)));
            pose.translate(0, 0, (6 + 24 * g) / 16.0f);
            for (int side = -1; side <= 1; side += 2) {
                pose.pushPose();
                pose.mulPose(Axis.XP.rotation(side * open));
                OrganParts.draw(OrganParts.TEETH, pose, buffer, light, 0, side * 2.0f, 0, 2.0f + 3.0f * g, OrganParts.WHITE);
                pose.popPose();
            }
            pose.popPose();
        }
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(LastOffshootBlockEntity offshoot) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(LastOffshootBlockEntity offshoot) {
        return new AABB(offshoot.getBlockPos()).inflate(4, 6, 4);
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
