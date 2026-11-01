package com.chronogenesis.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.biocomputer.ChronocortexBlock;
import com.chronogenesis.biocomputer.ChronocortexBlockEntity;
import com.chronogenesis.biocomputer.CortexLobeBlock;
import com.chronogenesis.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Chronocortex as what it is becoming: a living brain grown in curved flesh rather than boxes ({@link CortexBody},
 * one form per stage, from a brain afloat in its jar to the Cerebrum's writhing mass), its eyes following whoever is
 * in its line of sight ({@link CortexGaze}). Its grown lobes (their blocks are not drawn, see {@link CortexLobeBlock})
 * are drawn here as swelling folds where they stand, tied to it by sinew.
 */
public class CortexBrainRenderer implements BlockEntityRenderer<ChronocortexBlockEntity> {
    public static final ResourceLocation TEXTURE = ChronoGenesis.id("textures/entity/cortex_body.png");
    private static final int RESCAN = 40;

    private final Map<ChronocortexBlockEntity, Lobes> lobes = new WeakHashMap<>();
    private final Map<ChronocortexBlockEntity, CortexGaze> gazes = new WeakHashMap<>();

    private record Lobes(long scanned, List<CortexBody.Lobe> found) {}

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
        float time = level.getGameTime() + partialTick;
        CortexGaze gaze = gazes.computeIfAbsent(cortex, c -> new CortexGaze());
        CortexBody.Look look = gaze.update(level, cortex.getBlockPos(), stage, time, partialTick);

        // Fewer facets far off.
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double distance = camera.distanceTo(Vec3.atCenterOf(cortex.getBlockPos()));
        float detail = (float) Mth.clampedMap(distance, 16, 56, 1.0, 0.4);

        CortexBody.State body = new CortexBody.State(stage, cortex.knowledge(), time, cortex.getBlockPos().asLong(),
                lobes(cortex, level, (long) time), look, detail);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.scale(1 / 16.0f, 1 / 16.0f, 1 / 16.0f);
        CortexBody.draw(body, sink(pose.last(), buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE))), light, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }

    /** A sink drawing into {@code buffer} through {@code pose}. */
    static FleshMesh.Sink sink(PoseStack.Pose pose, VertexConsumer buffer) {
        Matrix4f matrix = pose.pose();
        Matrix3f normals = pose.normal();
        Vector3f p = new Vector3f();
        Vector3f n = new Vector3f();
        return (x, y, z, color, u, v, light, nx, ny, nz) -> {
            matrix.transformPosition(x, y, z, p);
            normals.transform(nx, ny, nz, n).normalize();
            buffer.addVertex(p.x, p.y, p.z, color, u, v, OverlayTexture.NO_OVERLAY, light, n.x, n.y, n.z);
        };
    }

    /** Its grown lobes (any but fresh), in pixels from its middle, looked for now and then round it. */
    private List<CortexBody.Lobe> lobes(ChronocortexBlockEntity cortex, Level level, long now) {
        Lobes cached = lobes.get(cortex);
        if (cached != null && now - cached.scanned() < RESCAN && now >= cached.scanned()) {
            return cached.found();
        }
        List<CortexBody.Lobe> found = new ArrayList<>();
        int reach = CortexLobeBlock.REACH;
        BlockPos origin = cortex.getBlockPos();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-reach, -reach, -reach), origin.offset(reach, reach, reach))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.CORTEX_LOBE.get()) && CortexLobeBlock.drawnByCortex(state)) {
                found.add(new CortexBody.Lobe(new Vector3f(pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ()).mul(16),
                        state.getValue(CortexLobeBlock.PART) == CortexLobeBlock.Part.CEREBRUM_EYE));
            }
        }
        lobes.put(cortex, new Lobes(now, found));
        return found;
    }

    @Override
    public boolean shouldRenderOffScreen(ChronocortexBlockEntity cortex) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(ChronocortexBlockEntity cortex) {
        return new AABB(cortex.getBlockPos()).inflate(CortexLobeBlock.REACH + 8);
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
