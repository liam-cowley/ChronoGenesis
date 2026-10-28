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
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
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

    /**
     * What the Lens sees of the past: every block of it that stood within the structures in view, where
     * something stands now that did not then (rubble, cobwebs, blocks placed since: air in the past), and the
     * boxes the structures occupy. Inside the lens, within those boxes, the world is rebuilt as it was.
     */
    public record Past(List<PastBlock> blocks, List<BlockPos> erased, List<BoundingBox> boxes) {
        public static final Past EMPTY = new Past(List.of(), List.of(), List.of());
    }

    /** What the Lens sees: the past, and any clues seeing it gives. */
    public record View(Past past, List<ResourceLocation> clues) {
        public static final View EMPTY = new View(Past.EMPTY, List.of());
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
        List<BlockPos> erased = new ArrayList<>();
        List<BoundingBox> boxes = new ArrayList<>();
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
                            addStart(level, start.getValue(), view, blocks, erased, boxes);
                            view.clue().ifPresent(clues::add);
                        }
                    }
                }
            }
        }
        // Nothing of the past survives inside an obelisk's event horizon: the shader blacks it out instead.
        List<BlockPos> obelisks = Obelisks.near(level, Vec3.atCenterOf(center), (CHUNK_RADIUS + 1) * 16.0);
        if (!obelisks.isEmpty()) {
            blocks.removeIf(block -> Obelisks.swallowed(obelisks, Vec3.atCenterOf(block.pos())));
            erased.removeIf(pos -> Obelisks.swallowed(obelisks, Vec3.atCenterOf(pos)));
        }
        return new View(new Past(cap(blocks), cap(erased), boxes), clues);
    }

    private static <T> List<T> cap(List<T> list) {
        return list.size() > MAX_BLOCKS ? list.subList(0, MAX_BLOCKS) : list;
    }

    private static void addStart(ServerLevel level, StructureStart start, PastView view, List<PastBlock> blocks,
                                 List<BlockPos> erased, List<BoundingBox> boxes) {
        Optional<StructureTemplate> template = level.getStructureManager().get(view.template());
        if (template.isEmpty()) {
            ChronoGenesis.LOGGER.warn("Past view template {} is missing", view.template());
            return;
        }
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof PoolElementStructurePiece pooled) {
                Past past = pastOf(level, template.get(), pooled.getPosition(), pooled.getRotation());
                blocks.addAll(past.blocks());
                erased.addAll(past.erased());
                boxes.add(pooled.getBoundingBox());
            }
        }
    }

    /**
     * The past of one piece: {@code template} placed at {@code origin} with {@code rotation} the way a jigsaw
     * piece is. Every solid block of the template is part of it; wherever the template says air but something
     * stands there now, that block is erased. Spots the template leaves out (structure void) keep the present.
     */
    public static Past pastOf(Level level, StructureTemplate template, BlockPos origin, Rotation rotation) {
        List<PastBlock> blocks = new ArrayList<>();
        List<BlockPos> erased = new ArrayList<>();
        if (template.palettes.isEmpty()) {
            return Past.EMPTY;
        }
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        for (StructureTemplate.StructureBlockInfo info : template.palettes.get(0).blocks()) {
            if (info.state().is(Blocks.STRUCTURE_VOID)) {
                continue;
            }
            BlockPos pos = origin.offset(StructureTemplate.calculateRelativePosition(settings, info.pos()));
            if (info.state().isAir()) {
                if (!level.getBlockState(pos).isAir()) {
                    erased.add(pos);
                }
            } else {
                blocks.add(new PastBlock(pos, info.state().rotate(level, pos, rotation)));
            }
        }
        return new Past(blocks, erased, List.of(template.getBoundingBox(settings, origin)));
    }
}
