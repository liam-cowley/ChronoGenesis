package com.chronogenesis.timetravel;

import java.util.Comparator;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.ChronoGenesis;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * The past: a real dimension ({@code data/chronogenesis/dimension/past.json}) generated from the same generator
 * and seed as the overworld, so its land is the land as it first was, before anyone dug or built. The ruins with
 * a past view are stamped into it in their prime ({@link PastStamping}). Nobody walks in it yet: the Temporal
 * Lens looks into it, and later eras of time travel build on it.
 */
public final class PastDimension {
    public static final ResourceKey<Level> KEY = ResourceKey.create(Registries.DIMENSION, ChronoGenesis.id("past"));

    /**
     * Keeps a past chunk generated and loaded, but not ticking (nothing moves or grows there), while a Lens looks
     * at it. Expires by itself unless renewed.
     */
    public static final TicketType<ChunkPos> LENS_TICKET =
            TicketType.create("chronogenesis_lens", Comparator.comparingLong(ChunkPos::toLong), 100);

    private PastDimension() {}

    @Nullable
    public static ServerLevel level(MinecraftServer server) {
        return server.getLevel(KEY);
    }

    /** Asks for a past chunk to be generated (asynchronously) and kept loaded for a while. */
    public static void keepLoaded(ServerLevel past, ChunkPos pos) {
        past.getChunkSource().addRegionTicket(LENS_TICKET, pos, 0, pos);
    }

    /** Whether the past mirrors this level: only the overworld has one so far. */
    public static boolean hasPast(Level level) {
        return level.dimension() == Level.OVERWORLD;
    }
}
