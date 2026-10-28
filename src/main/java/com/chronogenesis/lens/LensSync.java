package com.chronogenesis.lens;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.network.PastSectionsPayload;
import com.chronogenesis.timetravel.PastDimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Sends the past to a player looking through the Temporal Lens: the past dimension's chunk sections around them,
 * nearest first, a few at a time, each once. Past chunks are asked for (generated if new) as needed. What a
 * player has been sent is forgotten as they move away, so it is sent again if they come back.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class LensSync {
    /** How far around the viewer the past is sent, in chunks. */
    public static final int SEND_RADIUS = 3;
    /** Sections farther than this (in chunks) are forgotten, here and on the client. */
    public static final int KEEP_RADIUS = 6;
    /** Sections above and below the viewer's own. */
    public static final int VERTICAL = 3;
    /** Sections sent per burst. */
    private static final int PER_BURST = 12;

    private static final Map<UUID, Set<Long>> SENT = new HashMap<>();

    private LensSync() {}

    /** One burst of the past for a player looking through the Lens. */
    public static void tick(ServerPlayer player) {
        ServerLevel past = PastDimension.level(player.server);
        if (past == null || !PastDimension.hasPast(player.level()) || player instanceof FakePlayer) {
            return;
        }
        Set<Long> sent = SENT.computeIfAbsent(player.getUUID(), id -> new HashSet<>());
        ChunkPos centre = player.chunkPosition();
        sent.removeIf(pos -> Math.max(Math.abs(SectionPos.x(pos) - centre.x), Math.abs(SectionPos.z(pos) - centre.z)) > KEEP_RADIUS);

        int centreY = SectionPos.blockToSectionCoord(player.getBlockY());
        int minY = Math.max(past.getMinSection(), centreY - VERTICAL);
        int maxY = Math.min(past.getMaxSection() - 1, centreY + VERTICAL);
        List<ChunkPos> chunks = new ArrayList<>();
        for (int dx = -SEND_RADIUS; dx <= SEND_RADIUS; dx++) {
            for (int dz = -SEND_RADIUS; dz <= SEND_RADIUS; dz++) {
                chunks.add(new ChunkPos(centre.x + dx, centre.z + dz));
            }
        }
        chunks.sort(Comparator.comparingInt(c -> Math.max(Math.abs(c.x - centre.x), Math.abs(c.z - centre.z))));

        List<PastSectionsPayload.Section> batch = new ArrayList<>();
        for (ChunkPos chunkPos : chunks) {
            PastDimension.keepLoaded(past, chunkPos);
            LevelChunk chunk = past.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
            if (chunk == null) {
                continue; // still generating: next burst
            }
            for (int sy = minY; sy <= maxY && batch.size() < PER_BURST; sy++) {
                long key = SectionPos.asLong(chunkPos.x, sy, chunkPos.z);
                if (sent.add(key)) {
                    batch.add(encode(past, chunk, sy, key));
                }
            }
            if (batch.size() >= PER_BURST) {
                break;
            }
        }
        if (!batch.isEmpty()) {
            PastSectionsPayload payload = new PastSectionsPayload(batch);
            if (player.connection != null && player.connection.hasChannel(payload)) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    /** A section's blocks and the past's own final light values at every block of it. */
    public static PastSectionsPayload.Section encode(ServerLevel past, LevelChunk chunk, int sectionY, long key) {
        DataLayer sky = new DataLayer();
        DataLayer block = new DataLayer();
        SectionPos section = SectionPos.of(key);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    pos.set(section.minBlockX() + x, section.minBlockY() + y, section.minBlockZ() + z);
                    sky.set(x, y, z, past.getBrightness(LightLayer.SKY, pos));
                    block.set(x, y, z, past.getBrightness(LightLayer.BLOCK, pos));
                }
            }
        }
        return PastSectionsPayload.Section.of(key, chunk.getSection(chunk.getSectionIndexFromSectionY(sectionY)), sky, block);
    }

    /** The past changed inside {@code box} (a structure was stamped): send those sections again. */
    public static void invalidate(BoundingBox box) {
        for (Set<Long> sent : SENT.values()) {
            sent.removeIf(pos -> SectionPos.x(pos) >= box.minX() >> 4 && SectionPos.x(pos) <= box.maxX() >> 4
                    && SectionPos.y(pos) >= box.minY() >> 4 && SectionPos.y(pos) <= box.maxY() >> 4
                    && SectionPos.z(pos) >= box.minZ() >> 4 && SectionPos.z(pos) <= box.maxZ() >> 4);
        }
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }
}
