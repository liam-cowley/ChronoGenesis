package com.chronogenesis.timetravel;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.lens.LensSync;
import com.chronogenesis.registry.ModDataMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Remnants: what a traveller leaves in a past era, if it lasts (core-systems.md section 5).
 * <p>
 * A block that outlasts the ages (it has a {@link RemnantForm} in the {@code chronogenesis:remnant} data map) placed
 * in an era is recorded here. It has been standing all the time since, so in the present it is still there at the
 * same spot, crumbled (stone bricks cracked, cobblestone mossy, copper green, gold just as it was), and the Temporal
 * Lens, which looks into a past long after that era, sees it too. A Cortex Offshoot is the most important case: it
 * remains as a {@link FossilCortexBlock}.
 * <p>
 * Each remnant is put in place in the present (the overworld) and in the Lens's past as soon as its spot there is
 * loaded, never over a block entity (it moves up a little instead). Broken in its era again before it could last,
 * it is forgotten, and taken back out of the present and the past if it was already there.
 * Kept in the overworld's saved data.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class Remnants extends SavedData {
    private static final String NAME = ChronoGenesis.MODID + "_remnants";
    private static final int CHECK_INTERVAL = 40;
    /** How far up a remnant may move to avoid destroying a block entity. */
    private static final int SHIFT_UP = 8;

    /** A remnant, and where it already stands: {@code present} (the overworld) and {@code past} (the Lens's past). */
    public record Remnant(ResourceLocation era, BlockPos pos, BlockState state, @Nullable BlockPos present, @Nullable BlockPos past) {
        Remnant at(boolean inPresent, BlockPos where) {
            return inPresent ? new Remnant(era, pos, state, where, past) : new Remnant(era, pos, state, present, where);
        }
    }

    private final List<Remnant> remnants = new ArrayList<>();

    public static Remnants get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(Remnants::new, Remnants::load, null), NAME);
    }

    public List<Remnant> remnants() {
        return remnants;
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

    /** What a placed block becomes over the ages, or null if it does not last. Its shape (facing, half...) is kept. */
    @Nullable
    public static BlockState crumbled(BlockState placed) {
        RemnantForm form = placed.getBlockHolder().getData(ModDataMaps.REMNANT);
        if (form == null) {
            return null;
        }
        if (form.into().isEmpty()) {
            return placed;
        }
        BlockState into = form.into().get().defaultBlockState();
        for (Property<?> property : placed.getProperties()) {
            into = copy(placed, into, property);
        }
        return into;
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> property) {
        for (Property<?> target : to.getProperties()) {
            if (target.getName().equals(property.getName()) && target.getValueClass() == property.getValueClass()) {
                @SuppressWarnings("unchecked")
                Property<T> same = (Property<T>) target;
                T value = from.getValue(property);
                if (same.getPossibleValues().contains(value)) {
                    return to.setValue(same, value);
                }
            }
        }
        return to;
    }

    /** A lasting block was placed in an era: it will remain. */
    public static void record(MinecraftServer server, ResourceLocation era, BlockPos pos, BlockState crumbled) {
        Remnants remnants = get(server);
        remnants.remnants.removeIf(remnant -> remnant.pos().equals(pos) && remnant.era().equals(era));
        remnants.remnants.add(new Remnant(era, pos.immutable(), crumbled, null, null));
        remnants.setDirty();
    }

    /** It was broken in its era: it never lasted. Taken back out of the present and the past where it still stands. */
    public static void forget(MinecraftServer server, ResourceLocation era, BlockPos pos) {
        Remnants remnants = get(server);
        for (Remnant remnant : List.copyOf(remnants.remnants)) {
            if (remnant.pos().equals(pos) && remnant.era().equals(era)) {
                remnants.remnants.remove(remnant);
                undo(server.overworld(), remnant.present(), remnant.state());
                undo(PastDimension.level(server), remnant.past(), remnant.state());
                remnants.setDirty();
            }
        }
    }

    private static void undo(@Nullable ServerLevel level, @Nullable BlockPos where, BlockState state) {
        if (level != null && where != null && level.isLoaded(where) && level.getBlockState(where).is(state.getBlock())) {
            level.setBlockAndUpdate(where, Blocks.AIR.defaultBlockState());
            if (level.dimension() == PastDimension.KEY) {
                LensSync.invalidate(new BoundingBox(where));
            }
        }
    }

    // ---------------------------------------------------------------- noticing, and putting them in place

    @SubscribeEvent
    static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || com.chronogenesis.entity.CargoHold.isHold(event.getPos())) {
            // Blocks on a beast's back go where the beast goes: they stay nowhere.
            return;
        }
        ResourceLocation era = eraOf(level);
        BlockState crumbled = era == null ? null : crumbled(event.getPlacedBlock());
        if (crumbled != null) {
            record(level.getServer(), era, event.getPos(), crumbled);
        }
    }

    @SubscribeEvent
    static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ResourceLocation era = eraOf(level);
            if (era != null) {
                forget(level.getServer(), era, event.getPos());
            }
        }
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % CHECK_INTERVAL == 0
                && (level.dimension() == Level.OVERWORLD || level.dimension() == PastDimension.KEY)) {
            materialize(level);
        }
    }

    /**
     * Puts every remnant whose spot is loaded into the present (the overworld) or the Lens's past, whichever this
     * level is. Returns how many appeared.
     */
    public static int materialize(ServerLevel level) {
        boolean inPresent = level.dimension() != PastDimension.KEY;
        Remnants remnants = get(level.getServer());
        int placed = 0;
        for (int i = 0; i < remnants.remnants.size(); i++) {
            Remnant remnant = remnants.remnants.get(i);
            if ((inPresent ? remnant.present() : remnant.past()) != null || !level.isLoaded(remnant.pos())) {
                continue;
            }
            BlockPos pos = remnant.pos();
            for (int up = 0; up < SHIFT_UP && (level.getBlockEntity(pos) != null || level.getBlockState(pos).is(Blocks.BEDROCK)); up++) {
                pos = pos.above();
            }
            level.setBlockAndUpdate(pos, remnant.state());
            if (level.getBlockEntity(pos) instanceof FossilCortexBlockEntity fossil) {
                fossil.setEra(remnant.era());
            }
            if (!inPresent) {
                LensSync.invalidate(new BoundingBox(pos));
            }
            remnants.remnants.set(i, remnant.at(inPresent, pos));
            remnants.setDirty();
            placed++;
        }
        return placed;
    }

    // ---------------------------------------------------------------- saving

    private static Remnants load(CompoundTag tag, HolderLookup.Provider registries) {
        Remnants remnants = new Remnants();
        var blocks = registries.lookupOrThrow(Registries.BLOCK);
        for (Tag entry : tag.getList("remnants", Tag.TAG_COMPOUND)) {
            CompoundTag remnant = (CompoundTag) entry;
            ResourceLocation era = ResourceLocation.tryParse(remnant.getString("era"));
            BlockPos pos = NbtUtils.readBlockPos(remnant, "pos").orElse(null);
            if (era != null && pos != null) {
                remnants.remnants.add(new Remnant(era, pos, NbtUtils.readBlockState(blocks, remnant.getCompound("state")),
                        NbtUtils.readBlockPos(remnant, "present").orElse(null), NbtUtils.readBlockPos(remnant, "past").orElse(null)));
            }
        }
        return remnants;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Remnant remnant : remnants) {
            CompoundTag entry = new CompoundTag();
            entry.putString("era", remnant.era().toString());
            entry.put("pos", NbtUtils.writeBlockPos(remnant.pos()));
            entry.put("state", NbtUtils.writeBlockState(remnant.state()));
            if (remnant.present() != null) {
                entry.put("present", NbtUtils.writeBlockPos(remnant.present()));
            }
            if (remnant.past() != null) {
                entry.put("past", NbtUtils.writeBlockPos(remnant.past()));
            }
            list.add(entry);
        }
        tag.put("remnants", list);
        return tag;
    }
}
