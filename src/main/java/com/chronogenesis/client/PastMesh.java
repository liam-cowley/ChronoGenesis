package com.chronogenesis.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * The past, rebuilt as world geometry: everything inside the structures' boxes (plus a margin of their
 * surroundings) tessellated through a {@link PastWorld}, exactly as a chunk section is, per render layer, with
 * culling, smooth lighting and fluids. Built once per update from the server and kept on the GPU; drawn each
 * frame with the game's own chunk shaders.
 */
public final class PastMesh implements AutoCloseable {
    /** How much of the surroundings is rebuilt with each structure, in blocks. */
    public static final int MARGIN = 6;

    private final BlockPos origin;
    private final Map<RenderType, VertexBuffer> buffers;
    private final List<AABB> volumes;

    private PastMesh(BlockPos origin, Map<RenderType, VertexBuffer> buffers, List<AABB> volumes) {
        this.origin = origin;
        this.buffers = buffers;
        this.volumes = volumes;
    }

    /** The world-space boxes this mesh replaces: inside them, the lens shows only this mesh. */
    public List<AABB> volumes() {
        return volumes;
    }

    @Nullable
    public static PastMesh build(Level level, List<BlockPos> positions, List<BlockState> states, List<BlockPos> erased,
                                 List<BoundingBox> boxes) {
        if (boxes.isEmpty()) {
            return null;
        }
        Map<BlockPos, BlockState> past = new HashMap<>();
        for (int i = 0; i < positions.size(); i++) {
            past.put(positions.get(i), states.get(i));
        }
        PastWorld world = new PastWorld(level, past, new HashSet<>(erased));

        List<BoundingBox> areas = new ArrayList<>();
        List<AABB> volumes = new ArrayList<>();
        for (BoundingBox box : boxes) {
            BoundingBox area = box.inflatedBy(MARGIN);
            areas.add(area);
            volumes.add(new AABB(area.minX(), area.minY(), area.minZ(), area.maxX() + 1, area.maxY() + 1, area.maxZ() + 1));
        }
        BlockPos origin = new BlockPos(areas.get(0).minX(), areas.get(0).minY(), areas.get(0).minZ());

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        Map<RenderType, ByteBufferBuilder> memory = new IdentityHashMap<>();
        Map<RenderType, BufferBuilder> builders = new IdentityHashMap<>();
        RandomSource random = RandomSource.create();
        PoseStack pose = new PoseStack();
        Set<Long> done = new HashSet<>();
        for (BoundingBox area : areas) {
            for (BlockPos pos : BlockPos.betweenClosed(area.minX(), area.minY(), area.minZ(), area.maxX(), area.maxY(), area.maxZ())) {
                if (!done.add(pos.asLong())) {
                    continue;
                }
                BlockState state = world.getBlockState(pos);
                FluidState fluid = world.getFluidState(pos);
                if (!fluid.isEmpty()) {
                    // Liquids are written relative to their chunk section; shift them to the mesh origin.
                    RenderType layer = ItemBlockRenderTypes.getRenderLayer(fluid);
                    VertexConsumer consumer = new Offset(builder(layer, memory, builders),
                            (pos.getX() & ~15) - origin.getX(), (pos.getY() & ~15) - origin.getY(), (pos.getZ() & ~15) - origin.getZ());
                    dispatcher.renderLiquid(pos, world, consumer, state, fluid);
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
        return new PastMesh(origin, buffers, volumes);
    }

    private static BufferBuilder builder(RenderType layer, Map<RenderType, ByteBufferBuilder> memory,
                                         Map<RenderType, BufferBuilder> builders) {
        return builders.computeIfAbsent(layer, type -> {
            ByteBufferBuilder bytes = new ByteBufferBuilder(type.bufferSize());
            memory.put(type, bytes);
            return new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        });
    }

    /** Draws the past, layer by layer in the game's chunk order, as the level renderer draws chunk sections. */
    public void draw(Matrix4f modelView, Matrix4f projection, Vec3 camera) {
        for (RenderType layer : RenderType.chunkBufferLayers()) {
            VertexBuffer buffer = buffers.get(layer);
            if (buffer == null) {
                continue;
            }
            layer.setupRenderState();
            ShaderInstance shader = RenderSystem.getShader();
            if (shader != null) {
                if (shader.CHUNK_OFFSET != null) {
                    shader.CHUNK_OFFSET.set((float) (origin.getX() - camera.x), (float) (origin.getY() - camera.y),
                            (float) (origin.getZ() - camera.z));
                }
                buffer.bind();
                buffer.drawWithShader(modelView, projection, shader);
            }
            layer.clearRenderState();
        }
        VertexBuffer.unbind();
    }

    @Override
    public void close() {
        buffers.values().forEach(VertexBuffer::close);
    }

    /** Shifts every vertex by a fixed offset (for liquids, which are written section-relative). */
    private record Offset(VertexConsumer delegate, float dx, float dy, float dz) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x + dx, y + dy, z + dz);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            delegate.setColor(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }
    }
}
