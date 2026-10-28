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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * Finds the structures near a viewer whose past the Temporal Lens can show: each overworld structure start with a
 * {@link PastView} (data-driven: {@code data/<ns>/chronogenesis/past_view/<name>.json}). Their past templates
 * are stamped into the past dimension ({@link com.chronogenesis.timetravel.PastStamping}), which the Lens shows.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class PastViews {
    public static final ResourceKey<Registry<PastView>> REGISTRY = ResourceKey.createRegistryKey(ChronoGenesis.id("past_view"));
    /** How far around the viewer structures are looked for, in chunks. */
    public static final int CHUNK_RADIUS = 3;

    /** A structure start in the present, and how it looked in the past. */
    public record Found(StructureStart start, PastView view) {}

    private PastViews() {}

    @SubscribeEvent
    static void registerRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(REGISTRY, PastView.CODEC);
    }

    /** Structures with a past view whose start lies within {@value #CHUNK_RADIUS} chunks, deep enough to see. */
    public static List<Found> nearby(ServerLevel level, BlockPos center, LensDepth depth) {
        Optional<Registry<PastView>> views = level.registryAccess().registry(REGISTRY);
        List<Found> found = new ArrayList<>();
        if (views.isEmpty()) {
            return found;
        }
        Registry<Structure> structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
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
                            found.add(new Found(start.getValue(), view));
                        }
                    }
                }
            }
        }
        return found;
    }
}
