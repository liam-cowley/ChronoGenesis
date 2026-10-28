package com.chronogenesis.biocomputer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.organism.GraftBlockEntity;
import com.chronogenesis.organism.GraftType;
import com.chronogenesis.organism.OrganismCoreBlockEntity;
import com.chronogenesis.registry.ModBlockEntities;
import com.chronogenesis.registry.ModBlocks;
import com.chronogenesis.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

/**
 * The Chronocortex (Temporal Control Unit), core-systems.md section 5.
 * <ul>
 *   <li>Essence interface: organisms with a Ganglion graft within its wiring range (32 blocks, more as it grows)
 *       are wired to it. It reports their levels, and warns before a wired Time organism bursts.</li>
 *   <li>Research: costs are drawn from the essence stored in wired organisms (see {@link Research}).</li>
 *   <li>Growth: it grows lobes ({@link CortexLobeBlock}) over Brass Armature around its jar, one every few seconds,
 *       for {@value #LOBE_COST} drams of Awareness from its wired organisms. Each lobe is room in its mind.</li>
 *   <li>Lessons: show it things ({@link Lesson}) and it learns what they mean, if it has the room and is old
 *       enough. Lessons make it aware.</li>
 *   <li>Stages: lobes and awareness together make it grow up ({@link #STAGE_LOBES}, {@link #STAGE_AWARENESS};
 *       stage 1 also needs words, stage 2 a name). The last stage, the Cerebrum, is a multiblock: its lobes must
 *       fill the whole 5 x 5 x 5 cranium around the jar. Break the cranium and it falls back to stage 2.</li>
 *   <li>Speech: once it has words, it answers what is said near it ({@link CortexSpeech}).</li>
 * </ul>
 */
public class ChronocortexBlockEntity extends BlockEntity {
    /** How far its nerves reach, by stage: Ganglions this close are wired to it. */
    private static final int[] WIRE_RANGES = {32, 48, 64, 96};
    /** Lobes and awareness needed for each stage (stage 3 also needs the full cranium). */
    public static final int[] STAGE_LOBES = {0, 8, 32, 124};
    public static final int[] STAGE_AWARENESS = {0, 4, 9, 13};
    public static final int MAX_STAGE = 3;
    /** The cranium: lobes within this many blocks of the jar, all around it, make the Cerebrum. */
    public static final int CRANIUM_RADIUS = 2;
    public static final int LOBE_COST = 4;
    private static final int GROW_INTERVAL = 100;
    private static final int RESCAN_INTERVAL = 40;
    private static final int WATCH_INTERVAL = 100;
    /** Ticks between two warnings about the same unstable organism. */
    private static final int WARNING_COOLDOWN = 600;
    /** Chance per watch interval of an idle whisper while someone is listening: about every five minutes. */
    private static final int IDLE_CHANCE = 60;
    private static final int MAX_NAME = 24;

    /** What it heard, to answer once it has thought about it. Not saved. */
    private record Heard(UUID player, String said, int at) {}

    private int stage;
    private int tick;
    private int lobes;
    private final Set<ResourceLocation> lessons = new LinkedHashSet<>();
    @Nullable
    private String name;
    private final Map<UUID, String> nicknames = new HashMap<>();
    private final List<Heard> heard = new ArrayList<>();
    private List<BlockPos> wired = List.of();
    private final Map<BlockPos, Long> lastWarned = new HashMap<>();

    public ChronocortexBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHRONOCORTEX.get(), pos, state);
    }

    public int stage() {
        return stage;
    }

    public int lobes() {
        return lobes;
    }

    public Set<ResourceLocation> lessons() {
        return lessons;
    }

    public boolean knows(ResourceLocation lesson) {
        return lessons.contains(lesson);
    }

    public boolean canSpeak() {
        return knows(Lessons.WORDS);
    }

    @Nullable
    public String name() {
        return name;
    }

    public Component displayName() {
        return name == null ? Component.translatable("whisper.chronogenesis.unnamed") : Component.literal(name);
    }

    /** What it calls a listener: the name they asked for, or their own. */
    public Component callName(Player player) {
        String nickname = nicknames.get(player.getUUID());
        return nickname != null ? Component.literal(nickname) : player.getName();
    }

    void nickname(Player player, String nickname) {
        nicknames.put(player.getUUID(), nickname.length() > MAX_NAME ? nickname.substring(0, MAX_NAME) : nickname);
        setChanged();
    }

    public int wireRange() {
        return WIRE_RANGES[Math.min(stage, WIRE_RANGES.length - 1)];
    }

    /** The sum of the awareness its lessons gave it. */
    public int awareness() {
        if (level == null) {
            return 0;
        }
        int total = 0;
        for (ResourceLocation id : lessons) {
            Lesson lesson = Lessons.get(level.registryAccess(), id);
            if (lesson != null) {
                total += lesson.awareness();
            }
        }
        return total;
    }

    /** The first lesson it has not learned and is old enough for: what it asks for. */
    @Nullable
    public Map.Entry<ResourceLocation, Lesson> nextLesson() {
        if (level == null) {
            return null;
        }
        for (Map.Entry<ResourceLocation, Lesson> entry : Lessons.all(level.registryAccess())) {
            if (!knows(entry.getKey()) && entry.getValue().stage() <= stage) {
                return entry;
            }
        }
        return null;
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

    // ---------------------------------------------------------------- talking

    /** Says something to everyone in hearing range, in its own voice. */
    public void say(Component text) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Component line = Whispers.voiced(name, text);
        double range = CortexSpeech.hearing(stage);
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(range))) {
            player.sendSystemMessage(line);
        }
        serverLevel.playSound(null, worldPosition, ModSounds.CORTEX_SPEAKS.get(), SoundSource.BLOCKS, 0.8f,
                1.3f - 0.15f * stage);
    }

    public void say(String key, Object... args) {
        say(Component.translatable("whisper.chronogenesis." + key, args));
    }

    /** It heard someone; it answers in a second or two. */
    void hear(ServerPlayer player, String said) {
        if (heard.size() < 4) {
            heard.add(new Heard(player.getUUID(), said, tick + 20 + (level == null ? 0 : level.random.nextInt(30))));
        }
    }

    // ---------------------------------------------------------------- teaching

    /** Shown something: if it is a lesson it can learn now, it learns it (and the thing is used up). */
    public ItemInteractionResult teach(ItemStack stack, Player player) {
        if (level == null || stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        Map.Entry<ResourceLocation, Lesson> entry = Lessons.taughtBy(level.registryAccess(), stack);
        if (entry == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        ResourceLocation id = entry.getKey();
        Lesson lesson = entry.getValue();
        if (knows(id)) {
            say("lesson.known", lesson.title());
            return ItemInteractionResult.CONSUME;
        }
        if (stage < lesson.stage()) {
            say("lesson.too_young", lesson.title());
            return ItemInteractionResult.CONSUME;
        }
        if (lobes < lesson.capacity()) {
            say("lesson.no_room", lesson.title(), lesson.capacity(), lobes);
            return ItemInteractionResult.CONSUME;
        }
        if (id.equals(Lessons.SELF)) {
            Component custom = stack.get(DataComponents.CUSTOM_NAME);
            if (custom == null || custom.getString().isBlank()) {
                say("lesson.self_unnamed");
                return ItemInteractionResult.CONSUME;
            }
            String chosen = custom.getString().trim();
            name = chosen.length() > MAX_NAME ? chosen.substring(0, MAX_NAME) : chosen;
        }
        lessons.add(id);
        stack.consume(1, player);
        setChanged();
        ServerLevel serverLevel = (ServerLevel) level;
        serverLevel.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 0.6f);
        serverLevel.sendParticles(ParticleTypes.ENCHANT, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2,
                worldPosition.getZ() + 0.5, 40, 0.4, 0.4, 0.4, 0.6);
        say(lesson.learned());
        checkStage(serverLevel);
        return ItemInteractionResult.SUCCESS;
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
        if (cortex.tick % GROW_INTERVAL == 50) {
            cortex.grow(serverLevel);
            cortex.checkStage(serverLevel);
        }
        if (!cortex.heard.isEmpty()) {
            Heard first = cortex.heard.get(0);
            if (cortex.tick >= first.at) {
                cortex.heard.remove(0);
                if (serverLevel.getPlayerByUUID(first.player) instanceof ServerPlayer player) {
                    CortexSpeech.reply(cortex, player, first.said);
                }
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            CortexSpeech.LOADED.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        CortexSpeech.LOADED.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        CortexSpeech.LOADED.remove(this);
    }

    /** Finds Ganglions in range through the loaded chunks' block entities: no block-by-block search. */
    private void rescan(ServerLevel level) {
        Set<BlockPos> cores = new LinkedHashSet<>();
        int range = wireRange();
        int minX = (worldPosition.getX() - range) >> 4;
        int maxX = (worldPosition.getX() + range) >> 4;
        int minZ = (worldPosition.getZ() - range) >> 4;
        int maxZ = (worldPosition.getZ() + range) >> 4;
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof GraftBlockEntity graft && graft.type() == GraftType.GANGLION
                            && graft.getBlockPos().distSqr(worldPosition) <= (double) range * range) {
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
                say("unstable", at.getX(), at.getY(), at.getZ());
            }
        }
        List<ServerPlayer> listeners = level.getEntitiesOfClass(ServerPlayer.class,
                new AABB(worldPosition).inflate(CortexSpeech.hearing(stage)));
        if (!listeners.isEmpty() && level.random.nextInt(IDLE_CHANCE) == 0) {
            CortexSpeech.idle(this, level, listeners.get(level.random.nextInt(listeners.size())));
        }
    }

    // ---------------------------------------------------------------- growing

    /**
     * The lobes it has, found by walking from the jar through touching lobes (within reach); and one more grown
     * into the nearest bare armature that touches them, if its organisms can spare the Awareness.
     */
    private void grow(ServerLevel level) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(worldPosition);
        seen.add(worldPosition);
        BlockPos frame = null;
        int count = 0;
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!seen.add(next) || !withinReach(next) || !level.isLoaded(next)) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                if (state.is(ModBlocks.CORTEX_LOBE.get())) {
                    count++;
                    queue.add(next);
                } else if (frame == null && state.is(ModBlocks.BRASS_ARMATURE.get())) {
                    frame = next;
                }
            }
        }
        if (frame != null && availableEssence().getOrDefault(Aspect.AWARENESS, 0) >= LOBE_COST) {
            drawEssence(Aspect.AWARENESS, LOBE_COST);
            level.setBlockAndUpdate(frame, ModBlocks.CORTEX_LOBE.get().defaultBlockState());
            level.playSound(null, frame, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 0.7f, 0.6f);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, frame.getX() + 0.5, frame.getY() + 0.5, frame.getZ() + 0.5,
                    10, 0.3, 0.3, 0.3, 0.05);
            count++;
        }
        if (count != lobes) {
            lobes = count;
            setChanged();
        }
    }

    private boolean withinReach(BlockPos pos) {
        return Math.abs(pos.getX() - worldPosition.getX()) <= CortexLobeBlock.REACH
                && Math.abs(pos.getY() - worldPosition.getY()) <= CortexLobeBlock.REACH
                && Math.abs(pos.getZ() - worldPosition.getZ()) <= CortexLobeBlock.REACH;
    }

    /** Whether lobes (firm ones) fill every block of the 5 x 5 x 5 cranium around the jar. */
    public boolean craniumComplete() {
        if (level == null) {
            return false;
        }
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-CRANIUM_RADIUS, -CRANIUM_RADIUS, -CRANIUM_RADIUS),
                worldPosition.offset(CRANIUM_RADIUS, CRANIUM_RADIUS, CRANIUM_RADIUS))) {
            if (pos.equals(worldPosition)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlocks.CORTEX_LOBE.get()) || state.getValue(CortexLobeBlock.PART) == CortexLobeBlock.Part.FRESH) {
                return false;
            }
        }
        return true;
    }

    /** The stage its lobes and lessons allow, one step at a time. The Cerebrum is lost if its cranium breaks. */
    public int reachableStage() {
        int awareness = awareness();
        int reached = 0;
        for (int s = 1; s <= MAX_STAGE; s++) {
            boolean ok = lobes >= STAGE_LOBES[s] && awareness >= STAGE_AWARENESS[s];
            if (s == 1) {
                ok &= knows(Lessons.WORDS);
            } else if (s == 2) {
                ok &= knows(Lessons.SELF);
            } else {
                ok &= craniumComplete();
            }
            if (!ok) {
                break;
            }
            reached = s;
        }
        return reached;
    }

    private void checkStage(ServerLevel level) {
        int reachable = reachableStage();
        if (stage == MAX_STAGE && reachable < MAX_STAGE) {
            setStage(level, MAX_STAGE - 1);
            formCerebrum(level, false);
            say("stage.lost");
            return;
        }
        if (reachable > stage) {
            setStage(level, stage + 1);
            if (stage == MAX_STAGE) {
                formCerebrum(level, true);
            }
            level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.2f, 0.5f + 0.1f * stage);
            level.playSound(null, worldPosition, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 0.4f, 1.5f);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                    worldPosition.getZ() + 0.5, 80, 1.2, 1.2, 1.2, 0.2);
            say("stage." + stage, displayName());
        }
    }

    private void setStage(ServerLevel level, int newStage) {
        stage = newStage;
        setChanged();
        BlockState state = getBlockState();
        if (state.hasProperty(ChronocortexBlock.STAGE)) {
            level.setBlockAndUpdate(worldPosition, state.setValue(ChronocortexBlock.STAGE, stage));
        }
    }

    /** The cranium closes over the jar (or opens again): its lobes become the Cerebrum, an eye on every face. */
    private void formCerebrum(ServerLevel level, boolean form) {
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-CRANIUM_RADIUS, -CRANIUM_RADIUS, -CRANIUM_RADIUS),
                worldPosition.offset(CRANIUM_RADIUS, CRANIUM_RADIUS, CRANIUM_RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlocks.CORTEX_LOBE.get())) {
                continue;
            }
            int dx = Math.abs(pos.getX() - worldPosition.getX());
            int dy = Math.abs(pos.getY() - worldPosition.getY());
            int dz = Math.abs(pos.getZ() - worldPosition.getZ());
            // The middle of each face of the cube: one coordinate at the edge, the other two centred.
            boolean faceCentre = (dx == CRANIUM_RADIUS ? 1 : 0) + (dy == CRANIUM_RADIUS ? 1 : 0) + (dz == CRANIUM_RADIUS ? 1 : 0) == 1
                    && (dx == 0 || dx == CRANIUM_RADIUS) && (dy == 0 || dy == CRANIUM_RADIUS) && (dz == 0 || dz == CRANIUM_RADIUS);
            CortexLobeBlock.Part part = !form ? CortexLobeBlock.Part.MATURE
                    : faceCentre ? CortexLobeBlock.Part.CEREBRUM_EYE : CortexLobeBlock.Part.CEREBRUM;
            if (state.getValue(CortexLobeBlock.PART) != part && (form || state.getValue(CortexLobeBlock.PART).formed())) {
                level.setBlockAndUpdate(pos.immutable(), state.setValue(CortexLobeBlock.PART, part));
            }
        }
    }

    // ---------------------------------------------------------------- saving

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("stage", stage);
        tag.putInt("lobes", lobes);
        ListTag learned = new ListTag();
        lessons.forEach(id -> learned.add(StringTag.valueOf(id.toString())));
        tag.put("lessons", learned);
        if (name != null) {
            tag.putString("name", name);
        }
        CompoundTag nicks = new CompoundTag();
        nicknames.forEach((id, nickname) -> nicks.putString(id.toString(), nickname));
        tag.put("nicknames", nicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stage = Math.min(MAX_STAGE, tag.getInt("stage"));
        lobes = tag.getInt("lobes");
        lessons.clear();
        for (Tag entry : tag.getList("lessons", Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getAsString());
            if (id != null) {
                lessons.add(id);
            }
        }
        name = tag.contains("name") ? tag.getString("name") : null;
        nicknames.clear();
        CompoundTag nicks = tag.getCompound("nicknames");
        for (String key : nicks.getAllKeys()) {
            try {
                nicknames.put(UUID.fromString(key), nicks.getString(key));
            } catch (IllegalArgumentException ignored) {
                // not a player id
            }
        }
    }
}
