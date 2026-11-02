package com.chronogenesis.client;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.biocomputer.ChronocortexBlock;
import com.chronogenesis.biocomputer.ChronocortexBlockEntity;
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
import net.minecraft.core.Direction;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/**
 * The Chronocortex as what it has become ({@link CortexBody}): its stage's model (the brain in its jar, then the body its
 * whole shells have merged into: their lobes and capsules are not drawn as blocks then), its eyes following whoever is
 * in its line of sight ({@link CortexGaze}). The models are {@code assets/chronogenesis/geo/<name>.geo.json} with
 * {@code textures/entity/<name>.png}, read once and again on a resource reload.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID, value = Dist.CLIENT)
public class CortexBrainRenderer implements BlockEntityRenderer<ChronocortexBlockEntity> {
    private static final int RESCAN = 40;
    private static final Map<String, Optional<GeoModel>> MODELS = new HashMap<>();

    private final Map<ChronocortexBlockEntity, Capsules> capsules = new WeakHashMap<>();
    private final Map<ChronocortexBlockEntity, CortexGaze> gazes = new WeakHashMap<>();

    private record Capsules(long scanned, int stage, List<int[]> found) {}

    public CortexBrainRenderer(BlockEntityRendererProvider.Context context) {
    }

    @SubscribeEvent
    static void registerReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> MODELS.clear());
    }

    /** A model by name, read from its .geo.json the first time it is wanted (null if it is missing or broken). */
    static GeoModel model(String name) {
        return MODELS.computeIfAbsent(name, key -> {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(ChronoGenesis.id("geo/" + key + ".geo.json"));
            if (resource.isEmpty()) {
                ChronoGenesis.LOGGER.warn("Missing model geo/{}.geo.json", key);
                return Optional.empty();
            }
            try (Reader reader = resource.get().openAsReader()) {
                return Optional.of(GeoModel.read(reader));
            } catch (IOException | RuntimeException e) {
                ChronoGenesis.LOGGER.warn("Could not read model geo/{}.geo.json", key, e);
                return Optional.empty();
            }
        }).orElse(null);
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
        Direction path = cortex.path();
        CortexBody.State body = new CortexBody.State(stage, cortex.knowledge(), time, cortex.getBlockPos().asLong(),
                capsules(cortex, level, stage, (long) time), path == null ? null : new int[] {path.getStepX(), path.getStepZ()}, look);
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.scale(1 / 16.0f, 1 / 16.0f, 1 / 16.0f);
        PoseStack.Pose last = pose.last();
        CortexBody.draw(body, CortexBrainRenderer::model,
                name -> sink(last, buffers.getBuffer(RenderType.entityCutoutNoCull(ChronoGenesis.id("textures/entity/" + name + ".png")))),
                light, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }

    /** A sink drawing into {@code buffer} through {@code pose}. */
    static BoxModel.Sink sink(PoseStack.Pose pose, VertexConsumer buffer) {
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

    /** The Time Capsules built into its last shell (block offsets from the jar), looked for now and then. */
    private List<int[]> capsules(ChronocortexBlockEntity cortex, Level level, int stage, long now) {
        if (stage < ChronocortexBlockEntity.MAX_STAGE) {
            return List.of();
        }
        Capsules cached = capsules.get(cortex);
        if (cached != null && cached.stage() == stage && now - cached.scanned() < RESCAN && now >= cached.scanned()) {
            return cached.found();
        }
        List<int[]> found = new ArrayList<>();
        BlockPos jar = cortex.getBlockPos();
        for (BlockPos pos : ChronocortexBlockEntity.structure(jar, stage)) {
            if (ChronocortexBlockEntity.inShell(jar, pos, stage) && level.getBlockState(pos).is(ModBlocks.TIME_CAPSULE.get())) {
                found.add(new int[] {pos.getX() - jar.getX(), pos.getY() - jar.getY(), pos.getZ() - jar.getZ()});
            }
        }
        capsules.put(cortex, new Capsules(now, stage, found));
        return found;
    }

    @Override
    public boolean shouldRenderOffScreen(ChronocortexBlockEntity cortex) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(ChronocortexBlockEntity cortex) {
        return new AABB(cortex.getBlockPos()).inflate(8);
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
