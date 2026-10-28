package com.chronogenesis.timetravel;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Offshoots of the Chronocortex planted in past eras (core-systems.md section 5). An offshoot planted in an era has
 * all the time between then and now to grow: by the time the planter is home, an old, petrified Chronocortex has
 * been lying at the same spot in the present all along (a {@link FossilCortexBlock}). It is put there as soon as
 * that spot of the overworld is loaded. Kept in the overworld's saved data.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class Offshoots extends SavedData {
    private static final String NAME = ChronoGenesis.MODID + "_offshoots";
    private static final int CHECK_INTERVAL = 40;
    /** How far up from the planting spot the fossil may move to avoid destroying a block entity. */
    private static final int SHIFT_UP = 8;

    public record Planting(ResourceLocation era, BlockPos pos, boolean grown) {}

    private final List<Planting> plantings = new ArrayList<>();

    public static Offshoots get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(Offshoots::new, Offshoots::load, null), NAME);
    }

    public List<Planting> plantings() {
        return plantings;
    }

    /** An offshoot was planted in an era: a fossil waits at the same spot in the present. */
    public static void plant(MinecraftServer server, ResourceLocation era, BlockPos pos) {
        Offshoots offshoots = get(server);
        offshoots.plantings.add(new Planting(era, pos.immutable(), false));
        offshoots.setDirty();
    }

    /** The era a dimension is, if it is one. */
    @Nullable
    public static ResourceLocation eraOf(Level level) {
        ResourceKey<Level> dimension = level.dimension();
        for (var entry : Eras.all(level.registryAccess())) {
            if (entry.getValue().dimension() == dimension) {
                return entry.getKey();
            }
        }
        return null;
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD
                && level.getGameTime() % CHECK_INTERVAL == 0) {
            grow(level);
        }
    }

    /** Puts a fossil wherever a planting's spot in the present is loaded. Returns how many appeared. */
    public static int grow(ServerLevel present) {
        Offshoots offshoots = get(present.getServer());
        int grown = 0;
        for (int i = 0; i < offshoots.plantings.size(); i++) {
            Planting planting = offshoots.plantings.get(i);
            if (planting.grown() || !present.isLoaded(planting.pos())) {
                continue;
            }
            BlockPos pos = planting.pos();
            for (int up = 0; up < SHIFT_UP && (present.getBlockEntity(pos) != null || present.getBlockState(pos).is(Blocks.BEDROCK)); up++) {
                pos = pos.above();
            }
            present.setBlockAndUpdate(pos, ModBlocks.FOSSIL_CORTEX.get().defaultBlockState());
            if (present.getBlockEntity(pos) instanceof FossilCortexBlockEntity fossil) {
                fossil.setEra(planting.era());
            }
            offshoots.plantings.set(i, new Planting(planting.era(), pos, true));
            offshoots.setDirty();
            grown++;
        }
        return grown;
    }

    private static Offshoots load(CompoundTag tag, HolderLookup.Provider registries) {
        Offshoots offshoots = new Offshoots();
        for (Tag entry : tag.getList("plantings", Tag.TAG_COMPOUND)) {
            CompoundTag planting = (CompoundTag) entry;
            ResourceLocation era = ResourceLocation.tryParse(planting.getString("era"));
            BlockPos pos = NbtUtils.readBlockPos(planting, "pos").orElse(null);
            if (era != null && pos != null) {
                offshoots.plantings.add(new Planting(era, pos, planting.getBoolean("grown")));
            }
        }
        return offshoots;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Planting planting : plantings) {
            CompoundTag entry = new CompoundTag();
            entry.putString("era", planting.era().toString());
            entry.put("pos", NbtUtils.writeBlockPos(planting.pos()));
            entry.putBoolean("grown", planting.grown());
            list.add(entry);
        }
        tag.put("plantings", list);
        return tag;
    }
}
