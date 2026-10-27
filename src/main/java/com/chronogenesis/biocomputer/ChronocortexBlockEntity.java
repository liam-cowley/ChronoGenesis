package com.chronogenesis.biocomputer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.organism.GraftBlockEntity;
import com.chronogenesis.organism.GraftType;
import com.chronogenesis.organism.OrganismCoreBlockEntity;
import com.chronogenesis.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

/**
 * The Chronocortex (Temporal Control Unit), core-systems.md section 5. Stage 0 for now; later stages reach
 * further back in time.
 * <ul>
 *   <li>Essence interface: organisms with a Ganglion graft within {@value #WIRE_RANGE} blocks are wired to it.
 *       It reports their levels, and warns before a wired Time organism bursts.</li>
 *   <li>Research: costs are drawn from the essence stored in wired organisms (see {@link Research}).</li>
 *   <li>It whispers to whoever is near.</li>
 * </ul>
 */
public class ChronocortexBlockEntity extends BlockEntity {
    /** How far its nerves reach: Ganglions within this many blocks are wired to it. */
    public static final int WIRE_RANGE = 32;
    private static final int RESCAN_INTERVAL = 40;
    private static final int WATCH_INTERVAL = 100;
    /** Ticks between two warnings about the same unstable organism. */
    private static final int WARNING_COOLDOWN = 600;
    /** Chance per watch interval of an idle whisper while someone is listening: about every five minutes. */
    private static final int IDLE_CHANCE = 60;

    private int stage;
    private int tick;
    private List<BlockPos> wired = List.of();
    private final Map<BlockPos, Long> lastWarned = new HashMap<>();

    public ChronocortexBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHRONOCORTEX.get(), pos, state);
    }

    public int stage() {
        return stage;
    }

    /** The living organisms currently wired in, by Ganglion. */
    public List<OrganismCoreBlockEntity> wiredCores() {
        List<OrganismCoreBlockEntity> cores = new ArrayList<>();
        if (level != null) {
            for (BlockPos pos : wired) {
                if (level.getBlockEntity(pos) instanceof OrganismCoreBlockEntity core && !core.isNecrotic()) {
                    cores.add(core);
                }
            }
        }
        return cores;
    }

    /** Essence stored across all wired organisms, by aspect. */
    public Map<Aspect, Integer> availableEssence() {
        Map<Aspect, Integer> essence = new EnumMap<>(Aspect.class);
        for (OrganismCoreBlockEntity core : wiredCores()) {
            essence.merge(core.aspect(), core.stored(), Integer::sum);
        }
        return essence;
    }

    /** Draws drams of one aspect out of the wired organisms that hold it. */
    public void drawEssence(Aspect aspect, int amount) {
        int left = amount;
        for (OrganismCoreBlockEntity core : wiredCores()) {
            if (left <= 0) {
                return;
            }
            if (core.aspect() == aspect) {
                left -= core.extract(left);
            }
        }
    }

    public List<OrganismReport> reports() {
        return wiredCores().stream().map(OrganismReport::of).toList();
    }

    // ---------------------------------------------------------------- ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChronocortexBlockEntity cortex) {
        ServerLevel serverLevel = (ServerLevel) level;
        cortex.tick++;
        if (cortex.tick % RESCAN_INTERVAL == 1) {
            cortex.rescan(serverLevel);
        }
        if (cortex.tick % WATCH_INTERVAL == 0) {
            cortex.watch(serverLevel);
        }
    }

    /** Finds Ganglions in range through the loaded chunks' block entities: no block-by-block search. */
    private void rescan(ServerLevel level) {
        Set<BlockPos> cores = new LinkedHashSet<>();
        int minX = (worldPosition.getX() - WIRE_RANGE) >> 4;
        int maxX = (worldPosition.getX() + WIRE_RANGE) >> 4;
        int minZ = (worldPosition.getZ() - WIRE_RANGE) >> 4;
        int maxZ = (worldPosition.getZ() + WIRE_RANGE) >> 4;
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof GraftBlockEntity graft && graft.type() == GraftType.GANGLION
                            && graft.getBlockPos().distSqr(worldPosition) <= (double) WIRE_RANGE * WIRE_RANGE) {
                        OrganismCoreBlockEntity core = graft.core();
                        if (core != null) {
                            cores.add(core.getBlockPos());
                        }
                    }
                }
            }
        }
        wired = List.copyOf(cores);
    }

    /** Warns about wired Time organisms close to bursting, and now and then mutters to itself. */
    private void watch(ServerLevel level) {
        long now = level.getGameTime();
        for (OrganismCoreBlockEntity core : wiredCores()) {
            if (core.aspect() == Aspect.TIME && core.instability() >= OrganismCoreBlockEntity.BURST_AT / 2
                    && now - lastWarned.getOrDefault(core.getBlockPos(), Long.MIN_VALUE / 2) >= WARNING_COOLDOWN) {
                lastWarned.put(core.getBlockPos(), now);
                BlockPos at = core.getBlockPos();
                Whispers.sayNear(level, worldPosition, "unstable", at.getX(), at.getY(), at.getZ());
            }
        }
        List<ServerPlayer> listeners = level.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(Whispers.HEARING_RANGE));
        if (!listeners.isEmpty() && level.random.nextInt(IDLE_CHANCE) == 0) {
            String line = Whispers.randomIdle(level.random);
            listeners.forEach(player -> Whispers.say(player, line));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("stage", stage);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stage = tag.getInt("stage");
    }
}
