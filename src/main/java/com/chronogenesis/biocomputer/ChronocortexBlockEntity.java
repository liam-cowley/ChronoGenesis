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
import com.chronogenesis.item.NerveSpoolItem;
import com.chronogenesis.organism.GraftBlockEntity;
import com.chronogenesis.organism.GraftType;
import com.chronogenesis.organism.OrganismCoreBlockEntity;
import com.chronogenesis.registry.ModBlockEntities;
import com.chronogenesis.registry.ModBlocks;
import com.chronogenesis.registry.ModDataComponents;
import com.chronogenesis.registry.ModSounds;
import com.chronogenesis.timetravel.Era;
import com.chronogenesis.timetravel.Eras;
import com.chronogenesis.timetravel.FossilCortexBlock;
import com.chronogenesis.timetravel.FossilCortexBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
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
import net.minecraft.world.phys.Vec3;

/**
 * The Chronocortex (Temporal Control Unit), core-systems.md section 5.
 * <ul>
 *   <li>Essence interface: organisms with a Ganglion graft within its wiring range (32 blocks, more as it grows)
 *       are wired to it. It reports their levels, and warns before a wired Time organism bursts.</li>
 *   <li>Research: costs are drawn from the essence stored in wired organisms (see {@link Research}).</li>
 *   <li>Growth: it grows lobes ({@link CortexLobeBlock}) into Brass Armature built round its jar, one every few
 *       seconds, for {@value #LOBE_COST} drams of Awareness from its wired organisms. Each lobe is room in its mind.
 *       It only grows within its structure (see {@link #structure}): a 5 x 5 x 3 box, the jar the middle block of
 *       its bottom layer.</li>
 *   <li>Lessons: show it things ({@link Lesson}) and it learns what they mean, if it has the room and is old
 *       enough. Lessons make it aware.</li>
 *   <li>Stages: each stage from 2 is a shell of lobes round the last, and a multiblock: stage 2 is the 3 x 3 x 2
 *       box over the jar, stage 3 the 5 x 5 x 3 box round that (two of whose blocks may be Time Capsules). When a
 *       shell is whole its lobes stop being blocks and merge into one body (client/CortexBrainRenderer). Stage 1
 *       needs words, stage 2 a name; all need awareness ({@link #STAGE_AWARENESS}). Break a shell and it falls back.
 *       </li>
 *   <li>The way in: sneaking, empty-handed, a player opens a corridor through its body to the jar, where its sense of
 *       time is: only from the jar can it shift anyone into another era. Its Time Capsules let it reach further
 *       back.</li>
 *   <li>Speech: once it has words, it answers what is said near it ({@link CortexSpeech}).</li>
 * </ul>
 */
public class ChronocortexBlockEntity extends BlockEntity {
    /** How far its nerves reach, by stage: Ganglions this close are wired to it. */
    private static final int[] WIRE_RANGES = {32, 48, 64, 96};
    /** Lobes and awareness needed for each stage (stages 2 and 3 also need their structure whole). */
    public static final int[] STAGE_LOBES = {0, 4, 17, 72};
    public static final int[] STAGE_AWARENESS = {0, 4, 9, 13};
    public static final int MAX_STAGE = 3;
    public static final int LOBE_COST = 4;
    /** Awareness each remembered era gives it. */
    public static final int MEMORY_AWARENESS = 3;
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
    /** Creatures it has learned from their fossils, in the order learned: it can bud their eggs. */
    private final List<ResourceLocation> species = new ArrayList<>();
    private int nextSpecies;
    /** Eras it remembers: an offshoot it budded grew through each, and was linked back. */
    private final Set<ResourceLocation> memories = new LinkedHashSet<>();
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

    /** A lesson learned, or an era remembered (dialogue and eras check both the same way). */
    public boolean knows(ResourceLocation lesson) {
        return lessons.contains(lesson) || memories.contains(lesson);
    }

    public Set<ResourceLocation> memories() {
        return memories;
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
        int total = MEMORY_AWARENESS * memories.size();
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
            if (core.formed()) {
                essence.merge(core.aspect(), core.stored(), Integer::sum);
            }
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

    /**
     * Something used on it (the jar, or any of its lobes or capsules: once it has a body, the jar is inside it):
     * a Nerve Spool lays a nerve to a fossil; anything else may be a lesson.
     */
    public ItemInteractionResult use(ItemStack stack, Player player) {
        if (stack.getItem() instanceof NerveSpoolItem) {
            return link(stack, player);
        }
        if (stack.getItem() instanceof com.chronogenesis.item.FossilItem || (stack.is(net.minecraft.world.item.Items.EGG) && !species.isEmpty())) {
            return bud(stack, player);
        }
        return teach(stack, player);
    }

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
        sync();
        ServerLevel serverLevel = (ServerLevel) level;
        serverLevel.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 0.6f);
        serverLevel.sendParticles(ParticleTypes.ENCHANT, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2,
                worldPosition.getZ() + 0.5, 40, 0.4, 0.4, 0.4, 0.6);
        say(lesson.learned());
        checkStage(serverLevel);
        return ItemInteractionResult.SUCCESS;
    }

    // ---------------------------------------------------------------- eggs from fossils

    /** Drams of Size an egg is budded from (its own flesh, drawn from its organisms). */
    public static final int EGG_COST = 16;

    public List<ResourceLocation> species() {
        return species;
    }

    /**
     * Shown a fossil, it learns the creature and buds an egg of it from its own matter; shown a hen's egg, it buds
     * another of a creature it knows (each in turn). Each egg costs {@link #EGG_COST} drams of Size.
     */
    public ItemInteractionResult bud(ItemStack stack, Player player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return ItemInteractionResult.SUCCESS;
        }
        ResourceLocation kind;
        boolean fossil = stack.getItem() instanceof com.chronogenesis.item.FossilItem;
        if (fossil) {
            kind = stack.get(ModDataComponents.DINO.get());
            if (kind == null) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
        } else {
            kind = species.get(nextSpecies % species.size());
        }
        boolean hourglass = com.chronogenesis.item.EternalHourglassItem.carried(player);
        if (!hourglass && availableEssence().getOrDefault(Aspect.SIZE, 0) < EGG_COST) {
            say("egg.no_matter", EGG_COST);
            return ItemInteractionResult.CONSUME;
        }
        net.minecraft.world.entity.EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(kind).orElse(null);
        if (type == null) {
            return ItemInteractionResult.CONSUME;
        }
        if (fossil && !species.contains(kind)) {
            species.add(kind);
            sync();
            say("egg.learned", type.getDescription());
        } else {
            say("egg.budded", type.getDescription());
        }
        if (!fossil) {
            nextSpecies = (nextSpecies + 1) % species.size();
        }
        if (!hourglass) {
            drawEssence(Aspect.SIZE, EGG_COST);
        }
        stack.consume(1, player);
        setChanged();
        net.minecraft.world.entity.item.ItemEntity egg = new net.minecraft.world.entity.item.ItemEntity(serverLevel,
                worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, com.chronogenesis.entity.DinoEggItem.of(type));
        serverLevel.addFreshEntity(egg);
        serverLevel.playSound(null, worldPosition, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 1.0f, 0.5f);
        serverLevel.sendParticles(ParticleTypes.ITEM_SLIME, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                worldPosition.getZ() + 0.5, 20, 0.4, 0.3, 0.4, 0.05);
        return ItemInteractionResult.SUCCESS;
    }

    // ---------------------------------------------------------------- memories of other eras

    /**
     * A Nerve Spool bound to a Fossil Cortex is used on it: the nerve is laid between them, however far, and the
     * fossil's era becomes a memory. Whoever laid it can understand the people of that era from then on.
     */
    public ItemInteractionResult link(ItemStack spool, Player player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return ItemInteractionResult.SUCCESS;
        }
        GlobalPos end = spool.get(ModDataComponents.SPOOL_END.get());
        if (end == null && NerveSpoolItem.threaded(spool)) {
            // A threaded nerve takes root in the cortex itself, to be carried back through time.
            if (spool.has(ModDataComponents.SPOOL_CORTEX.get())) {
                player.displayClientMessage(Component.translatable("message.chronogenesis.spool.cortex_already").withStyle(ChatFormatting.GRAY), true);
                return ItemInteractionResult.CONSUME;
            }
            spool.set(ModDataComponents.SPOOL_CORTEX.get(), name == null ? "" : name);
            serverLevel.playSound(null, worldPosition, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 1.0f, 0.4f);
            say("spool_rooted");
            return ItemInteractionResult.SUCCESS;
        }
        if (end == null) {
            player.displayClientMessage(Component.translatable("message.chronogenesis.spool.loose").withStyle(ChatFormatting.GRAY), false);
            return ItemInteractionResult.CONSUME;
        }
        if (end.dimension() != serverLevel.dimension()) {
            player.displayClientMessage(Component.translatable("message.chronogenesis.spool.other_world").withStyle(ChatFormatting.GRAY), false);
            return ItemInteractionResult.CONSUME;
        }
        if (!(serverLevel.getBlockEntity(end.pos()) instanceof FossilCortexBlockEntity fossil) || fossil.era() == null || fossil.linked()) {
            player.displayClientMessage(Component.translatable("message.chronogenesis.spool.nothing").withStyle(ChatFormatting.GRAY), false);
            spool.remove(ModDataComponents.SPOOL_END.get());
            return ItemInteractionResult.CONSUME;
        }
        ResourceLocation era = fossil.era();
        memories.add(era);
        sync();
        setChanged();
        serverLevel.setBlockAndUpdate(end.pos(), fossil.getBlockState().setValue(FossilCortexBlock.AWAKE, true));
        spool.consume(1, player);
        // The nerve pulses along its length, both ends crackling.
        net.minecraft.world.phys.Vec3 from = worldPosition.getCenter();
        net.minecraft.world.phys.Vec3 to = end.pos().getCenter();
        int points = (int) Math.min(64, from.distanceTo(to));
        for (int i = 0; i <= points; i++) {
            net.minecraft.world.phys.Vec3 at = from.lerp(to, points == 0 ? 0 : (double) i / points);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 0.5, at.z, 2, 0.1, 0.1, 0.1, 0.0);
        }
        serverLevel.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 0.4f);
        serverLevel.playSound(null, end.pos(), SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 0.4f);
        Era known = Eras.get(serverLevel.registryAccess(), era);
        say("memory", known == null ? Component.literal(era.toString()) : known.title());
        if (era.equals(com.chronogenesis.ChronoGenesis.id("hadean_eon"))) {
            // It has seen the world unmade: it knows what is before it, and wants to go.
            say("last_offshoot.prompt");
        }
        if (player instanceof ServerPlayer serverPlayer) {
            Research.set(serverPlayer, Research.get(serverPlayer).withCompleted(memoryOf(era)));
        }
        checkStage(serverLevel);
        return ItemInteractionResult.SUCCESS;
    }

    /** The topic marking that a player linked the memory of an era (and so understands its people). */
    public static ResourceLocation memoryOf(ResourceLocation era) {
        return ResourceLocation.fromNamespaceAndPath(era.getNamespace(), "memory/" + era.getPath());
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
            cortex.grow(serverLevel, false);
            cortex.checkStage(serverLevel);
        }
        if (cortex.tick % 10 == 3) {
            cortex.tendPath(serverLevel);
        }
        if (cortex.tick == 2) {
            // Lobes left from an older shape of the cortex (or a world from before shells) are put right.
            cortex.shapeStructure(serverLevel);
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
     * The Eternal Hourglass poured into it: every wired organism filled, and a firm lobe grown into every bit of
     * armature it can reach, at once and for nothing. Then it grows up as far as its lessons allow.
     */
    public Component saturate(ServerLevel level) {
        rescan(level);
        int fed = 0;
        for (OrganismCoreBlockEntity core : wiredCores()) {
            fed += core.feed(core.room());
        }
        int before = lobes;
        for (int i = 0; i < 1000 && grow(level, true); i++) {
            // one lobe per pass
        }
        for (int s = 0; s < MAX_STAGE; s++) {
            checkStage(level);
        }
        return Component.translatable("message.chronogenesis.hourglass.cortex", fed, lobes - before);
    }

    /**
     * The lobes it has, found by walking from the jar through touching lobes (within its structure); and one more grown
     * into the nearest bare armature that touches them, if its organisms can spare the Awareness ({@code free}:
     * without asking, and already firm). Returns whether a lobe grew.
     */
    private boolean grow(ServerLevel level, boolean free) {
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
        boolean grew = false;
        if (frame != null && (free || availableEssence().getOrDefault(Aspect.AWARENESS, 0) >= LOBE_COST)) {
            if (!free) {
                drawEssence(Aspect.AWARENESS, LOBE_COST);
            }
            level.setBlockAndUpdate(frame, ModBlocks.CORTEX_LOBE.get().defaultBlockState()
                    .setValue(CortexLobeBlock.PART, free ? CortexLobeBlock.Part.MATURE : CortexLobeBlock.Part.FRESH));
            grew = true;
            level.playSound(null, frame, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 0.7f, 0.6f);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, frame.getX() + 0.5, frame.getY() + 0.5, frame.getZ() + 0.5,
                    10, 0.3, 0.3, 0.3, 0.05);
            count++;
        }
        if (count != lobes) {
            lobes = count;
            sync();
            setChanged();
        }
        return grew;
    }

    /** It only grows (and counts lobes) within its last stage's structure. */
    private boolean withinReach(BlockPos pos) {
        return inStructure(worldPosition, pos, MAX_STAGE);
    }

    /** How far a stage's structure reaches out from the jar to each side (and up, less one): 0 for the jar alone. */
    public static int structureRadius(int stage) {
        return Math.max(0, stage - 1);
    }

    /**
     * Every block of a stage's structure, the jar's own included: a box standing on the jar's layer, the jar the middle
     * block of its bottom layer. Stage 2: 3 x 3 x 2. Stage 3: 5 x 5 x 3.
     */
    public static Iterable<BlockPos> structure(BlockPos jar, int stage) {
        int r = structureRadius(stage);
        return BlockPos.betweenClosed(jar.offset(-r, 0, -r), jar.offset(r, r, r));
    }

    public static boolean inStructure(BlockPos jar, BlockPos pos, int stage) {
        int r = structureRadius(stage);
        int dy = pos.getY() - jar.getY();
        return Math.abs(pos.getX() - jar.getX()) <= r && Math.abs(pos.getZ() - jar.getZ()) <= r && dy >= 0 && dy <= r;
    }

    /** Whether a block is in the shell a stage adds round the last: where a Time Capsule may stand (stage 3). */
    public static boolean inShell(BlockPos jar, BlockPos pos, int stage) {
        return inStructure(jar, pos, stage) && !inStructure(jar, pos, stage - 1);
    }

    /** Whether lobes (firm ones) fill every block of a stage's structure but the jar; Time Capsules may stand in its last shell. */
    public boolean structureComplete(int stage) {
        if (level == null || stage < 2) {
            return stage < 2;
        }
        for (BlockPos pos : structure(worldPosition, stage)) {
            if (pos.equals(worldPosition)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            boolean lobe = state.is(ModBlocks.CORTEX_LOBE.get()) && state.getValue(CortexLobeBlock.PART) != CortexLobeBlock.Part.FRESH;
            boolean capsule = stage == MAX_STAGE && state.is(ModBlocks.TIME_CAPSULE.get()) && inShell(worldPosition, pos, MAX_STAGE);
            if (!lobe && !capsule) {
                return false;
            }
        }
        return true;
    }

    /** Time Capsules built into its last shell, once it is whole: each lets it reach further back. */
    public int capsules() {
        if (level == null || stage < MAX_STAGE) {
            return 0;
        }
        int count = 0;
        for (BlockPos pos : structure(worldPosition, MAX_STAGE)) {
            if (inShell(worldPosition, pos, MAX_STAGE) && level.getBlockState(pos).is(ModBlocks.TIME_CAPSULE.get())) {
                count++;
            }
        }
        return count;
    }

    /** The stage its lobes and lessons allow, one step at a time. A stage is lost if its structure breaks. */
    public int reachableStage() {
        int awareness = awareness();
        int reached = 0;
        for (int s = 1; s <= MAX_STAGE; s++) {
            boolean ok = lobes >= STAGE_LOBES[s] && awareness >= STAGE_AWARENESS[s];
            if (s == 1) {
                ok &= knows(Lessons.WORDS);
            } else if (s == 2) {
                ok &= knows(Lessons.SELF) && structureComplete(2);
            } else {
                ok &= structureComplete(3);
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
        if (stage >= 2 && reachable < stage) {
            // A shell broke: it shrinks back to what still stands, and its lobes are blocks again.
            setStage(level, Math.max(1, reachable));
            closePath(level);
            shapeStructure(level);
            say("stage.lost");
            return;
        }
        if (reachable > stage) {
            setStage(level, stage + 1);
            shapeStructure(level);
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

    /**
     * Its lobes and capsules as its stage has them: inside its whole structure they are part of its body (not drawn as
     * blocks, see {@link CortexLobeBlock#drawnByCortex}); outside it (grown ahead into the next shell, or left from
     * an older shape) they are lobes in their frames again.
     */
    private void shapeStructure(ServerLevel level) {
        int reach = CortexLobeBlock.REACH;
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-reach, -reach, -reach), worldPosition.offset(reach, reach, reach))) {
            BlockState state = level.getBlockState(pos);
            boolean inside = stage >= 2 && inStructure(worldPosition, pos, stage);
            if (state.is(ModBlocks.CORTEX_LOBE.get())) {
                CortexLobeBlock.Part part = state.getValue(CortexLobeBlock.PART);
                CortexLobeBlock.Part wanted = inside && part != CortexLobeBlock.Part.FRESH ? CortexLobeBlock.Part.CEREBRUM
                        : part.formed() ? CortexLobeBlock.Part.MATURE : part;
                if (wanted != part || (!inside && state.getValue(CortexLobeBlock.OPEN))) {
                    level.setBlockAndUpdate(pos.immutable(), state.setValue(CortexLobeBlock.PART, wanted).setValue(CortexLobeBlock.OPEN, false));
                }
            } else if (state.is(ModBlocks.TIME_CAPSULE.get())) {
                boolean formed = inside && stage == MAX_STAGE;
                if (state.getValue(TimeCapsuleBlock.FORMED) != formed) {
                    level.setBlockAndUpdate(pos.immutable(), state.setValue(TimeCapsuleBlock.FORMED, formed));
                }
            }
        }
    }

    /** For the creative Chronocortex and tests: sets its stage outright and shapes its structure to it. */
    public void forceStage(ServerLevel level, int newStage) {
        setStage(level, Math.max(0, Math.min(MAX_STAGE, newStage)));
        closePath(level);
        shapeStructure(level);
        sync();
    }

    // ---------------------------------------------------------------- the way in

    /** How long a corridor stays open once nobody is in it, in ticks. */
    public static final int PATH_TICKS = 1200;

    /** The corridor through its body to the jar, if one is open: the side it opens on. Synced. */
    @Nullable
    private Direction path;
    private long pathUntil;

    @Nullable
    public Direction path() {
        return path;
    }

    /** The blocks a corridor on {@code side} opens: from beside the jar out through its structure, two high. */
    public static List<BlockPos> pathBlocks(BlockPos jar, int stage, Direction side) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int d = 1; d <= structureRadius(stage); d++) {
            for (int y = 0; y <= 1; y++) {
                blocks.add(jar.relative(side, d).above(y));
            }
        }
        return blocks;
    }

    /** The corridor's space (and the jar's), to see who is in it. */
    private AABB pathSpace() {
        AABB space = new AABB(worldPosition);
        if (path != null) {
            for (BlockPos pos : pathBlocks(worldPosition, stage, path)) {
                space = space.minmax(new AABB(pos));
            }
        }
        return space;
    }

    private boolean someoneInPath() {
        return level != null && path != null && !level.getEntitiesOfClass(Player.class, pathSpace()).isEmpty();
    }

    /**
     * Sneak-used with an empty hand: opens a corridor through its body to the jar on the side the player stands (or the
     * nearest side that is all lobe), or closes the one that is open, if nobody is in it.
     */
    public void togglePath(ServerPlayer player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (stage < 2) {
            player.displayClientMessage(Component.translatable("message.chronogenesis.cortex.path_needless").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (path != null) {
            if (someoneInPath() || pathSpace().contains(player.position())) {
                player.displayClientMessage(Component.translatable("message.chronogenesis.cortex.path_occupied").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            closePath(serverLevel);
            return;
        }
        double dx = player.getX() - (worldPosition.getX() + 0.5);
        double dz = player.getZ() - (worldPosition.getZ() + 0.5);
        List<Direction> sides = new ArrayList<>(List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST));
        sides.sort(java.util.Comparator.comparingDouble(side -> -(side.getStepX() * dx + side.getStepZ() * dz)));
        for (Direction side : sides) {
            List<BlockPos> blocks = pathBlocks(worldPosition, stage, side);
            if (blocks.stream().allMatch(pos -> level.getBlockState(pos).is(ModBlocks.CORTEX_LOBE.get()))) {
                for (BlockPos pos : blocks) {
                    level.setBlockAndUpdate(pos, level.getBlockState(pos).setValue(CortexLobeBlock.OPEN, true));
                }
                path = side;
                pathUntil = serverLevel.getGameTime() + PATH_TICKS;
                BlockPos mouth = worldPosition.relative(side, structureRadius(stage));
                serverLevel.playSound(null, mouth, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 1.4f, 0.5f);
                serverLevel.playSound(null, mouth, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 1.0f, 0.4f);
                serverLevel.sendParticles(ParticleTypes.ITEM_SLIME, mouth.getX() + 0.5, mouth.getY() + 1.0, mouth.getZ() + 0.5,
                        30, 0.4, 0.6, 0.4, 0.05);
                say("path.open");
                setChanged();
                sync();
                return;
            }
        }
        player.displayClientMessage(Component.translatable("message.chronogenesis.cortex.path_blocked").withStyle(ChatFormatting.GRAY), true);
    }

    /** Closes the corridor: its lobes close back over it. */
    private void closePath(ServerLevel level) {
        if (path == null) {
            return;
        }
        for (BlockPos pos : pathBlocks(worldPosition, stage, path)) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.CORTEX_LOBE.get()) && state.getValue(CortexLobeBlock.OPEN)) {
                level.setBlockAndUpdate(pos, state.setValue(CortexLobeBlock.OPEN, false));
            }
        }
        level.playSound(null, worldPosition.relative(path, structureRadius(stage)), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 1.2f, 0.4f);
        path = null;
        setChanged();
        sync();
    }

    /** It keeps the corridor while anyone is in it, and closes it a minute after they leave. */
    private void tendPath(ServerLevel level) {
        if (path == null) {
            return;
        }
        boolean intact = stage >= 2 && pathBlocks(worldPosition, stage, path).stream()
                .allMatch(pos -> level.getBlockState(pos).is(ModBlocks.CORTEX_LOBE.get()));
        if (someoneInPath()) {
            pathUntil = level.getGameTime() + PATH_TICKS;
        } else if (!intact || level.getGameTime() >= pathUntil) {
            closePath(level);
        }
    }

    /**
     * Whether a player can reach its sense of time: the jar itself. While it is only a jar, from anywhere near; once it
     * has a body, only from inside it, at the end of an open corridor.
     */
    public boolean reachableCore(Player player) {
        double distance = player.position().distanceTo(Vec3.atBottomCenterOf(worldPosition));
        if (stage < 2) {
            return distance <= com.chronogenesis.timetravel.TemporalShift.REACH;
        }
        return path != null && distance <= structureRadius(stage) + 1.5;
    }

    // ---------------------------------------------------------------- what it sounds like

    /** From its third stage it crackles with thought, and something in it calls, in whispers. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, ChronocortexBlockEntity cortex) {
        if (cortex.stage < MAX_STAGE) {
            return;
        }
        net.minecraft.util.RandomSource random = level.getRandom();
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 4;
        double y = pos.getY() + 1.5 + random.nextDouble() * 1.5;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 4;
        if (random.nextInt(50) == 0) {
            level.playLocalSound(x, y, z, ModSounds.CORTEX_CRACKLE.get(), SoundSource.BLOCKS, 0.6f + random.nextFloat() * 0.3f,
                    0.8f + random.nextFloat() * 0.4f, false);
        }
        if (random.nextInt(260) == 0) {
            level.playLocalSound(x, y, z, ModSounds.CORTEX_WHISPER.get(), SoundSource.AMBIENT, 0.9f, 0.55f + random.nextFloat() * 0.2f, false);
        }
    }

    // ---------------------------------------------------------------- what clients see

    /**
     * How much it knows: lessons, eras remembered (twice over), creatures learned. The more, the stranger it looks
     * (client/CortexBrainRenderer).
     */
    public int knowledge() {
        return lessons.size() + 2 * memories.size() + species.size();
    }

    /** Tells clients what it has become (its look follows its lobes and its knowledge). */
    private void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
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
        ListTag remembered = new ListTag();
        memories.forEach(id -> remembered.add(StringTag.valueOf(id.toString())));
        tag.put("memories", remembered);
        CompoundTag nicks = new CompoundTag();
        nicknames.forEach((id, nickname) -> nicks.putString(id.toString(), nickname));
        tag.put("nicknames", nicks);
        ListTag learnedSpecies = new ListTag();
        species.forEach(id -> learnedSpecies.add(StringTag.valueOf(id.toString())));
        tag.put("species", learnedSpecies);
        tag.putInt("next_species", nextSpecies);
        if (path != null) {
            tag.putString("path", path.getSerializedName());
            tag.putLong("path_until", pathUntil);
        }
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
        memories.clear();
        for (Tag entry : tag.getList("memories", Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getAsString());
            if (id != null) {
                memories.add(id);
            }
        }
        species.clear();
        for (Tag entry : tag.getList("species", Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getAsString());
            if (id != null) {
                species.add(id);
            }
        }
        nextSpecies = tag.getInt("next_species");
        path = tag.contains("path") ? Direction.byName(tag.getString("path")) : null;
        pathUntil = tag.getLong("path_until");
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
