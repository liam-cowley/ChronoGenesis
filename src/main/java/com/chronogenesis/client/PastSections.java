package com.chronogenesis.client;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.lens.LensSync;
import com.chronogenesis.network.PastSectionsPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * The client's copy of the past around the player: chunk sections of the past dimension, as the server sent them
 * for the Temporal Lens. Kept between looks; forgotten when the player moves away or changes world.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID, value = Dist.CLIENT)
public final class PastSections {
    /** One section of the past: its blocks and its light. */
    public record Section(LevelChunkSection blocks, DataLayer sky, DataLayer block) {}

    private static final Map<Long, Section> SECTIONS = new HashMap<>();
    /** Sections whose mesh must be (re)built: new ones, and their neighbours, whose faces they may hide. */
    private static final Set<Long> DIRTY = new LinkedHashSet<>();

    private PastSections() {}

    public static void receive(PastSectionsPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        var biomes = minecraft.level.registryAccess().registryOrThrow(Registries.BIOME);
        for (PastSectionsPayload.Section section : payload.sections()) {
            SECTIONS.put(section.pos(), new Section(section.decode(biomes), section.skyLight(), section.blockLight()));
            DIRTY.add(section.pos());
            for (Direction direction : Direction.values()) {
                long neighbour = SectionPos.offset(section.pos(), direction);
                if (SECTIONS.containsKey(neighbour)) {
                    DIRTY.add(neighbour);
                }
            }
        }
    }

    /** The mesh of this section must be rebuilt (models changed). */
    public static void markDirty(long pos) {
        DIRTY.add(pos);
    }

    @Nullable
    public static Section get(long pos) {
        return SECTIONS.get(pos);
    }

    public static Set<Long> positions() {
        return SECTIONS.keySet();
    }

    /** Takes up to {@code count} sections whose mesh needs building. */
    public static Set<Long> takeDirty(int count) {
        Set<Long> taken = new LinkedHashSet<>();
        var it = DIRTY.iterator();
        while (it.hasNext() && taken.size() < count) {
            taken.add(it.next());
            it.remove();
        }
        return taken;
    }

    /** Forgets sections far from the player, as the server does, so they are sent again on return. */
    public static void prune(ChunkPos centre) {
        SECTIONS.keySet().removeIf(pos -> {
            boolean far = Math.max(Math.abs(SectionPos.x(pos) - centre.x), Math.abs(SectionPos.z(pos) - centre.z)) > LensSync.KEEP_RADIUS;
            if (far) {
                DIRTY.remove(pos);
                PastMesh.forget(pos);
            }
            return far;
        });
    }

    public static void clear() {
        SECTIONS.clear();
        DIRTY.clear();
        PastMesh.clear();
    }

    @SubscribeEvent
    static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            clear();
        }
    }
}
