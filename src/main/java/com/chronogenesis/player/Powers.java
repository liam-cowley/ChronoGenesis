package com.chronogenesis.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.network.SyncPlayerEssencePayload;
import com.chronogenesis.organism.OrganismCoreBlockEntity;
import com.chronogenesis.registry.ModAttachments;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Level 1 powers (core-systems.md section 3), fuelled by the player's essence pools.
 * <ul>
 *   <li>Density, Iron Fists: bare hands mine what an iron pickaxe can, at stone-pickaxe speed.
 *       1 dram per block.</li>
 *   <li>Awareness: mobs and non-sneaking players within 20 blocks glow (rendered client-side).</li>
 *   <li>Speed: +20% movement and attack speed per level.</li>
 *   <li>Size: shrink or grow one step.</li>
 *   <li>Time, Stutter: a 3-block bubble where everything moves at half speed for 3 seconds.</li>
 * </ul>
 * Pools only refill at an Enhancing Gland (until the Umbilical Locket exists).
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class Powers {
    private static final ResourceLocation SPEED_MOD = ChronoGenesis.id("power_speed");
    private static final ResourceLocation ATTACK_SPEED_MOD = ChronoGenesis.id("power_attack_speed");
    private static final ResourceLocation SIZE_MOD = ChronoGenesis.id("power_size");

    public static final double AWARENESS_RADIUS = 20.0;
    private static final float IRON_FIST_SPEED = 4.0f; // a stone pickaxe's mining speed
    private static final int STUTTER_COST = 8;
    private static final int STUTTER_TICKS = 60;
    private static final double STUTTER_RADIUS = 3.0;
    private static final String STUTTERED_TAG = "chronogenesis_stuttered";

    /** Active Stutter bubbles: player id -> game time the bubble ends. */
    private static final Map<UUID, Long> STUTTERS = new HashMap<>();

    private Powers() {}

    public static PlayerEssence get(Player player) {
        return player.getData(ModAttachments.PLAYER_ESSENCE);
    }

    public static void set(Player player, PlayerEssence essence) {
        player.setData(ModAttachments.PLAYER_ESSENCE, essence);
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new SyncPlayerEssencePayload(essence));
        }
    }

    /** Spends drams from a pool. Returns false (and spends nothing) if there is not enough. */
    public static boolean spend(Player player, Aspect aspect, int amount) {
        PlayerEssence essence = get(player);
        if (essence.pool(aspect) < amount) {
            return false;
        }
        set(player, essence.withPool(aspect, essence.pool(aspect) - amount));
        return true;
    }

    // ---------------------------------------------------------------- filling up

    /** Enhancing Gland: fill the player's pool for the organism's aspect. */
    public static void infuseFromGland(Player player, OrganismCoreBlockEntity core, BlockPos glandPos) {
        Aspect aspect = core.aspect();
        PlayerEssence essence = get(player);
        boolean first = essence.level(aspect) == 0;
        if (first) {
            essence = essence.withLevel(aspect, 1);
        }
        int room = essence.max(aspect) - essence.pool(aspect);
        int taken = core.extract(room);
        essence = essence.withPool(aspect, essence.pool(aspect) + taken);
        if (aspect == Aspect.TIME) {
            essence = essence.withExposure(essence.exposure() + TemporalExposure.scaled(player, taken));
        }
        set(player, essence);

        player.level().playSound(null, glandPos, SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 1.0f, 0.5f);
        if (first) {
            player.displayClientMessage(Component.translatable("message.chronogenesis.gland.first." + aspect.getSerializedName())
                    .withStyle(ChatFormatting.DARK_PURPLE), false);
        } else if (taken == 0) {
            player.displayClientMessage(Component.translatable(room <= 0
                    ? "message.chronogenesis.gland.full" : "message.chronogenesis.gland.empty")
                    .withStyle(ChatFormatting.GRAY), true);
        }
    }

    /** Self-injecting Time from a Stasis Ampoule: the only way to Time powers before Time organisms. */
    public static int injectTime(Player player, int amount) {
        PlayerEssence essence = get(player);
        if (essence.level(Aspect.TIME) == 0) {
            essence = essence.withLevel(Aspect.TIME, 1);
            player.displayClientMessage(Component.translatable("message.chronogenesis.gland.first.time")
                    .withStyle(ChatFormatting.DARK_PURPLE), false);
        }
        int taken = Math.min(amount, essence.max(Aspect.TIME) - essence.pool(Aspect.TIME));
        essence = essence.withPool(Aspect.TIME, essence.pool(Aspect.TIME) + taken)
                .withExposure(essence.exposure() + TemporalExposure.scaled(player, taken * 4));
        set(player, essence);
        return taken;
    }

    // ---------------------------------------------------------------- key actions (from the client)

    public static void cycleSelected(ServerPlayer player) {
        PlayerEssence essence = get(player);
        Aspect[] aspects = Aspect.values();
        for (int i = 1; i <= aspects.length; i++) {
            Aspect next = aspects[(essence.selected().ordinal() + i) % aspects.length];
            if (essence.level(next) > 0) {
                set(player, essence.withSelected(next));
                return;
            }
        }
    }

    /** The "use power" key: toggles the selected power, or triggers it (Time), or cycles it (Size). */
    public static void use(ServerPlayer player) {
        PlayerEssence essence = get(player);
        Aspect aspect = essence.selected();
        if (essence.level(aspect) == 0) {
            player.displayClientMessage(Component.translatable("message.chronogenesis.power.unknown")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        switch (aspect) {
            case TIME -> stutter(player);
            case SIZE -> {
                int mode = essence.sizeMode() == 0 ? -1 : essence.sizeMode() == -1 ? 1 : 0;
                set(player, essence.withSizeMode(mode).withActive(Aspect.SIZE, mode != 0));
            }
            default -> {
                boolean on = !essence.isActive(aspect);
                if (on && essence.pool(aspect) <= 0) {
                    player.displayClientMessage(Component.translatable("message.chronogenesis.power.empty",
                            aspect.displayName()).withStyle(ChatFormatting.GRAY), true);
                    return;
                }
                set(player, essence.withActive(aspect, on));
            }
        }
        applyModifiers(player);
    }

    private static void stutter(ServerPlayer player) {
        if (!spend(player, Aspect.TIME, STUTTER_COST)) {
            player.displayClientMessage(Component.translatable("message.chronogenesis.power.empty",
                    Aspect.TIME.displayName()).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        PlayerEssence essence = get(player);
        set(player, essence.withExposure(essence.exposure() + TemporalExposure.scaled(player, 10)));
        STUTTERS.put(player.getUUID(), player.level().getGameTime() + STUTTER_TICKS);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0f, 0.5f);
    }

    // ---------------------------------------------------------------- per-tick upkeep

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PlayerEssence essence = get(player);
        long time = player.level().getGameTime();

        // Continuous powers burn their pool; when it runs dry they switch off.
        PlayerEssence updated = essence;
        updated = upkeep(updated, Aspect.AWARENESS, time, 60);
        updated = upkeep(updated, Aspect.SPEED, time, 40);
        updated = upkeep(updated, Aspect.SIZE, time, 60);
        if (updated.isActive(Aspect.SIZE) != essence.isActive(Aspect.SIZE) && !updated.isActive(Aspect.SIZE)) {
            updated = updated.withSizeMode(0);
        }
        // Exposure fades slowly.
        if (time % 200 == 0 && updated.exposure() > 0) {
            updated = updated.withExposure(updated.exposure() - 1);
        }
        if (updated != essence) {
            set(player, updated);
            applyModifiers(player);
        }

        TemporalExposure.tick(player, updated);
        tickStutter(player, time);
    }

    private static PlayerEssence upkeep(PlayerEssence essence, Aspect aspect, long time, int interval) {
        if (!essence.isActive(aspect) || time % interval != 0) {
            return essence;
        }
        if (essence.pool(aspect) <= 0) {
            return essence.withActive(aspect, false);
        }
        return essence.withPool(aspect, essence.pool(aspect) - 1);
    }

    private static void tickStutter(ServerPlayer player, long time) {
        Long end = STUTTERS.get(player.getUUID());
        if (end == null) {
            return;
        }
        if (time >= end) {
            STUTTERS.remove(player.getUUID());
            return;
        }
        ServerLevel level = player.serverLevel();
        AABB area = player.getBoundingBox().inflate(STUTTER_RADIUS);
        for (Entity entity : level.getEntities(player, area)) {
            if (entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 2, false, false));
            } else if (entity instanceof Projectile projectile && !projectile.getPersistentData().getBoolean(STUTTERED_TAG)) {
                projectile.getPersistentData().putBoolean(STUTTERED_TAG, true);
                projectile.setDeltaMovement(projectile.getDeltaMovement().scale(0.5));
                projectile.hurtMarked = true;
            }
        }
        if (time % 4 == 0) {
            Vec3 c = player.position();
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y + 1, c.z, 12, STUTTER_RADIUS / 2, 1.0,
                    STUTTER_RADIUS / 2, 0.0);
        }
    }

    // ---------------------------------------------------------------- attribute-based powers

    /** Speed and Size are attribute modifiers, recomputed whenever the power state changes. */
    public static void applyModifiers(Player player) {
        PlayerEssence essence = get(player);
        int speedLevel = essence.isActive(Aspect.SPEED) ? essence.level(Aspect.SPEED) : 0;
        setModifier(player, Attributes.MOVEMENT_SPEED, SPEED_MOD, 0.2 * speedLevel, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        setModifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_MOD, 0.2 * speedLevel, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        double scale = 0.0;
        if (essence.isActive(Aspect.SIZE)) {
            int level = essence.level(Aspect.SIZE);
            scale = essence.sizeMode() < 0 ? -Math.min(0.75, 0.5 + 0.1 * (level - 1)) : 0.25 * level;
        }
        setModifier(player, Attributes.SCALE, SIZE_MOD, scale, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static void setModifier(Player player, Holder<Attribute> attribute, ResourceLocation id, double amount,
            AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (amount == 0.0) {
            instance.removeModifier(id);
        } else {
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    // ---------------------------------------------------------------- Iron Fists

    /** Whether Iron Fists apply to this block right now. Works on both sides (client uses the synced copy). */
    public static boolean ironFists(Player player, BlockState state) {
        PlayerEssence essence = get(player);
        return player.getMainHandItem().isEmpty()
                && essence.isActive(Aspect.DENSITY)
                && essence.pool(Aspect.DENSITY) > 0
                && !state.is(BlockTags.INCORRECT_FOR_IRON_TOOL)
                && (state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                        || state.is(BlockTags.MINEABLE_WITH_AXE));
    }

    @SubscribeEvent
    static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (!event.canHarvest() && ironFists(event.getEntity(), event.getTargetBlock())) {
            event.setCanHarvest(true);
        }
    }

    @SubscribeEvent
    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (ironFists(event.getEntity(), event.getState())) {
            event.setNewSpeed(Math.max(event.getNewSpeed(), IRON_FIST_SPEED));
        }
    }

    @SubscribeEvent
    static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (!player.level().isClientSide() && ironFists(player, event.getState())) {
            spend(player, Aspect.DENSITY, 1);
        }
    }

    // ---------------------------------------------------------------- syncing

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        resync(event.getEntity());
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STUTTERS.remove(event.getEntity().getUUID());
        TemporalExposure.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        resync(event.getEntity());
    }

    @SubscribeEvent
    static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        resync(event.getEntity());
    }

    private static void resync(Player player) {
        set(player, get(player));
        applyModifiers(player);
    }
}
