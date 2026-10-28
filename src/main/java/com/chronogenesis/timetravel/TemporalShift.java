package com.chronogenesis.timetravel;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.biocomputer.ChronocortexBlockEntity;
import com.chronogenesis.biocomputer.Clues;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.item.EternalHourglassItem;
import com.chronogenesis.network.ShiftStatusPayload;
import com.chronogenesis.player.TemporalExposure;
import com.chronogenesis.registry.ModAttachments;
import com.chronogenesis.registry.ModItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Temporal shifts (core-systems.md section 5): the Chronocortex sends a player into another era, a dimension that
 * shares the overworld's seed, so they arrive where they stood, in the land as it was then.
 * <ul>
 *   <li>It needs the cortex's stage (and lesson) for the era, the Time it costs, drawn from the wired organisms
 *       (or an Eternal Hourglass), and a <b>Chrono Tether</b> on the traveller: without one there is no way back.</li>
 *   <li>The shift holds for the era's minutes. Then time snaps the traveller back to where they left. Using the
 *       tether returns them early. Dying there ends it (they wake at home, like any death).</li>
 *   <li>Shifting is hard on an unprotected body: Temporal Exposure (less with the suit) and nausea, and while there
 *       the era pulls at every suit piece that is not Time-warded. Only a traveller warded head to foot shifts
 *       cleanly and stays the era's full minutes; with no warded piece the shift holds half of them.</li>
 * </ul>
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class TemporalShift {
    /** How far from the Chronocortex a traveller may stand. */
    public static final int REACH = 8;
    private static final int EXPOSURE = 40;
    private static final int SYNC_INTERVAL = 20;
    /** Ticks between the era's pulls at an unwarded traveller (an exposure per unwarded piece). */
    private static final int DRIFT_INTERVAL = 100;

    /** Where a traveller came from, which era they are in, and how long it holds. Not shifted: {@link #NONE}. */
    public record Shift(ResourceKey<Level> home, Vec3 homePos, ResourceLocation era, int ticksLeft) {
        public static final Shift NONE = new Shift(Level.OVERWORLD, Vec3.ZERO, ChronoGenesis.id("none"), 0);
        public static final Codec<Shift> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceKey.codec(Registries.DIMENSION).fieldOf("home").forGetter(Shift::home),
                Vec3.CODEC.fieldOf("home_pos").forGetter(Shift::homePos),
                ResourceLocation.CODEC.fieldOf("era").forGetter(Shift::era),
                Codec.INT.fieldOf("ticks_left").forGetter(Shift::ticksLeft)
        ).apply(instance, Shift::new));

        public boolean active() {
            return ticksLeft > 0;
        }

        Shift tick() {
            return new Shift(home, homePos, era, ticksLeft - 1);
        }
    }

    /** What a shift into an era still needs. */
    public record Check(boolean stage, boolean lesson, boolean memory, boolean tether, boolean time, boolean here) {
        public boolean ok() {
            return stage && lesson && memory && tether && time && here;
        }
    }

    private TemporalShift() {}

    public static Shift get(Player player) {
        return player.getData(ModAttachments.TIME_SHIFT);
    }

    public static boolean hasTether(Player player) {
        return player.getInventory().contains(stack -> stack.is(ModItems.CHRONO_TETHER.get()));
    }

    /** Works on both sides: the screen uses the essence and lessons it was sent. */
    public static Check check(Player player, Era era, int cortexStage, java.util.Collection<ResourceLocation> lessons,
            java.util.Collection<ResourceLocation> memories, Map<Aspect, Integer> essence) {
        boolean hourglass = EternalHourglassItem.carried(player);
        return new Check(cortexStage >= era.stage(),
                era.lesson().map(lessons::contains).orElse(true),
                era.memory().map(memories::contains).orElse(true)
                        && (era.routes().isEmpty() || era.routes().stream().anyMatch(route -> route.open(lessons, memories))),
                hasTether(player),
                hourglass || essence.getOrDefault(Aspect.TIME, 0) >= era.cost(),
                !get(player).active() && player.level().dimension() != era.dimension());
    }

    /** The Chronocortex sends the player. Says why not, if it cannot. */
    public static boolean begin(ServerPlayer player, ChronocortexBlockEntity cortex, ResourceLocation id) {
        Era era = Eras.get(player.registryAccess(), id);
        if (era == null) {
            return false;
        }
        Check check = check(player, era, cortex.stage(), cortex.lessons(), cortex.memories(), cortex.availableEssence());
        if (!check.ok()) {
            tell(player, !check.tether() ? "message.chronogenesis.shift.no_tether"
                    : !check.time() ? "message.chronogenesis.shift.no_time" : "message.chronogenesis.shift.cannot");
            return false;
        }
        ServerLevel there = player.server.getLevel(era.dimension());
        if (there == null) {
            tell(player, "message.chronogenesis.shift.unreachable");
            return false;
        }
        if (!EternalHourglassItem.carried(player)) {
            cortex.drawEssence(Aspect.TIME, era.cost());
        }
        int warded = TemporalExposure.wardedPieces(player);
        Shift shift = new Shift(player.level().dimension(), player.position(), id, duration(era, warded));
        ServerLevel here = player.serverLevel();
        here.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 1.0f, 0.5f);
        here.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY(1.0), player.getZ(), 80, 0.4, 1.0, 0.4, 0.1);
        cortex.say("shift", era.title());

        player.setData(ModAttachments.TIME_SHIFT, shift);
        travel(player, there, player.getX(), player.getZ(), player.getYRot(), player.getXRot());
        player.displayClientMessage(Component.translatable("message.chronogenesis.shift.arrived", era.title(),
                Math.max(1, shift.ticksLeft() / 1200)).withStyle(ChatFormatting.DARK_AQUA), false);
        if (warded < 4) {
            // An unwarded body is wrenched through, and the era lets go of it sooner.
            TemporalExposure.expose(player, EXPOSURE);
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false));
            tell(player, "message.chronogenesis.shift.unwarded");
        }
        era.clue().ifPresent(clue -> Clues.grant(player, clue));
        sync(player);
        return true;
    }

    /**
     * How long a shift holds: the era's minutes for a traveller Time-warded head to foot, half of them with no warded
     * piece, an eighth more for each warded piece between.
     */
    public static int duration(Era era, int wardedPieces) {
        return era.minutes() * 60 * 20 * (4 + wardedPieces) / 8;
    }

    /** Back to where the shift began. */
    public static void returnHome(ServerPlayer player, String message) {
        Shift shift = get(player);
        if (!shift.active()) {
            return;
        }
        player.setData(ModAttachments.TIME_SHIFT, Shift.NONE);
        ServerLevel home = player.server.getLevel(shift.home());
        if (home == null) {
            home = player.server.overworld();
        }
        player.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY(1.0), player.getZ(),
                60, 0.4, 1.0, 0.4, 0.1);
        Vec3 pos = shift.homePos();
        home.getChunk((int) Math.floor(pos.x) >> 4, (int) Math.floor(pos.z) >> 4);
        player.resetFallDistance();
        if (player instanceof FakePlayer) {
            player.moveTo(pos.x, pos.y, pos.z, player.getYRot(), player.getXRot());
        } else {
            player.teleportTo(home, pos.x, pos.y, pos.z, player.getYRot(), player.getXRot());
        }
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false));
        home.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.4f);
        player.displayClientMessage(Component.translatable(message).withStyle(ChatFormatting.DARK_AQUA), false);
        sync(player);
    }

    /** Into a level at x, z: onto the ground there (generating it if needed), softly. */
    private static void travel(ServerPlayer player, ServerLevel level, double x, double z, float yRot, float xRot) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        level.getChunk(bx >> 4, bz >> 4);
        double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz) + 0.1;
        if (y <= level.getMinBuildHeight()) {
            y = level.getSeaLevel() + 1;
        }
        player.resetFallDistance();
        if (player instanceof FakePlayer) {
            player.moveTo(x, y, z, yRot, xRot);
        } else {
            player.teleportTo(level, x, y, z, yRot, xRot);
        }
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false));
    }

    // ---------------------------------------------------------------- the countdown

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Shift shift = get(player);
        if (!shift.active()) {
            return;
        }
        Era era = Eras.get(player.registryAccess(), shift.era());
        if (era == null || player.level().dimension() != era.dimension()) {
            // Left the era some other way (or it is gone from the data): the shift is over.
            player.setData(ModAttachments.TIME_SHIFT, Shift.NONE);
            sync(player);
            return;
        }
        if (player.tickCount % DRIFT_INTERVAL == 0) {
            // The era pulls at every piece of the traveller that is not warded against it.
            TemporalExposure.exposeUnshielded(player, 4 - TemporalExposure.wardedPieces(player));
        }
        Shift next = shift.tick();
        player.setData(ModAttachments.TIME_SHIFT, next);
        if (next.ticksLeft() == 600 || next.ticksLeft() == 200) {
            player.displayClientMessage(Component.translatable("message.chronogenesis.shift.thinning", next.ticksLeft() / 20)
                    .withStyle(ChatFormatting.GOLD), true);
            player.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.6f, 0.5f);
        }
        if (!next.active()) {
            // Keep the attachment active for returnHome to read.
            player.setData(ModAttachments.TIME_SHIFT, new Shift(shift.home(), shift.homePos(), shift.era(), 1));
            returnHome(player, "message.chronogenesis.shift.snapped_back");
        } else if (next.ticksLeft() % SYNC_INTERVAL == 0) {
            sync(player);
        }
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.setData(ModAttachments.TIME_SHIFT, Shift.NONE);
            sync(player);
        }
    }

    static void sync(ServerPlayer player) {
        if (player instanceof FakePlayer || player.connection == null) {
            return;
        }
        Shift shift = get(player);
        Era era = shift.active() ? Eras.get(player.registryAccess(), shift.era()) : null;
        ShiftStatusPayload payload = new ShiftStatusPayload(shift.ticksLeft(), era == null ? Component.empty() : era.title());
        if (player.connection.hasChannel(payload)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    private static void tell(Player player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), false);
    }

    @Nullable
    public static Era currentEra(Player player) {
        Shift shift = get(player);
        return shift.active() ? Eras.get(player.registryAccess(), shift.era()) : null;
    }
}
