package com.chronogenesis.player;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.network.SyncPlayerEssencePayload;
import com.chronogenesis.organism.OrganismCoreBlockEntity;
import com.chronogenesis.registry.ModAttachments;
import com.chronogenesis.registry.ModItems;
import com.chronogenesis.time.TimeZones;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Player powers (core-systems.md section 3), fuelled by the player's essence pools. See {@link Power} for the list.
 * <ul>
 *   <li>Passive powers are switched on (on the Essence Manipulator's wheel) and burn their pools slowly while on.
 *       Several can run at once; when a pool runs dry, the powers drinking from it switch off.</li>
 *   <li>One active power is selected; using it (right-click with the Manipulator, or the power key) fires it.</li>
 * </ul>
 * Pools only refill at an Enhancing Gland (or through an Umbilical Locket). How deep a gland can fill you is its
 * level ({@link com.chronogenesis.organism.GlandBlock}).
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class Powers {
    private static final ResourceLocation SPEED_MOD = ChronoGenesis.id("power_speed");
    private static final ResourceLocation ATTACK_SPEED_MOD = ChronoGenesis.id("power_attack_speed");
    private static final ResourceLocation SIZE_MOD = ChronoGenesis.id("power_size");
    private static final ResourceLocation STEP_MOD = ChronoGenesis.id("power_step");
    private static final ResourceLocation FALL_MOD = ChronoGenesis.id("power_safe_fall");
    private static final ResourceLocation GRAVITY_MOD = ChronoGenesis.id("power_gravity");
    private static final ResourceLocation JUMP_MOD = ChronoGenesis.id("power_jump");
    private static final ResourceLocation FALL_DAMAGE_MOD = ChronoGenesis.id("power_fall_damage");

    public static final double AWARENESS_RADIUS = 20.0;
    /** The highest power level there is: a gland grows to level 3 (Deepest Infusion). */
    public static final int MAX_LEVEL = 3;
    /** Mining speed of Iron Fists by level: a stone, an iron and a diamond pickaxe's. */
    private static final float[] FIST_SPEED = {4.0f, 6.0f, 8.0f};

    private Powers() {}

    public static PlayerEssence get(Player player) {
        return player.getData(ModAttachments.PLAYER_ESSENCE);
    }

    public static void set(Player player, PlayerEssence essence) {
        player.setData(ModAttachments.PLAYER_ESSENCE, essence);
        SyncPlayerEssencePayload payload = new SyncPlayerEssencePayload(essence);
        // Fake players (machines from other mods, test players) have no client to sync to.
        if (player instanceof ServerPlayer serverPlayer && !(serverPlayer instanceof FakePlayer)
                && serverPlayer.connection != null && serverPlayer.connection.hasChannel(payload)) {
            PacketDistributor.sendToPlayer(serverPlayer, payload);
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

    /** Spends the same drams from every pool a power drinks from, all or nothing. Says so if a pool is short. */
    public static boolean spend(Player player, Power power, int each) {
        PlayerEssence essence = get(player);
        for (Aspect aspect : power.aspects()) {
            if (essence.pool(aspect) < each) {
                empty(player, aspect);
                return false;
            }
        }
        for (Aspect aspect : power.aspects()) {
            essence = essence.withPool(aspect, essence.pool(aspect) - each);
        }
        set(player, essence);
        return true;
    }

    static void empty(Player player, Aspect aspect) {
        player.displayClientMessage(Component.translatable("message.chronogenesis.power.empty", aspect.displayName())
                .withStyle(ChatFormatting.GRAY), true);
    }

    /** Adds Temporal Exposure (reduced by the suit). */
    static void expose(Player player, int amount) {
        PlayerEssence essence = get(player);
        int added = TemporalExposure.scaled(player, amount);
        if (added > 0) {
            set(player, essence.withExposure(essence.exposure() + added));
        }
    }

    // ---------------------------------------------------------------- filling up

    /**
     * Enhancing Gland: fills the player's pool for the organism's aspect, and raises their power level to the
     * gland's level. With a Confluence Needle and a {@code partner} organism (wired by veins, another essence), the
     * other essence flows in too, and the player learns the composite power of the two.
     */
    public static void infuseFromGland(Player player, OrganismCoreBlockEntity core, BlockPos glandPos, int glandLevel,
            @Nullable OrganismCoreBlockEntity partner) {
        Aspect aspect = core.aspect();
        PlayerEssence essence = get(player);
        int before = essence.level(aspect);
        boolean first = before == 0;
        boolean deeper = !first && before < glandLevel;
        if (before < glandLevel) {
            essence = essence.withLevel(aspect, glandLevel);
        }
        int room = essence.max(aspect) - essence.pool(aspect);
        int taken = core.extract(room);
        essence = fill(player, essence, aspect, taken);

        Power learned = null;
        boolean unknownOther = false;
        if (partner != null && partner.aspect() != aspect) {
            Aspect other = partner.aspect();
            if (essence.level(other) == 0) {
                unknownOther = true;
            } else {
                int otherTaken = partner.extract(essence.max(other) - essence.pool(other));
                essence = fill(player, essence, other, otherTaken);
                taken += otherTaken;
                Power composite = Power.composite(aspect, other);
                if (composite != null && !essence.composites().contains(composite)) {
                    essence = essence.withComposite(composite);
                    learned = composite;
                }
            }
        }
        set(player, essence);

        player.level().playSound(null, glandPos, SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 1.0f, 0.5f);
        if (first) {
            say(player, Component.translatable("message.chronogenesis.gland.first." + aspect.getSerializedName()));
            if (!hasManipulator(player)) {
                say(player, Component.translatable("message.chronogenesis.gland.manipulator_hint"));
            }
        } else if (deeper) {
            say(player, Component.translatable("message.chronogenesis.gland.deeper", aspect.displayName(), glandLevel));
            applyModifiers(player);
        }
        if (learned != null) {
            say(player, Component.translatable("message.chronogenesis.gland.composite", learned.displayName()));
            player.level().playSound(null, glandPos, SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0f, 0.6f);
        } else if (unknownOther) {
            player.displayClientMessage(Component.translatable("message.chronogenesis.gland.unknown_other",
                    partner.aspect().displayName()).withStyle(ChatFormatting.GRAY), false);
        }
        if (!first && !deeper && learned == null && taken == 0) {
            player.displayClientMessage(Component.translatable(room <= 0
                    ? "message.chronogenesis.gland.full" : "message.chronogenesis.gland.empty")
                    .withStyle(ChatFormatting.GRAY), true);
        }
    }

    private static PlayerEssence fill(Player player, PlayerEssence essence, Aspect aspect, int amount) {
        essence = essence.withPool(aspect, essence.pool(aspect) + amount);
        if (aspect == Aspect.TIME) {
            essence = essence.withExposure(essence.exposure() + TemporalExposure.scaled(player, amount));
        }
        return essence;
    }

    private static void say(Player player, Component message) {
        player.displayClientMessage(message.copy().withStyle(ChatFormatting.DARK_PURPLE), false);
    }

    /** Self-injecting Time from a Stasis Ampoule: the only way to Time powers before Time organisms. */
    public static int injectTime(Player player, int amount) {
        PlayerEssence essence = get(player);
        if (essence.level(Aspect.TIME) == 0) {
            essence = essence.withLevel(Aspect.TIME, 1);
            say(player, Component.translatable("message.chronogenesis.gland.first.time"));
        }
        int taken = Math.min(amount, essence.max(Aspect.TIME) - essence.pool(Aspect.TIME));
        essence = essence.withPool(Aspect.TIME, essence.pool(Aspect.TIME) + taken)
                .withExposure(essence.exposure() + TemporalExposure.scaled(player, taken * 4));
        set(player, essence);
        return taken;
    }

    /** Whether the player carries an Essence Manipulator (needed for the wheel). */
    public static boolean hasManipulator(Player player) {
        return player.getInventory().contains(stack -> stack.is(ModItems.ESSENCE_MANIPULATOR.get()));
    }

    // ---------------------------------------------------------------- choosing (from the wheel)

    /** The wheel picked an active power. Only powers the player has can be selected. */
    public static void select(ServerPlayer player, Power power) {
        PlayerEssence essence = get(player);
        if (!power.isPassive() && essence.knows(power) && essence.selected() != power) {
            set(player, essence.withSelected(power));
        }
    }

    /** Switches a passive power on or off. Shrink and Grow exclude each other. */
    public static void toggle(ServerPlayer player, Power power) {
        PlayerEssence essence = get(player);
        if (!power.isPassive() || !essence.knows(power)) {
            return;
        }
        boolean on = !essence.isOn(power);
        if (on) {
            for (Aspect aspect : power.aspects()) {
                if (essence.pool(aspect) <= 0) {
                    empty(player, aspect);
                    return;
                }
            }
            if (power == Power.SHRINK) {
                essence = essence.withPassive(Power.GROW, false);
            } else if (power == Power.GROW) {
                essence = essence.withPassive(Power.SHRINK, false);
            }
        }
        set(player, essence.withPassive(power, on));
        applyModifiers(player);
        player.level().playSound(null, player.blockPosition(), on ? SoundEvents.BEACON_POWER_SELECT : SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 0.4f, on ? 1.6f : 1.2f);
    }

    /** Sets a power's strength (1 to its maximum). */
    public static void setStrength(ServerPlayer player, Power power, int strength) {
        PlayerEssence essence = get(player);
        if (!essence.knows(power)) {
            return;
        }
        int clamped = Math.max(1, Math.min(power.maxStrength(essence), strength));
        set(player, essence.withStrength(power, clamped));
        applyModifiers(player);
        TimeZones.strengthChanged(player);
        player.displayClientMessage(Component.translatable("message.chronogenesis.power.strength", power.displayName(),
                clamped, power.maxStrength(essence)).withStyle(ChatFormatting.GOLD), true);
    }

    /** Nudges the selected active power's strength up or down (Shift + scroll with the Manipulator in hand). */
    public static void nudgeStrength(ServerPlayer player, int delta) {
        PlayerEssence essence = get(player);
        Power power = essence.selected();
        if (essence.knows(power)) {
            setStrength(player, power, essence.strength(power) + delta);
        }
    }

    // ---------------------------------------------------------------- using the selected active power

    public static void use(ServerPlayer player) {
        PlayerEssence essence = get(player);
        Power power = essence.selected();
        if (!essence.knows(power)) {
            player.displayClientMessage(Component.translatable("message.chronogenesis.power.unknown")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        int strength = essence.strength(power);
        switch (power) {
            case REWIND -> Rewind.use(player, strength);
            case SLOW_TIME, TIME_FREEZE -> TimeZones.toggle(player, power, strength);
            case IMPETUS -> CompositePowers.impetus(player, strength);
            case GROUND_SLAM -> CompositePowers.groundSlam(player, strength);
            case STUTTER -> CompositePowers.stutter(player, strength);
            case OVERLOOK -> CompositePowers.overlook(player, strength);
            case REGROWTH -> CompositePowers.regrowth(player, strength);
            default -> { }
        }
    }

    // ---------------------------------------------------------------- per-tick upkeep

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PlayerEssence essence = get(player);
        long time = player.level().getGameTime();

        // Passive powers burn their pools; when one runs dry, the powers drinking from it switch off.
        PlayerEssence updated = essence;
        for (Power power : essence.passives()) {
            updated = upkeep(updated, power, time);
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
        Rewind.record(player);
        CompositePowers.tick(player, updated, time);
    }

    /** Ticks between drams burned by a passive power at strength 1; stronger burns faster. 0 = paid per use. */
    private static int upkeepInterval(Power power) {
        return switch (power) {
            case IRON_FISTS, REFLEXES, PREMONITION -> 0;
            case QUICKENING -> 40;
            case FEATHERWEIGHT -> 50;
            default -> 60;
        };
    }

    private static PlayerEssence upkeep(PlayerEssence essence, Power power, long time) {
        if (!essence.knows(power)) {
            return essence.withPassive(power, false);
        }
        for (Aspect aspect : power.aspects()) {
            if (essence.pool(aspect) <= 0) {
                return essence.withPassive(power, false);
            }
        }
        int interval = upkeepInterval(power);
        if (interval == 0 || time % Math.max(10, interval / essence.strength(power)) != 0) {
            return essence;
        }
        for (Aspect aspect : power.aspects()) {
            essence = essence.withPool(aspect, essence.pool(aspect) - 1);
        }
        return essence;
    }

    // ---------------------------------------------------------------- attribute-based powers

    /** Quickening, Shrink, Grow and Scurry are attribute modifiers, recomputed whenever the power state changes. */
    public static void applyModifiers(Player player) {
        PlayerEssence essence = get(player);
        int quick = essence.isOn(Power.QUICKENING) ? essence.strength(Power.QUICKENING) : 0;
        setModifier(player, Attributes.MOVEMENT_SPEED, SPEED_MOD, 0.2 * quick, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        setModifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_MOD, 0.2 * quick, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        double scale = 0.0;
        if (essence.isOn(Power.SHRINK)) {
            scale = -Math.min(0.75, 0.5 + 0.1 * (essence.strength(Power.SHRINK) - 1));
        } else if (essence.isOn(Power.GROW)) {
            scale = 0.25 * essence.strength(Power.GROW);
        }
        setModifier(player, Attributes.SCALE, SIZE_MOD, scale, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        int scurry = essence.isOn(Power.SCURRY) ? essence.strength(Power.SCURRY) : 0;
        setModifier(player, Attributes.STEP_HEIGHT, STEP_MOD, scurry > 0 ? 0.4 + 0.3 * scurry : 0.0,
                AttributeModifier.Operation.ADD_VALUE);
        int light = featherweight(essence);
        setModifier(player, Attributes.SAFE_FALL_DISTANCE, FALL_MOD, 5.0 * scurry + 6.0 * light, AttributeModifier.Operation.ADD_VALUE);
        // Featherweight: a fall as slow as a feather's, a jump of several blocks, half the harm from landing.
        setModifier(player, Attributes.GRAVITY, GRAVITY_MOD, light > 0 ? -(0.45 + 0.1 * light) : 0.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(player, Attributes.JUMP_STRENGTH, JUMP_MOD, 0.1 * light, AttributeModifier.Operation.ADD_VALUE);
        setModifier(player, Attributes.FALL_DAMAGE_MULTIPLIER, FALL_DAMAGE_MOD, light > 0 ? -0.5 : 0.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    // ---------------------------------------------------------------- Featherweight

    /** Featherweight's strength if it is on (and there is Density left to burn), else 0. Both sides. */
    public static int featherweight(PlayerEssence essence) {
        return essence.isOn(Power.FEATHERWEIGHT) && essence.pool(Aspect.DENSITY) > 0 ? essence.strength(Power.FEATHERWEIGHT) : 0;
    }

    /** How much harder blows land on a Featherweight body: a third more at strength 1, two thirds at 2, double at 3. */
    public static float featherweightHarm(int strength) {
        return strength <= 0 ? 1.0f : 1.0f + strength / 3.0f;
    }

    /** How hard the second jump in mid-air pushes up. */
    public static double doubleJumpPower(int strength) {
        return 0.5 + 0.08 * strength;
    }

    /** The client leapt again in mid-air: a dram of Density, and the fall so far forgotten. */
    public static void doubleJumped(ServerPlayer player) {
        if (featherweight(get(player)) > 0 && !player.onGround()) {
            spend(player, Aspect.DENSITY, 1);
            player.resetFallDistance();
        }
    }

    @SubscribeEvent
    static void onFeatherweightHurt(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && !event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
            int light = featherweight(get(player));
            if (light > 0) {
                event.setAmount(event.getAmount() * featherweightHarm(light));
            }
        }
    }

    static void setModifier(Player player, Holder<Attribute> attribute, ResourceLocation id, double amount,
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

    /** Fists count as bare hands, and so does a hand in an Essence Manipulator. */
    private static boolean fist(ItemStack stack) {
        return stack.isEmpty() || stack.is(ModItems.ESSENCE_MANIPULATOR.get());
    }

    /** Whether Iron Fists apply to this block right now. Works on both sides (client uses the synced copy). */
    public static boolean ironFists(Player player, BlockState state) {
        PlayerEssence essence = get(player);
        int tier = essence.strength(Power.IRON_FISTS);
        // Strength 1: iron-tier fists. 2: diamond. 3: netherite.
        var incorrect = switch (tier) {
            case 1 -> BlockTags.INCORRECT_FOR_IRON_TOOL;
            case 2 -> BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
            default -> BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
        };
        return fist(player.getMainHandItem())
                && essence.isOn(Power.IRON_FISTS)
                && essence.pool(Aspect.DENSITY) > 0
                && !state.is(incorrect)
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
            int tier = get(event.getEntity()).strength(Power.IRON_FISTS);
            event.setNewSpeed(Math.max(event.getNewSpeed(), FIST_SPEED[Math.min(tier, FIST_SPEED.length) - 1]));
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
        CompositePowers.forget(event.getEntity().getUUID());
        Rewind.forget(event.getEntity().getUUID());
        TemporalExposure.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        resync(event.getEntity());
    }

    @SubscribeEvent
    static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Rewind.forget(event.getEntity().getUUID());
        resync(event.getEntity());
    }

    private static void resync(Player player) {
        set(player, get(player));
        applyModifiers(player);
    }
}
