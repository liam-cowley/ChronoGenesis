package com.chronogenesis.lens;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.chronogenesis.ChronoGenesis;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * Works out what the Temporal Lens shows: for every structure near the viewer that has a {@link PastView}, the
 * blocks of its past template that differ from the ruin standing there now. The client draws those as ghosts.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class PastViews {
    public static final ResourceKey<Registry<PastView>> REGISTRY = ResourceKey.createRegistryKey(ChronoGenesis.id("past_view"));
    /** How far the Lens looks, in chunks around the viewer. */
    public static final int CHUNK_RADIUS = 3;
    /** A cap on what is sent to the client in one view. */
    public static final int MAX_BLOCKS = 8192;

    /** One block of the past, and where it stood. */
    public record PastBlock(BlockPos pos, BlockState state) {}

    /** What the Lens sees: the past blocks, and any clues seeing them gives. */
    public record View(List<PastBlock> blocks, List<ResourceLocation> clues) {
        public static final View EMPTY = new View(List.of(), List.of());
    }

    private PastViews() {}

    @SubscribeEvent
    static void registerRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(REGISTRY, PastView.CODEC);
    }

    public static View around(ServerLevel level, BlockPos center, LensDepth depth) {
        Optional<Registry<PastView>> views = level.registryAccess().registry(REGISTRY);
        if (views.isEmpty()) {
            return View.EMPTY;
        }
        Registry<Structure> structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        List<PastBlock> blocks = new ArrayList<>();
        List<ResourceLocation> clues = new ArrayList<>();
        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;
        for (int x = cx - CHUNK_RADIUS; x <= cx + CHUNK_RADIUS; x++) {
            for (int z = cz - CHUNK_RADIUS; z <= cz + CHUNK_RADIUS; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) {
                    continue;
                }
                for (Map.Entry<Structure, StructureStart> start : chunk.getAllStarts().entrySet()) {
                    Optional<ResourceKey<Structure>> key = structures.getResourceKey(start.getKey());
                    if (key.isEmpty() || !start.getValue().isValid()) {
                        continue;
                    }
                    for (PastView view : views.get()) {
                        if (view.structure().equals(key.get()) && depth.reaches(view.depth())) {
                            addStart(level, start.getValue(), view, blocks);
                            view.clue().ifPresent(clues::add);
                        }
                    }
                }
            }
        }
        return new View(blocks.size() > MAX_BLOCKS ? blocks.subList(0, MAX_BLOCKS) : blocks, clues);
    }

    private static void addStart(ServerLevel level, StructureStart start, PastView view, List<PastBlock> out) {
        Optional<StructureTemplate> template = level.getStructureManager().get(view.template());
        if (template.isEmpty()) {
            ChronoGenesis.LOGGER.warn("Past view template {} is missing", view.template());
            return;
        }
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof PoolElementStructurePiece pooled) {
                out.addAll(diff(level, template.get(), pooled.getPosition(), pooled.getRotation()));
            }
        }
    }

    /**
     * The blocks of {@code template}, placed at {@code origin} with {@code rotation} the way a jigsaw piece is,
     * that are not already standing there. Air in the past is left out: the Lens adds, it cannot take away.
     */
    public static List<PastBlock> diff(Level level, StructureTemplate template, BlockPos origin, Rotation rotation) {
        List<PastBlock> out = new ArrayList<>();
        if (template.palettes.isEmpty()) {
            return out;
        }
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        for (StructureTemplate.StructureBlockInfo info : template.palettes.get(0).blocks()) {
            if (info.state().isAir() || info.state().is(Blocks.STRUCTURE_VOID)) {
                continue;
            }
            BlockPos pos = origin.offset(StructureTemplate.calculateRelativePosition(settings, info.pos()));
            BlockState state = info.state().rotate(level, pos, rotation);
            if (!level.getBlockState(pos).equals(state)) {
                out.add(new PastBlock(pos, state));
            }
        }
        return out;
    }
}
