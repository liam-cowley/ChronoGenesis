package com.chronogenesis.client;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * The past as world geometry: one mesh per section of the past the client has ({@link PastSections}),
 * tessellated through a {@link PastWorld} exactly as the game builds a chunk section (per render layer, with
 * culling, smooth lighting and fluids), kept on the GPU, and drawn with the game's own chunk shaders.
 * A few sections are built per frame, so the past comes into focus rather than stalling the game.
 */
public final class PastMesh {
    private static final Map<Long, Map<RenderType, VertexBuffer>> MESHES = new HashMap<>();

    private PastMesh() {}

    /** Builds the meshes of up to {@code count} sections that are new or changed. */
    public static void buildSome(PastWorld world, int count) {
        for (long pos : PastSections.takeDirty(count)) {
            forget(pos);
            if (PastSections.get(pos) != null) {
                MESHES.put(pos, build(world, pos));
            }
        }
    }

    private static Map<RenderType, VertexBuffer> build(PastWorld world, long section) {
        SectionPos sectionPos = SectionPos.of(section);
        BlockPos origin = sectionPos.origin();
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        Map<RenderType, ByteBufferBuilder> memory = new IdentityHashMap<>();
        Map<RenderType, BufferBuilder> builders = new IdentityHashMap<>();
        RandomSource random = RandomSource.create();
        PoseStack pose = new PoseStack();
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(15, 15, 15))) {
            BlockState state = world.getBlockState(pos);
            FluidState fluid = state.getFluidState();
            if (!fluid.isEmpty()) {
                // Liquids are written relative to their chunk section: exactly this mesh's origin.
                dispatcher.renderLiquid(pos, world, builder(ItemBlockRenderTypes.getRenderLayer(fluid), memory, builders), state, fluid);
            }
            if (state.getRenderShape() != RenderShape.MODEL) {
                continue;
            }
            BakedModel model = dispatcher.getBlockModel(state);
            ModelData data = model.getModelData(world, pos, state, ModelData.EMPTY);
            random.setSeed(state.getSeed(pos));
            for (RenderType layer : model.getRenderTypes(state, random, data)) {
                pose.pushPose();
                pose.translate(pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ());
                dispatcher.renderBatched(state, pos, world, pose, builder(layer, memory, builders), true, random, data, layer);
                pose.popPose();
            }
        }
        Map<RenderType, VertexBuffer> buffers = new IdentityHashMap<>();
        for (Map.Entry<RenderType, BufferBuilder> entry : builders.entrySet()) {
            MeshData mesh = entry.getValue().build();
            if (mesh != null) {
                VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
                buffer.bind();
                buffer.upload(mesh);
                buffers.put(entry.getKey(), buffer);
            }
        }
        VertexBuffer.unbind();
        memory.values().forEach(ByteBufferBuilder::close);
        return buffers;
    }

    private static BufferBuilder builder(RenderType layer, Map<RenderType, ByteBufferBuilder> memory,
                                         Map<RenderType, BufferBuilder> builders) {
        return builders.computeIfAbsent(layer, type -> {
            ByteBufferBuilder bytes = new ByteBufferBuilder(type.bufferSize());
            memory.put(type, bytes);
            return new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        });
    }

    /** Draws every section of the past, layer by layer in the game's chunk order, as the level renderer does. */
    public static void draw(Matrix4f modelView, Matrix4f projection, Vec3 camera) {
        for (RenderType layer : RenderType.chunkBufferLayers()) {
            boolean set = false;
            ShaderInstance shader = null;
            for (Map.Entry<Long, Map<RenderType, VertexBuffer>> mesh : MESHES.entrySet()) {
                VertexBuffer buffer = mesh.getValue().get(layer);
                if (buffer == null) {
                    continue;
                }
                if (!set) {
                    layer.setupRenderState();
                    shader = RenderSystem.getShader();
                    set = true;
                }
                if (shader == null) {
                    break;
                }
                BlockPos origin = SectionPos.of(mesh.getKey()).origin();
                if (shader.CHUNK_OFFSET != null) {
                    shader.CHUNK_OFFSET.set((float) (origin.getX() - camera.x), (float) (origin.getY() - camera.y),
                            (float) (origin.getZ() - camera.z));
                }
                buffer.bind();
                buffer.drawWithShader(modelView, projection, shader);
            }
            if (set) {
                layer.clearRenderState();
            }
        }
        VertexBuffer.unbind();
    }

    public static void forget(long section) {
        Map<RenderType, VertexBuffer> mesh = MESHES.remove(section);
        if (mesh != null) {
            mesh.values().forEach(VertexBuffer::close);
        }
    }

    public static void clear() {
        MESHES.values().forEach(mesh -> mesh.values().forEach(VertexBuffer::close));
        MESHES.clear();
    }
}
