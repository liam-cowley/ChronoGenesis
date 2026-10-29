package com.chronogenesis.player;

import java.util.ArrayList;
import java.util.List;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.item.EternalHourglassItem;
import com.chronogenesis.network.PlayerGraftsPayload;
import com.chronogenesis.registry.ModAttachments;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Grafts grown into a player's body ({@link PlayerGraft}, a player attachment kept through death): what they do, how
 * they are cut in, and how they are cut out. They show on the body ({@code client/PlayerGraftLayer}), so everyone who
 * can see a player is told what they carry.
 * <ul>
 *   <li>Cut in: hold the graft to yourself (use it) until it takes. It hurts.</li>
 *   <li>Cut out: lie on an empty Dissection Table (sneak and use it, with nothing in hand): {@value #EXCISE_COST} drams
 *       of Time from your own pool run the body back to before the last graft, which comes away whole. It hurts more.</li>
 * </ul>
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class PlayerGrafts {
    public static final int EXCISE_COST = 16;
    private static final ResourceLocation REACH = ChronoGenesis.id("graft_reach");
    private static final ResourceLocation ENTITY_REACH = ChronoGenesis.id("graft_entity_reach");
    private static final ResourceLocation ARMOR = ChronoGenesis.id("graft_armor");
    private static final ResourceLocation TOUGHNESS = ChronoGenesis.id("graft_toughness");

    private PlayerGrafts() {}

    public static List<PlayerGraft> get(Player player) {
        return player.getData(ModAttachments.PLAYER_GRAFTS);
    }

    public static boolean has(Player player, PlayerGraft graft) {
        return get(player).contains(graft);
    }

    private static void set(ServerPlayer player, List<PlayerGraft> grafts) {
        player.setData(ModAttachments.PLAYER_GRAFTS, List.copyOf(grafts));
        apply(player);
        sync(player);
    }

    private static void sync(ServerPlayer player) {
        if (player instanceof FakePlayer || player.connection == null) {
            return;
        }
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new PlayerGraftsPayload(player.getId(), get(player)));
    }

    /** Grows a graft into the body. */
    public static boolean graft(ServerPlayer player, PlayerGraft graft) {
        if (has(player, graft)) {
            return false;
        }
        List<PlayerGraft> grafts = new ArrayList<>(get(player));
        grafts.add(graft);
        set(player, grafts);
        player.hurt(player.damageSources().magic(), 4.0f);
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0, false, false));
        player.serverLevel().sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(graft.item())),
                player.getX(), player.getY(0.6), player.getZ(), 30, 0.3, 0.5, 0.3, 0.1);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ZOMBIE_INFECT, SoundSource.PLAYERS, 1.0f, 0.5f);
        player.level().playSound(null, player.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0f, 0.4f);
        player.displayClientMessage(Component.translatable("message.chronogenesis.player_graft.took." + graft.getSerializedName())
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), false);
        return true;
    }

    /** On an empty Dissection Table: the last graft is cut out, the body run back to before it. */
    public static boolean excise(ServerPlayer player, BlockPos table) {
        List<PlayerGraft> grafts = new ArrayList<>(get(player));
        if (grafts.isEmpty()) {
            return false;
        }
        if (!EternalHourglassItem.carried(player) && !Powers.spend(player, Aspect.TIME, EXCISE_COST)) {
            player.displayClientMessage(Component.translatable("message.chronogenesis.player_graft.no_time", EXCISE_COST)
                    .withStyle(ChatFormatting.GRAY), true);
            return true;
        }
        PlayerGraft graft = grafts.remove(grafts.size() - 1);
        set(player, grafts);
        player.hurt(player.damageSources().magic(), 6.0f);
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 1, false, true));
        player.getInventory().placeItemBackInInventory(new ItemStack(graft.item()));
        Vec3 at = Vec3.atCenterOf(table);
        player.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 0.6, at.z, 40, 0.5, 0.3, 0.5, 0.05);
        player.level().playSound(null, table, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0f, 0.5f);
        player.level().playSound(null, table, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 1.0f, 0.8f);
        player.displayClientMessage(Component.translatable("message.chronogenesis.player_graft.excised", graft.item().getDescription())
                .withStyle(ChatFormatting.DARK_AQUA), false);
        return true;
    }

    /** The grafts' lasting changes: reach and armour, as attribute modifiers. */
    public static void apply(Player player) {
        boolean tentacle = has(player, PlayerGraft.GRASPING_TENTACLE);
        boolean hide = has(player, PlayerGraft.UNBEGOTTEN_HIDE);
        Powers.setModifier(player, Attributes.BLOCK_INTERACTION_RANGE, REACH, tentacle ? 2.0 : 0.0, AttributeModifier.Operation.ADD_VALUE);
        Powers.setModifier(player, Attributes.ENTITY_INTERACTION_RANGE, ENTITY_REACH, tentacle ? 1.5 : 0.0, AttributeModifier.Operation.ADD_VALUE);
        Powers.setModifier(player, Attributes.ARMOR, ARMOR, hide ? 5.0 : 0.0, AttributeModifier.Operation.ADD_VALUE);
        Powers.setModifier(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS, hide ? 2.0 : 0.0, AttributeModifier.Operation.ADD_VALUE);
    }

    /** Both sides: the eye's sight (server), the tendrils' grip (each side moves its own player). */
    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        List<PlayerGraft> grafts = get(player);
        if (grafts.isEmpty()) {
            return;
        }
        if (!player.level().isClientSide() && grafts.contains(PlayerGraft.UNBLINKING_EYE)) {
            if (player.tickCount % 100 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, false));
            }
            player.removeEffect(MobEffects.BLINDNESS);
            player.removeEffect(MobEffects.DARKNESS);
        }
        if (grafts.contains(PlayerGraft.DORSAL_TENDRILS) && player.horizontalCollision && !player.onGround()
                && !player.getAbilities().flying && !player.isPassenger()) {
            // The tendrils find a hold: up the wall (or, sneaking, cling where you are).
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, player.isShiftKeyDown() ? Math.max(motion.y, 0.0) : Math.max(motion.y, 0.2), motion.z);
            player.resetFallDistance();
        }
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            apply(player);
            sync(player);
        }
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            apply(player);
            sync(player);
        }
    }

    @SubscribeEvent
    static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    /** Someone comes into view: they are told what that player carries. */
    @SubscribeEvent
    static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Player target && event.getEntity() instanceof ServerPlayer watcher
                && !get(target).isEmpty() && !(watcher instanceof FakePlayer)) {
            PacketDistributor.sendToPlayer(watcher, new PlayerGraftsPayload(target.getId(), get(target)));
        }
    }
}
