package com.chronogenesis.client;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.organism.CorePhase;
import com.chronogenesis.organism.FleshBlock;
import com.chronogenesis.organism.OrganismCoreBlock;
import com.chronogenesis.organism.OrganismCoreBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The living heart of an organism, inside its brass frame (the frame itself is the block's model): a mass that beats,
 * an eye that follows whoever is near, and its essence's own growths, all moving.
 * <ul>
 *   <li>Density: a heavy lump strapped in iron, bulging between the straps, beating slow.</li>
 *   <li>Speed: drawn taut on tendons to the frame's corners, which thrum.</li>
 *   <li>Size: sacs that swell and sag out of step.</li>
 *   <li>Awareness: folded like a brain, eyes on stalks looking about, each on its own.</li>
 *   <li>Time: amethyst shards circling it, a brass hand going round.</li>
 * </ul>
 * By phase: a seed is a small bud; awake it fills the frame; hungry it shrinks and its heart races; necrotic it is
 * grey, slumped and still. Malformed (not in its essence's shape) it twitches, flushed.
 */
public class OrganismCoreRenderer implements BlockEntityRenderer<OrganismCoreBlockEntity> {
    private static final int NECROTIC = 0x6A6660;

    public OrganismCoreRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(OrganismCoreBlockEntity core, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (core.getLevel() == null) {
            return;
        }
        BlockState state = core.getBlockState();
        CorePhase phase = state.getValue(OrganismCoreBlock.PHASE);
        Aspect aspect = state.getValue(FleshBlock.ASPECT);
        boolean formed = state.getValue(OrganismCoreBlock.FORMED);
        long seed = core.getBlockPos().asLong();
        float time = core.getLevel().getGameTime() + partialTick;
        VertexConsumer buffer = OrganParts.buffer(buffers);
        Vec3 centre = Vec3.atCenterOf(core.getBlockPos());
        Vec3 watcher = OrganParts.watcher(centre.add(0, 0, -4));

        float size = switch (phase) {
            case SEED -> 0.45f;
            case AWAKE -> 1.0f;
            case HUNGRY -> 0.8f;
            case NECROTIC -> 0.85f;
        };
        float period = switch (phase) {
            case SEED -> 40;
            case AWAKE -> aspect == Aspect.DENSITY ? 44 : 30;
            case HUNGRY -> 13;
            case NECROTIC -> 1;
        };
        boolean dead = phase == CorePhase.NECROTIC;
        float pulse = dead ? 0 : OrganParts.beat(time + (seed & 31), period) * (phase == CorePhase.HUNGRY ? 0.09f : 0.06f);
        int flesh = dead ? OrganParts.tint(NECROTIC, 1) : OrganParts.tint(aspect.color(), 0.85f + pulse);
        if (!formed && !dead) {
            // Malformed: flushed, and it twitches.
            flesh = OrganParts.mix(flesh, 0xD02020, 0.35f + 0.25f * Mth.sin(time * 0.5f));
        }

        pose.pushPose();
        pose.translate(0.5, dead ? 0.35 : 0.5, 0.5);
        if (!formed && !dead && ((time + (seed & 15)) % 23) < 3) {
            pose.translate(Mth.sin(time * 3.1f) * 0.02f, 0, Mth.cos(time * 2.7f) * 0.02f);
        }
        pose.scale(size, dead ? size * 0.6f : size, size);
        float m = 1.5f * (1 + pulse);
        var mass = aspect == Aspect.AWARENESS ? OrganParts.GYRI : OrganParts.FLESH;
        OrganParts.draw(mass, pose, buffer, light, 0, 0, 0, m, flesh);
        // Lumps round the mass, swelling out of step.
        for (int i = 0; i < 4; i++) {
            float a = i * Mth.HALF_PI + (seed & 7);
            float swell = dead ? 0.55f : 0.55f + 0.08f * Mth.sin(time * 0.07f + i * 1.9f);
            OrganParts.draw(mass, pose, buffer, light, Mth.cos(a) * 4.5f, (i % 2 == 0 ? 2.5f : -2.5f), Mth.sin(a) * 4.5f, swell, flesh);
        }
        if (!dead) {
            growths(aspect, pose, buffer, light, time, seed, flesh, centre, watcher);
            // The eye, on the side towards whoever watches.
            Vec3 d = watcher.subtract(centre);
            float yaw = (float) Mth.atan2(d.x, d.z);
            float ex = Mth.sin(yaw) * 6.2f;
            float ez = Mth.cos(yaw) * 6.2f;
            int eye = phase == CorePhase.HUNGRY ? 0xFFFFB0B0 : OrganParts.WHITE;
            OrganParts.eye(pose, buffer, light, ex, 0.5f, ez, phase == CorePhase.SEED ? 0.8f : 1.1f, centre, watcher, time, seed, eye);
        } else {
            // Stitched shut: a brass staple across where its eye was.
            OrganParts.draw(OrganParts.BRASS, pose, buffer, light, 0, 1, -6.2f, 0.5f, 0.2f, 1.6f, OrganParts.WHITE);
        }
        pose.popPose();
    }

    private static void growths(Aspect aspect, PoseStack pose, VertexConsumer buffer, int light, float time, long seed, int flesh,
            Vec3 centre, Vec3 watcher) {
        switch (aspect) {
            case DENSITY -> {
                for (float y : new float[] {-3.5f, 3.5f}) {
                    OrganParts.draw(OrganParts.STRAP, pose, buffer, light, 0, y, 0, 1.3f, 1.0f, 1.3f, OrganParts.WHITE);
                }
                OrganParts.draw(OrganParts.FLESH, pose, buffer, light, 0, 0, 0, 1.65f, 0.55f, 1.65f, flesh);
            }
            case SPEED -> {
                // Tendons to the frame's corners, thrumming.
                for (int i = 0; i < 4; i++) {
                    float a = Mth.HALF_PI * i + Mth.PI / 4;
                    pose.pushPose();
                    pose.mulPose(Axis.YP.rotation(-a + Mth.HALF_PI));
                    pose.mulPose(Axis.XP.rotation((i % 2 == 0 ? 0.6f : -0.6f) + Mth.sin(time * 1.7f + i) * 0.03f));
                    OrganParts.draw(OrganParts.TENDON, pose, buffer, light, 0, 0, 7.5f, 1.2f, 1.2f, 1.1f, flesh);
                    pose.popPose();
                }
            }
            case SIZE -> {
                float[][] at = {{0, 6, 0}, {6, -1, 2}, {-5, 1, -4}};
                for (int i = 0; i < at.length; i++) {
                    float swell = 0.65f + 0.3f * (0.5f + 0.5f * Mth.sin(time * 0.05f + i * 2.1f));
                    OrganParts.draw(OrganParts.SAC, pose, buffer, light, at[i][0], at[i][1], at[i][2], swell, flesh);
                }
            }
            case AWARENESS -> {
                for (int i = 0; i < 3; i++) {
                    float a = i * Mth.TWO_PI / 3 + time * 0.01f;
                    float sx = Mth.cos(a) * 5;
                    float sz = Mth.sin(a) * 5;
                    float h = 6 + Mth.sin(time * 0.05f + i) * 1.2f;
                    pose.pushPose();
                    pose.translate(sx / 16.0f, 2.0f / 16.0f, sz / 16.0f);
                    OrganParts.tentacle(pose, buffer, light, 2, 0.6f, time, i * 2.0f, 0.15f, flesh);
                    pose.popPose();
                    // Each eye looks about a little behind the watcher, on its own.
                    Vec3 lag = watcher.add(Mth.sin(time * 0.03f + i * 3) * 1.5, Mth.cos(time * 0.02f + i) * 0.8, 0);
                    OrganParts.eye(pose, buffer, light, sx, h, sz, 0.7f, centre, lag, time, seed + i * 977L, OrganParts.WHITE);
                }
            }
            case TIME -> {
                for (int i = 0; i < 4; i++) {
                    float a = time * 0.02f + i * Mth.HALF_PI;
                    pose.pushPose();
                    pose.translate(Mth.cos(a) * 7.0f / 16.0f, Mth.sin(time * 0.04f + i) * 1.5f / 16.0f, Mth.sin(a) * 7.0f / 16.0f);
                    pose.mulPose(Axis.YP.rotation(-a));
                    pose.mulPose(Axis.ZP.rotation(0.3f));
                    OrganParts.draw(OrganParts.SHARD, pose, buffer, light, 0, 0, 0, 0.8f, OrganParts.WHITE);
                    pose.popPose();
                }
                // A clock hand, going round on its face.
                pose.pushPose();
                pose.translate(0, 0, -6.2f / 16.0f);
                pose.mulPose(Axis.ZP.rotation(time * 0.05f));
                OrganParts.draw(OrganParts.BRASS, pose, buffer, light, 0, 2.2f, 0, 0.35f, 0.75f, 0.2f, OrganParts.WHITE);
                pose.popPose();
            }
        }
    }
}
