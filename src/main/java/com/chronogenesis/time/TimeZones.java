package com.chronogenesis.time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.network.TimeZonesPayload;
import com.chronogenesis.player.Power;
import com.chronogenesis.player.Powers;
import com.chronogenesis.player.TemporalExposure;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Chunks whose time runs at their own rate: Slow Time (the Time power at level 2) and Time Freeze (level 3).
 * <p>
 * Inside the chunk, creatures, items and projectiles, block entities (furnaces, organisms), random ticks (crops,
 * leaves) and scheduled ticks (flowing water, redstone) all run at the chunk's rate: 1/2, 1/4 or 1/8 of a tick per
 * tick when slowed (by strength), none when frozen. Players are not ticked by the server, so a player inside who
 * is not wearing the full Chrono-Insulated Suit is slowed through their attributes instead (moving, jumping,
 * falling, swinging, mining); frozen, they cannot move or act at all. A player in the full suit keeps the world's
 * time, wherever they stand: the caster, suited, walks through their own slowed chunk at normal speed.
 * <p>
 * The chunk keeps its own clock, and whoever stands in it sees its own sky (the sun crawls, or stands still).
 * Letting go (using the power again, running out of Time, or walking more than {@value #RANGE} blocks away) does
 * not snap it back: it speeds up past the world's pace and eases back down until it is exactly on the world's
 * time again, moon phase and all ({@link TimeZone#advance}).
 * <p>
 * The hooks into ticking are mixins ({@code com.chronogenesis.mixin}); this class decides how many ticks are due.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class TimeZones {
    /** The owner must stay within this many blocks of the chunk to hold it. */
    public static final int RANGE = 96;
    private static final int SLOW_START_COST = 4;
    private static final int FREEZE_START_COST = 8;
    /** A frozen chunk burns a dram of Time every this many ticks; a slowed one, slower the gentler it is. */
    private static final int FREEZE_UPKEEP = 5;

    private static final ResourceLocation SLOWED = ChronoGenesis.id("time_zone");

    private static final Map<ResourceKey<Level>, Map<Long, TimeZone>> SERVER = new HashMap<>();
    private static final Map<UUID, ServerPlayer> OWNERS = new HashMap<>();
    /** Which level each scheduled-tick list belongs to, for levels that have zones (used by the mixin). */
    private static final Map<LevelTicks<?>, ServerLevel> TICK_LISTS = new IdentityHashMap<>();
    private static final Map<Long, TimeZone> CLIENT = new HashMap<>();
    /** Ticks the client's sky is behind: the lag of the zone the viewer stands in. */
    private static long clientDayOffset;
    private static boolean extraTicking;

    private TimeZones() {}

    /** The rate of Slow Time at a strength: half, a quarter, an eighth. */
    public static double slowRate(int strength) {
        return 1.0 / (1 << Math.max(1, strength));
    }

    // ---------------------------------------------------------------- lookups (both sides)

    private static Map<Long, TimeZone> zones(Level level) {
        return level.isClientSide() ? CLIENT : SERVER.getOrDefault(level.dimension(), Map.of());
    }

    @Nullable
    public static TimeZone at(Level level, long chunk) {
        Map<Long, TimeZone> zones = zones(level);
        return zones.isEmpty() ? null : zones.get(chunk);
    }

    @Nullable
    public static TimeZone at(Level level, BlockPos pos) {
        return at(level, ChunkPos.asLong(pos));
    }

    /** Whole ticks due this game tick at a position: 1 outside zones. */
    public static int ticksAt(Level level, BlockPos pos) {
        TimeZone zone = at(level, pos);
        return zone == null ? 1 : zone.ticksNow;
    }

    public static int ticksAt(Level level, long chunk) {
        TimeZone zone = at(level, chunk);
        return zone == null ? 1 : zone.ticksNow;
    }

    public static boolean stoppedAt(Level level, BlockPos pos) {
        TimeZone zone = at(level, pos);
        return zone != null && zone.stopped();
    }

    /** The full Chrono-Insulated Suit keeps its wearer in the world's time. */
    public static boolean shielded(Player player) {
        return TemporalExposure.suitPieces(player) >= 4;
    }

    /** The time rate a player lives at right now: their zone's, unless shielded. */
    public static double playerRate(Player player) {
        if (shielded(player)) {
            return 1.0;
        }
        TimeZone zone = at(player.level(), player.blockPosition());
        return zone == null ? 1.0 : zone.rate;
    }

    /** A player stopped in a frozen chunk cannot act. */
    public static boolean frozen(Player player) {
        return playerRate(player) < TimeZone.STOPPED;
    }

    @Nullable
    private static TimeZone owned(UUID owner) {
        for (Map<Long, TimeZone> zones : SERVER.values()) {
            for (TimeZone zone : zones.values()) {
                if (owner.equals(zone.owner) && !zone.releasing) {
                    return zone;
                }
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- the powers

    /** Using Slow Time or Time Freeze: bends the chunk you stand in, or lets go of the one you hold. */
    public static void toggle(ServerPlayer player, Power power, int strength) {
        TimeZone own = owned(player.getUUID());
        double target = power == Power.TIME_FREEZE ? 0.0 : slowRate(strength);
        if (own != null) {
            if (own.power == power) {
                release(own, player, "message.chronogenesis.power.time.released");
                return;
            }
            // Switching between slowing and stopping the same chunk.
            if (!Powers.spend(player, Aspect.TIME, power == Power.TIME_FREEZE ? FREEZE_START_COST : SLOW_START_COST)) {
                tellEmpty(player);
                return;
            }
            own.power = power;
            own.target = target;
            announce(player.serverLevel(), own, power);
            return;
        }
        ServerLevel level = player.serverLevel();
        ChunkPos chunk = player.chunkPosition();
        Map<Long, TimeZone> zones = SERVER.computeIfAbsent(level.dimension(), key -> new HashMap<>());
        TimeZone existing = zones.get(chunk.toLong());
        if (existing != null && !existing.releasing) {
            player.displayClientMessage(Component.translatable("message.chronogenesis.power.time.bent")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (!Powers.spend(player, Aspect.TIME, power == Power.TIME_FREEZE ? FREEZE_START_COST : SLOW_START_COST)) {
            tellEmpty(player);
            return;
        }
        // A chunk still catching up is taken hold of again, behind as it is.
        TimeZone zone = existing != null ? existing : new TimeZone(chunk, player.getUUID(), target);
        zone.owner = player.getUUID();
        zone.power = power;
        zone.target = target;
        zone.releasing = false;
        zones.put(chunk.toLong(), zone);
        OWNERS.put(player.getUUID(), player);
        TICK_LISTS.put(level.getBlockTicks(), level);
        TICK_LISTS.put(level.getFluidTicks(), level);
        TemporalExposure.expose(player, power == Power.TIME_FREEZE ? 12 : 6);
        announce(level, zone, power);
    }

    /** A different strength for a held Slow Time: the chunk's rate follows. */
    public static void strengthChanged(ServerPlayer player) {
        TimeZone own = owned(player.getUUID());
        if (own != null && own.power == Power.SLOW_TIME) {
            own.target = slowRate(Powers.get(player).strength(Power.SLOW_TIME));
        }
    }

    private static void announce(ServerLevel level, TimeZone zone, Power power) {
        boolean freeze = power == Power.TIME_FREEZE;
        BlockPos centre = zone.chunk.getMiddleBlockPosition(0);
        ServerPlayer owner = zone.owner == null ? null : OWNERS.get(zone.owner);
        double y = owner != null ? owner.getY() + 1 : level.getSeaLevel();
        level.playSound(null, centre.getX(), y, centre.getZ(), freeze ? SoundEvents.BEACON_DEACTIVATE : SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                SoundSource.PLAYERS, 1.2f, freeze ? 0.4f : 0.6f);
        // Sparks along the chunk's edges at eye level: the seam where the two times meet.
        for (int i = 0; i <= 16; i++) {
            for (int[] edge : new int[][] {{i, 0}, {i, 16}, {0, i}, {16, i}}) {
                level.sendParticles(freeze ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.END_ROD,
                        zone.chunk.getMinBlockX() + edge[0], y, zone.chunk.getMinBlockZ() + edge[1], 1, 0, 0.6, 0, 0.0);
            }
        }
        if (owner != null) {
            owner.displayClientMessage(Component.translatable(freeze ? "message.chronogenesis.power.time.frozen"
                    : "message.chronogenesis.power.time.slowed", Math.round(1.0 / Math.max(zone.target, 1.0e-3)))
                    .withStyle(ChatFormatting.DARK_AQUA), true);
        }
    }

    private static void release(TimeZone zone, @Nullable ServerPlayer owner, @Nullable String message) {
        zone.releasing = true;
        if (zone.owner != null) {
            OWNERS.remove(zone.owner);
        }
        zone.owner = null;
        if (owner != null && message != null) {
            owner.displayClientMessage(Component.translatable(message).withStyle(ChatFormatting.DARK_AQUA), true);
            owner.level().playSound(null, owner.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 0.6f);
        }
    }

    private static void tellEmpty(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.chronogenesis.power.empty", Aspect.TIME.displayName())
                .withStyle(ChatFormatting.GRAY), true);
    }

    // ---------------------------------------------------------------- server ticking

    @SubscribeEvent
    static void onLevelTickPre(LevelTickEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Map<Long, TimeZone> zones = SERVER.get(level.dimension());
        if (zones == null) {
            return;
        }
        long time = level.getGameTime();
        for (Iterator<TimeZone> it = zones.values().iterator(); it.hasNext(); ) {
            TimeZone zone = it.next();
            if (!zone.releasing) {
                hold(level, zone, time);
            }
            if (!zone.advance()) {
                it.remove();
            }
        }
        sync(level, zones);
        if (zones.isEmpty()) {
            SERVER.remove(level.dimension());
            TICK_LISTS.remove(level.getBlockTicks());
            TICK_LISTS.remove(level.getFluidTicks());
        }
    }

    /** The owner keeps paying for it while they are near; if they cannot, it is let go. */
    private static void hold(ServerLevel level, TimeZone zone, long time) {
        ServerPlayer owner = zone.owner == null ? null : OWNERS.get(zone.owner);
        BlockPos centre = zone.chunk.getMiddleBlockPosition((int) (owner == null ? 0 : owner.getY()));
        if (owner == null || owner.isRemoved() || !owner.isAlive() || owner.level() != level
                || owner.distanceToSqr(Vec3.atCenterOf(centre)) > (double) RANGE * RANGE) {
            release(zone, owner, "message.chronogenesis.power.time.too_far");
            return;
        }
        boolean freeze = zone.power == Power.TIME_FREEZE;
        int interval = freeze ? FREEZE_UPKEEP : Math.max(10, (int) Math.round(80 * zone.target));
        if (time % interval == 0) {
            if (Powers.spend(owner, Aspect.TIME, 1)) {
                TemporalExposure.expose(owner, freeze ? 2 : 1);
            } else {
                release(zone, owner, "message.chronogenesis.power.time.ran_out");
            }
        }
    }

    /** Catching up: what is inside gets the ticks it missed, several per game tick. */
    @SubscribeEvent
    static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Map<Long, TimeZone> zones = SERVER.get(level.dimension());
        if (zones != null) {
            extraTicks(level, zones, level::tickNonPassenger);
        }
    }

    /** Runs the extra ticks of zones that are catching up (also used by the client, with its own tick method). */
    public static void extraTicks(Level level, Map<Long, TimeZone> zones, java.util.function.Consumer<Entity> tick) {
        for (TimeZone zone : zones.values()) {
            if (zone.ticksNow <= 1) {
                continue;
            }
            AABB box = new AABB(zone.chunk.getMinBlockX(), level.getMinBuildHeight(), zone.chunk.getMinBlockZ(),
                    zone.chunk.getMaxBlockX() + 1, level.getMaxBuildHeight(), zone.chunk.getMaxBlockZ() + 1);
            List<Entity> inside = level.getEntities((Entity) null, box, e -> !(e instanceof Player) && !e.isPassenger());
            for (int i = 1; i < zone.ticksNow; i++) {
                for (Entity entity : inside) {
                    if (!entity.isRemoved()) {
                        level.guardEntityTick(tick, entity);
                    }
                }
            }
        }
    }

    private static void sync(ServerLevel level, Map<Long, TimeZone> zones) {
        PacketDistributor.sendToPlayersInDimension(level, payload(zones));
    }

    private static TimeZonesPayload payload(Map<Long, TimeZone> zones) {
        List<TimeZonesPayload.Entry> entries = new ArrayList<>();
        for (TimeZone zone : zones.values()) {
            entries.add(new TimeZonesPayload.Entry(zone.chunk.toLong(), (float) zone.rate, zone.lag, zone.releasing));
        }
        return new TimeZonesPayload(entries);
    }

    // ---------------------------------------------------------------- what is inside

    /** Creatures, items and projectiles in a stopped chunk do not tick (both sides; players are slowed below). */
    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player) && ticksAt(entity.level(), entity.blockPosition()) == 0) {
            event.setCanceled(true);
        }
    }

    /** Players live at their zone's rate unless shielded by the suit: slower moving, jumping, falling, swinging. */
    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        double rate = playerRate(player);
        double factor = Math.min(rate, 2.0) - 1.0;
        var op = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        double f = Math.abs(factor) < 1.0e-3 ? 0.0 : factor;
        setModifier(player, Attributes.MOVEMENT_SPEED, f, op);
        setModifier(player, Attributes.ATTACK_SPEED, f, op);
        setModifier(player, Attributes.JUMP_STRENGTH, f, op);
        setModifier(player, Attributes.GRAVITY, f, op);
        setModifier(player, Attributes.BLOCK_BREAK_SPEED, f, op);
        if (f != 0.0 && player.tickCount % 40 == 0) {
            TemporalExposure.expose(player, 1);
        }
        if (rate < TimeZone.STOPPED && player.tickCount % 5 == 0) {
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
        }
    }

    private static void setModifier(Player player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            double amount, AttributeModifier.Operation operation) {
        var instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (amount == 0.0) {
            instance.removeModifier(SLOWED);
        } else {
            instance.addOrUpdateTransientModifier(new AttributeModifier(SLOWED, amount, operation));
        }
    }

    @SubscribeEvent
    static void onAttack(AttackEntityEvent event) {
        if (frozen(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (frozen(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (frozen(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onHitBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (frozen(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, payload(SERVER.getOrDefault(player.level().dimension(), Map.of())));
        }
    }

    @SubscribeEvent
    static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, payload(SERVER.getOrDefault(event.getTo(), Map.of())));
        }
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        SERVER.clear();
        OWNERS.clear();
        TICK_LISTS.clear();
    }

    // ---------------------------------------------------------------- called from mixins

    /**
     * A tick scheduled inside a slowed (or catching-up) chunk waits proportionally longer (or shorter). In a stopped
     * chunk it is not stretched: it waits at the moment it would run ({@code ServerLevelMixin}).
     */
    public static <T> ScheduledTick<T> stretch(LevelTicks<T> list, ScheduledTick<T> tick) {
        if (TICK_LISTS.isEmpty()) {
            return tick;
        }
        ServerLevel level = TICK_LISTS.get(list);
        if (level == null) {
            return tick;
        }
        TimeZone zone = at(level, tick.pos());
        if (zone == null || zone.stopped() || Math.abs(zone.rate - 1.0) < 1.0e-3) {
            return tick;
        }
        long now = level.getGameTime();
        long remaining = tick.triggerTick() - now;
        if (remaining <= 0) {
            return tick;
        }
        long stretched = Math.max(1, Math.round(remaining / zone.rate));
        return new ScheduledTick<>(tick.type(), tick.pos(), now + stretched, tick.priority(), tick.subTickOrder());
    }

    /** Re-entrancy guard for running extra random ticks of a chunk from inside its own tick. */
    public static boolean beginExtra() {
        if (extraTicking) {
            return false;
        }
        extraTicking = true;
        return true;
    }

    public static void endExtra() {
        extraTicking = false;
    }

    // ---------------------------------------------------------------- the client's copy

    public static Map<Long, TimeZone> clientZones() {
        return CLIENT;
    }

    public static void clientReceive(TimeZonesPayload payload) {
        Map<Long, TimeZone> next = new HashMap<>();
        for (TimeZonesPayload.Entry entry : payload.zones()) {
            TimeZone zone = CLIENT.get(entry.chunk());
            if (zone == null) {
                zone = new TimeZone(new ChunkPos(entry.chunk()), null, entry.rate());
            }
            zone.sync(entry.rate(), entry.lag(), entry.releasing());
            next.put(entry.chunk(), zone);
        }
        CLIENT.clear();
        CLIENT.putAll(next);
    }

    /** Each client tick: count each zone's whole ticks, and set the sky's offset for where the viewer stands. */
    public static void clientTick(@Nullable BlockPos viewer) {
        for (TimeZone zone : CLIENT.values()) {
            zone.clientAdvance();
        }
        TimeZone here = viewer == null || CLIENT.isEmpty() ? null : CLIENT.get(ChunkPos.asLong(viewer));
        clientDayOffset = here == null ? 0 : Math.round(here.lag);
    }

    public static void clientClear() {
        CLIENT.clear();
        clientDayOffset = 0;
    }

    /** How far behind the world's day the sky above the viewer is. */
    public static long clientDayOffset() {
        return clientDayOffset;
    }
}
