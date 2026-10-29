package com.chronogenesis.client;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.organism.FleshBlock;
import com.chronogenesis.organism.GraftBlock;
import com.chronogenesis.organism.GraftBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The moving parts of the grafts, on top of their models: what each one does, shown doing it. Drawn in the graft's
 * own frame, up being away from the flesh it grows from.
 * <ul>
 *   <li>Tap: its plunger works (fast while it pours).</li>
 *   <li>Maw: a jaw of teeth opening and closing.</li>
 *   <li>Leech Tendril: a tentacle writhing (gulping while it drinks).</li>
 *   <li>Enhancing Gland and Stasis Bladder: a sac throbbing; swelling slowly.</li>
 *   <li>Starting Gate and Finish: a brass flag that ticks round.</li>
 *   <li>Heart: beating. Eye: following whoever is near, blinking.</li>
 *   <li>Clock: its hands going round. Ganglion: nerves twitching. Temporal Field: rings of shards turning against
 *       each other.</li>
 * </ul>
 */
public class GraftRenderer implements BlockEntityRenderer<GraftBlockEntity> {
    public GraftRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(GraftBlockEntity graft, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (graft.getLevel() == null) {
            return;
        }
        BlockState state = graft.getBlockState();
        if (!state.hasProperty(GraftBlock.FACING)) {
            return;
        }
        Direction facing = state.getValue(GraftBlock.FACING);
        boolean active = state.getValue(GraftBlock.ACTIVE);
        BlockState support = graft.getLevel().getBlockState(GraftBlock.supportPos(state, graft.getBlockPos()));
        Aspect aspect = support.hasProperty(FleshBlock.ASPECT) ? support.getValue(FleshBlock.ASPECT) : Aspect.DENSITY;
        int flesh = OrganParts.tint(aspect.color(), 0.9f);
        float time = graft.getLevel().getGameTime() + partialTick;
        long seed = graft.getBlockPos().asLong();
        VertexConsumer buffer = OrganParts.buffer(buffers);
        Vec3 centre = Vec3.atCenterOf(graft.getBlockPos());

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(facing.getRotation());
        pose.translate(0, -0.5, 0); // origin on the flesh's face, up is outwards
        switch (graft.type()) {
            case TAP -> {
                float stroke = Mth.sin(time * (active ? 0.6f : 0.08f)) * (active ? 1.5f : 0.4f);
                OrganParts.draw(OrganParts.BRASS, pose, buffer, light, 0, 10.5f + stroke, 0, 0.6f, 0.5f, 0.6f, OrganParts.WHITE);
                OrganParts.draw(OrganParts.SAC, pose, buffer, light, 0, 4.0f, 0, 0.55f, 0.5f + 0.1f * Math.max(0, stroke), 0.55f, flesh);
            }
            case MAW -> {
                float open = 0.25f + 0.25f * (0.5f + 0.5f * Mth.sin(time * (active ? 0.4f : 0.07f)));
                for (int side = -1; side <= 1; side += 2) {
                    pose.pushPose();
                    pose.translate(0, 9.0f / 16.0f, side * 1.2f / 16.0f);
                    pose.mulPose(Axis.XP.rotation(side * open));
                    OrganParts.draw(OrganParts.TEETH, pose, buffer, light, 0, 1.0f, 0, 0.8f, OrganParts.WHITE);
                    pose.popPose();
                }
            }
            case LEECH_TENDRIL -> {
                pose.pushPose();
                pose.translate(0, 3.0f / 16.0f, 0);
                OrganParts.tentacle(pose, buffer, light, 5, active ? 1.15f : 0.9f, time * (active ? 2.5f : 1.0f), seed & 15, 0.28f, flesh);
                pose.popPose();
            }
            case ENHANCING_GLAND -> {
                float throb = 0.75f + 0.12f * OrganParts.beat(time + (seed & 31), 26);
                OrganParts.draw(OrganParts.SAC, pose, buffer, light, 0, 6.0f, 0, throb, flesh);
                if (state.hasProperty(com.chronogenesis.organism.GlandBlock.PUMP) && state.getValue(com.chronogenesis.organism.GlandBlock.PUMP)) {
                    // The Unbegotten's heart, strapped to its side in brass, beating slow and hard; an eye open in it.
                    float beat = 0.55f + 0.2f * OrganParts.beat(time, 34);
                    OrganParts.draw(OrganParts.FLESH, pose, buffer, light, 5.0f, 5.0f, 0, beat, OrganParts.tint(0x5A1E66, 1.0f));
                    OrganParts.draw(OrganParts.STRAP, pose, buffer, light, 5.0f, 5.0f, 0, 0.5f, 0.6f, 0.5f, OrganParts.WHITE);
                    OrganParts.draw(OrganParts.EYE, pose, buffer, light, 7.4f, 5.0f, 0, 0.5f, OrganParts.tint(0x9CFF80, 1.0f));
                }
            }
            case STASIS_BLADDER -> {
                float swell = 0.8f + 0.12f * Mth.sin(time * 0.03f + (seed & 7));
                OrganParts.draw(OrganParts.SAC, pose, buffer, light, 0, 5.5f, 0, swell, swell * 1.1f, swell, OrganParts.mix(flesh, 0xC0E8FF, 0.5f));
            }
            case STARTING_GATE, FINISH -> {
                pose.pushPose();
                pose.translate(0, 8.0f / 16.0f, 0);
                pose.mulPose(Axis.YP.rotation(time * (active ? 0.3f : 0.03f)));
                OrganParts.draw(OrganParts.BRASS, pose, buffer, light, 0, 2.0f, 0, 0.3f, 0.6f, 0.3f, OrganParts.WHITE);
                OrganParts.draw(OrganParts.SAC, pose, buffer, light, 2.0f, 3.5f, 0, 0.5f, 0.3f, 0.08f, flesh);
                pose.popPose();
            }
            case HEART -> {
                float beat = 0.7f + 0.18f * OrganParts.beat(time + (seed & 31), 18);
                OrganParts.draw(OrganParts.FLESH, pose, buffer, light, 0, 6.0f, 0, beat, OrganParts.tint(0xA01828, 1.0f));
            }
            case EYE -> OrganParts.eye(pose, buffer, light, 0, 8.5f, 0, 1.4f, centre, OrganParts.watcher(centre.add(0, 0, -4)), time, seed,
                    OrganParts.WHITE);
            case CLOCK -> {
                pose.pushPose();
                pose.translate(0, 9.5f / 16.0f, 0);
                pose.mulPose(Axis.XP.rotation(Mth.HALF_PI));
                for (int hand = 0; hand < 2; hand++) {
                    pose.pushPose();
                    pose.mulPose(Axis.ZP.rotation(time * (hand == 0 ? 0.1f : 0.0083f)));
                    OrganParts.draw(OrganParts.BRASS, pose, buffer, light, 0, hand == 0 ? 1.6f : 1.1f, 0, 0.25f, hand == 0 ? 0.55f : 0.4f, 0.2f, OrganParts.WHITE);
                    pose.popPose();
                }
                pose.popPose();
            }
            case GANGLION -> {
                for (int i = 0; i < 3; i++) {
                    pose.pushPose();
                    pose.mulPose(Axis.YP.rotation(i * Mth.TWO_PI / 3));
                    pose.translate(0, 4.0f / 16.0f, 2.0f / 16.0f);
                    pose.mulPose(Axis.XP.rotation(0.5f + (((time + i * 7 + (seed & 15)) % 31) < 2 ? 0.3f : 0)));
                    OrganParts.tentacle(pose, buffer, light, 2, 0.5f, time, i * 2.2f, 0.1f, flesh);
                    pose.popPose();
                }
            }
            case TEMPORAL_FIELD -> {
                for (int ring = 0; ring < 2; ring++) {
                    float spin = time * (ring == 0 ? 0.03f : -0.045f);
                    for (int i = 0; i < 6; i++) {
                        float a = spin + i * Mth.TWO_PI / 6;
                        float r = ring == 0 ? 6.0f : 4.0f;
                        pose.pushPose();
                        pose.translate(Mth.cos(a) * r / 16.0f, (ring == 0 ? 8.0f : 11.0f) / 16.0f, Mth.sin(a) * r / 16.0f);
                        pose.mulPose(Axis.YP.rotation(-a));
                        OrganParts.draw(OrganParts.SHARD, pose, buffer, light, 0, 0, 0, 0.45f, OrganParts.WHITE);
                        pose.popPose();
                    }
                }
            }
        }
        pose.popPose();
    }
}
