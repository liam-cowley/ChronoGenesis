package com.chronogenesis.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.chronogenesis.biocomputer.ChronocortexBlock;
import com.chronogenesis.biocomputer.ChronocortexBlockEntity;
import com.chronogenesis.biocomputer.CortexLobeBlock;
import com.chronogenesis.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Chronocortex as what it is becoming: not a jar and a cube of lobes but a living brain, a blob of folds that
 * breathes, tendrils reaching out of it, eyes opening round it. Its grown lobes (their blocks are not drawn, see
 * {@link CortexLobeBlock}) are drawn here as swelling masses of folds where they stand, tied to it by sinew.
 * <p>
 * The bigger it grows (stage, lobes) the larger and more tendrilled it is; the more it knows (lessons, eras
 * remembered, creatures learned) the stranger: its flesh goes from a grey-pink to bruise violet to an eldritch
 * green, more eyes open round it, a crown of shards turns over it, and at the last a maw opens in it.
 */
public class CortexBrainRenderer implements BlockEntityRenderer<ChronocortexBlockEntity> {
    private static final int PINK = 0xD8AEA8;
    private static final int VIOLET = 0x9A5AB4;
    private static final int ELDRITCH = 0x6AE09A;
    private static final int RESCAN = 40;

    private final Map<ChronocortexBlockEntity, Lobes> lobes = new WeakHashMap<>();

    private record Lobes(long scanned, List<BlockPos> positions, List<Boolean> eyes) {}

    public CortexBrainRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ChronocortexBlockEntity cortex, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Level level = cortex.getLevel();
        if (level == null) {
            return;
        }
        BlockState state = cortex.getBlockState();
        int stage = state.hasProperty(ChronocortexBlock.STAGE) ? state.getValue(ChronocortexBlock.STAGE) : 0;
        int knowledge = cortex.knowledge();
        float strange = Mth.clamp(knowledge / 16.0f, 0, 1);
        int flesh = knowledge < 8 ? OrganParts.mix(PINK, VIOLET, knowledge / 8.0f) : OrganParts.mix(VIOLET, ELDRITCH, (knowledge - 8) / 16.0f);
        float time = level.getGameTime() + partialTick;
        long seed = cortex.getBlockPos().asLong();
        VertexConsumer buffer = OrganParts.buffer(buffers);
        Vec3 centre = Vec3.atCenterOf(cortex.getBlockPos());
        Vec3 watcher = OrganParts.watcher(centre.add(0, 0, -4));
        float breath = 1 + 0.04f * Mth.sin(time * 0.05f);

        Lobes grown = lobes(cortex, level, (long) time);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);

        // The brain itself, rising out of the jar: more of it every stage.
        float crown = 12.0f + stage * 3.0f;
        int lumps = 3 + stage * 3 + Math.min(12, cortex.lobes() / 2);
        for (int i = 0; i < lumps; i++) {
            float a = i * 2.39996f + (seed & 7);
            float ring = Mth.sqrt(i / (float) lumps);
            float r = (3.0f + stage * 2.2f) * ring;
            float y = crown - 4 + Mth.cos(i * 1.7f) * (1.5f + stage);
            float s = (0.7f + 0.25f * stage) * (1.0f - 0.35f * ring) * (breath + 0.05f * Mth.sin(time * 0.08f + i));
            // The more it knows, the less its folds keep to a brain's shape.
            float wander = strange * 2.5f * Mth.sin(time * 0.02f + i * 0.9f);
            OrganParts.draw(OrganParts.GYRI, pose, buffer, light, Mth.cos(a) * r + wander, y, Mth.sin(a) * r - wander, s, flesh);
        }

        // Its grown lobes, drawn where they stand: swollen folds, sinews back to it, eyes in the cranium's faces.
        BlockPos origin = cortex.getBlockPos();
        for (int i = 0; i < grown.positions().size(); i++) {
            BlockPos lobe = grown.positions().get(i);
            float lx = (lobe.getX() - origin.getX()) * 16;
            float ly = (lobe.getY() - origin.getY()) * 16;
            float lz = (lobe.getZ() - origin.getZ()) * 16;
            long ls = lobe.asLong();
            for (int k = 0; k < 3; k++) {
                float a = k * 2.1f + (ls & 15);
                float s = (1.45f + 0.25f * ((ls >> k) & 1)) * (breath + 0.04f * Mth.sin(time * 0.06f + k + (ls & 7)));
                OrganParts.draw(OrganParts.GYRI, pose, buffer, light, lx + Mth.cos(a) * 2.5f, ly + Mth.sin(a * 1.3f) * 2.0f,
                        lz + Mth.sin(a) * 2.5f, s, flesh);
            }
            if (grown.eyes().get(i)) {
                Vec3 at = Vec3.atCenterOf(lobe);
                OrganParts.eye(pose, buffer, light, lx + Math.signum(lx) * 8.5f, ly + Math.signum(ly) * 8.5f, lz + Math.signum(lz) * 8.5f,
                        2.6f, at, watcher, time, ls, OrganParts.WHITE);
            }
        }

        // Tendrils out of the brain: more with every stage and every thing it knows, longer as it grows.
        int tendrils = Math.min(16, 2 + stage * 2 + knowledge / 2);
        int length = 3 + stage * 2;
        for (int i = 0; i < tendrils; i++) {
            float a = i * Mth.TWO_PI / tendrils + (seed & 3);
            pose.pushPose();
            pose.translate(Mth.cos(a) * (2.5f + stage * 1.5f) / 16.0f, (crown - 3) / 16.0f, Mth.sin(a) * (2.5f + stage * 1.5f) / 16.0f);
            pose.mulPose(Axis.YP.rotation(-a));
            pose.mulPose(Axis.ZP.rotation(i % 3 == 0 ? -1.1f : -2.0f - 0.4f * Mth.sin(i)));
            OrganParts.tentacle(pose, buffer, light, length, 0.9f + 0.1f * stage, time, i * 1.37f, 0.18f + 0.1f * strange, flesh);
            pose.popPose();
        }

        // Eyes open round it, one for every few things it knows, each watching.
        int eyes = Math.min(10, knowledge / 3);
        for (int i = 0; i < eyes; i++) {
            float a = time * 0.004f + i * Mth.TWO_PI / Math.max(1, eyes);
            float r = 8.0f + stage * 3.0f;
            float y = crown + 2 + Mth.sin(time * 0.03f + i * 1.3f) * 2.0f;
            OrganParts.eye(pose, buffer, light, Mth.cos(a) * r, y, Mth.sin(a) * r, 1.2f + 0.2f * stage, centre, watcher, time,
                    seed + i * 7919L, OrganParts.WHITE);
        }

        // A crown of shards turning over it, once it knows a great deal.
        if (knowledge >= 10) {
            int shards = 6 + (knowledge - 10) / 2;
            for (int i = 0; i < Math.min(14, shards); i++) {
                float a = time * 0.015f + i * Mth.TWO_PI / Math.min(14, shards);
                pose.pushPose();
                pose.translate(Mth.cos(a) * (6.0f + stage * 2) / 16.0f, (crown + 9 + stage * 2) / 16.0f, Mth.sin(a) * (6.0f + stage * 2) / 16.0f);
                pose.mulPose(Axis.YP.rotation(-a));
                pose.mulPose(Axis.XP.rotation(time * 0.05f + i));
                OrganParts.draw(OrganParts.SHARD, pose, buffer, light, 0, 0, 0, 0.7f, OrganParts.mix(0xFFFFFF, ELDRITCH, strange));
                pose.popPose();
            }
        }

        // And at the last, a maw opens in it.
        if (knowledge >= 16) {
            float open = 0.2f + 0.3f * (0.5f + 0.5f * Mth.sin(time * 0.04f));
            Vec3 d = watcher.subtract(centre);
            float yaw = (float) Mth.atan2(d.x, d.z);
            pose.pushPose();
            pose.translate(0, (crown - 1) / 16.0f, 0);
            pose.mulPose(Axis.YP.rotation(yaw));
            pose.translate(0, 0, (4.5f + stage * 2) / 16.0f);
            for (int side = -1; side <= 1; side += 2) {
                pose.pushPose();
                pose.mulPose(Axis.XP.rotation(side * open));
                OrganParts.draw(OrganParts.TEETH, pose, buffer, light, 0, side * 1.0f, 0, 1.2f + 0.3f * stage, OrganParts.WHITE);
                pose.popPose();
            }
            pose.popPose();
        }
        pose.popPose();
    }

    /** Its grown lobes (any but fresh), looked for now and then round it. */
    private Lobes lobes(ChronocortexBlockEntity cortex, Level level, long now) {
        Lobes cached = lobes.get(cortex);
        if (cached != null && now - cached.scanned() < RESCAN) {
            return cached;
        }
        List<BlockPos> found = new ArrayList<>();
        List<Boolean> eyes = new ArrayList<>();
        int reach = CortexLobeBlock.REACH;
        for (BlockPos pos : BlockPos.betweenClosed(cortex.getBlockPos().offset(-reach, -reach, -reach), cortex.getBlockPos().offset(reach, reach, reach))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.CORTEX_LOBE.get()) && CortexLobeBlock.drawnByCortex(state)) {
                found.add(pos.immutable());
                eyes.add(state.getValue(CortexLobeBlock.PART) == CortexLobeBlock.Part.CEREBRUM_EYE);
            }
        }
        Lobes scanned = new Lobes(now, found, eyes);
        lobes.put(cortex, scanned);
        return scanned;
    }

    @Override
    public boolean shouldRenderOffScreen(ChronocortexBlockEntity cortex) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(ChronocortexBlockEntity cortex) {
        return new AABB(cortex.getBlockPos()).inflate(CortexLobeBlock.REACH + 2);
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
