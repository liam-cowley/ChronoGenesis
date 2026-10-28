package com.chronogenesis.timetravel;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.lens.LensSync;
import com.chronogenesis.lens.PastView;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Builds the past's structures: where a ruin in the overworld has a {@link PastView}, its past template is placed
 * into the past dimension at the same spot and turned the same way, once. The past's own generation already put
 * the same ruin there; the template (air included) replaces it with the building as it was.
 */
public final class PastStamping {
    private PastStamping() {}

    /**
     * Stamps the past of one overworld structure, if it has not been already. The past chunks it covers must be
     * loaded; until they are, they are asked for and this returns false (try again on a later beat).
     */
    public static boolean stamp(ServerLevel past, StructureStart start, PastView view) {
        Stamps stamps = Stamps.get(past);
        String key = view.structure().location() + "@" + start.getChunkPos().toLong();
        if (stamps.done.contains(key)) {
            return true;
        }
        Optional<StructureTemplate> template = past.getStructureManager().get(view.template());
        if (template.isEmpty()) {
            ChronoGenesis.LOGGER.warn("Past template {} is missing", view.template());
            return false;
        }
        boolean ready = true;
        for (StructurePiece piece : start.getPieces()) {
            BoundingBox box = piece.getBoundingBox();
            for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
                for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                    PastDimension.keepLoaded(past, new ChunkPos(cx, cz));
                    ready &= past.getChunkSource().getChunkNow(cx, cz) != null;
                }
            }
        }
        if (!ready) {
            return false;
        }
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof PoolElementStructurePiece pooled) {
                place(past, template.get(), pooled.getPosition(), pooled.getRotation());
                LensSync.invalidate(piece.getBoundingBox());
            }
        }
        stamps.done.add(key);
        stamps.setDirty();
        return true;
    }

    /** Places a template the way a jigsaw piece is placed, air and all, without neighbour updates. */
    public static void place(ServerLevel past, StructureTemplate template, BlockPos origin, Rotation rotation) {
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        template.placeInWorld(past, origin, origin, settings, past.getRandom(), Block.UPDATE_CLIENTS);
    }

    /** Which overworld structures have already had their past stamped. Saved with the past dimension. */
    private static final class Stamps extends SavedData {
        private static final String NAME = "chronogenesis_past_stamps";
        private final Set<String> done = new HashSet<>();

        static Stamps get(ServerLevel past) {
            return past.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Stamps::new, Stamps::load, null), NAME);
        }

        private static Stamps load(CompoundTag tag, HolderLookup.Provider registries) {
            Stamps stamps = new Stamps();
            for (Tag entry : tag.getList("done", Tag.TAG_STRING)) {
                stamps.done.add(entry.getAsString());
            }
            return stamps;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            done.forEach(key -> list.add(StringTag.valueOf(key)));
            tag.put("done", list);
            return tag;
        }
    }
}
