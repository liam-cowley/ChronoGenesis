package com.chronogenesis.biocomputer;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.item.EternalHourglassItem;
import com.chronogenesis.network.SyncResearchPayload;
import com.chronogenesis.registry.ModAttachments;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * Research at the Chronocortex: checking a topic's costs, paying them and recording the result.
 * Topics live in the {@link #REGISTRY} datapack registry; what they unlock is checked with {@link #has}.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class Research {
    public static final ResourceKey<Registry<ResearchTopic>> REGISTRY =
            ResourceKey.createRegistryKey(ChronoGenesis.id("research"));

    /** Level 2 powers: the Enhancing Gland can fill you deeper. */
    public static final ResourceLocation LEVEL_2_POWERS = ChronoGenesis.id("level_2_powers");
    /** Level 3 powers: the gland can swell a third time (and Time Freeze). Needs a stage 2 Chronocortex. */
    public static final ResourceLocation LEVEL_3_POWERS = ChronoGenesis.id("level_3_powers");
    /** The Confluence Needle: mixing two essences at a gland. Needs a stage 1 Chronocortex. */
    public static final ResourceLocation CONFLUENCE = ChronoGenesis.id("confluence");

    private Research() {}

    @SubscribeEvent
    static void registerRegistry(DataPackRegistryEvent.NewRegistry event) {
        // Synced, so the client's research screen can list the topics.
        event.dataPackRegistry(REGISTRY, ResearchTopic.CODEC, ResearchTopic.CODEC);
    }

    // ---------------------------------------------------------------- player state

    public static PlayerResearch get(Player player) {
        return player.getData(ModAttachments.PLAYER_RESEARCH);
    }

    public static void set(Player player, PlayerResearch research) {
        player.setData(ModAttachments.PLAYER_RESEARCH, research);
        SyncResearchPayload payload = new SyncResearchPayload(research);
        // Fake players (machines from other mods, test players) have no client to sync to.
        if (player instanceof ServerPlayer serverPlayer && !(serverPlayer instanceof FakePlayer)
                && serverPlayer.connection != null && serverPlayer.connection.hasChannel(payload)) {
            PacketDistributor.sendToPlayer(serverPlayer, payload);
        }
    }

    public static boolean has(Player player, ResourceLocation topic) {
        return get(player).isCompleted(topic);
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        set(event.getEntity(), get(event.getEntity()));
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        set(event.getEntity(), get(event.getEntity()));
    }

    // ---------------------------------------------------------------- checking

    @Nullable
    public static ResearchTopic topic(RegistryAccess access, ResourceLocation id) {
        return access.registry(REGISTRY).map(registry -> registry.get(id)).orElse(null);
    }

    /**
     * Whether each cost of a topic is met. Used by the server to decide, and by the screen to show what
     * is missing.
     *
     * @param clueSet the first complete clue set (empty if the topic needs none), or null if none is complete
     */
    public record Check(boolean completed, boolean regrow, boolean prerequisites, boolean stage, boolean essence,
                        boolean materials, @Nullable List<ResourceLocation> clueSet) {
        public boolean ok() {
            if (regrow) {
                // Growing a reward again needs only the essence and materials.
                return essence && materials;
            }
            return !completed && prerequisites && stage && essence && materials && clueSet != null;
        }
    }

    public static Check check(Player player, ResourceLocation id, ResearchTopic topic, int cortexStage,
                              Map<Aspect, Integer> availableEssence) {
        PlayerResearch research = get(player);
        boolean prerequisites = topic.requires().stream().allMatch(research::isCompleted);
        // A creative Eternal Hourglass supplies any essence; otherwise it comes from the wired organisms.
        boolean essence = EternalHourglassItem.carried(player) || topic.essence().entrySet().stream()
                .allMatch(cost -> availableEssence.getOrDefault(cost.getKey(), 0) >= cost.getValue());
        boolean materials = topic.materials().stream().allMatch(material -> count(player.getInventory(), material) >= material.count());
        boolean completed = research.isCompleted(id);
        return new Check(completed, completed && !topic.rewards().isEmpty(), prerequisites, topic.stage() <= cortexStage,
                essence, materials, clueSet(research, topic));
    }

    @Nullable
    private static List<ResourceLocation> clueSet(PlayerResearch research, ResearchTopic topic) {
        if (topic.clues().isEmpty()) {
            return List.of();
        }
        for (List<ResourceLocation> set : topic.clues()) {
            if (set.stream().allMatch(research::hasClue)) {
                return set;
            }
        }
        return null;
    }

    public static int count(Inventory inventory, SizedIngredient material) {
        int total = 0;
        for (ItemStack stack : inventory.items) {
            if (material.ingredient().test(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    // ---------------------------------------------------------------- researching

    /** Pays for a topic and completes it. Returns false (and takes nothing) if anything is missing. */
    public static boolean perform(ServerPlayer player, ChronocortexBlockEntity cortex, ResourceLocation id) {
        ResearchTopic topic = topic(player.registryAccess(), id);
        if (topic == null) {
            return false;
        }
        Check check = check(player, id, topic, cortex.stage(), cortex.availableEssence());
        if (!check.ok()) {
            return false;
        }
        if (!EternalHourglassItem.carried(player)) {
            topic.essence().forEach(cortex::drawEssence);
        }
        for (SizedIngredient material : topic.materials()) {
            take(player.getInventory(), material);
        }
        if (!check.regrow()) {
            set(player, get(player).withSpent(check.clueSet()).withCompleted(id));
        }
        for (ItemStack reward : topic.rewards()) {
            player.getInventory().placeItemBackInInventory(reward.copy());
        }

        player.level().playSound(null, cortex.getBlockPos(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 0.6f);
        Whispers.say(player, "research_done", topic.title());
        return true;
    }

    private static void take(Inventory inventory, SizedIngredient material) {
        int left = material.count();
        for (ItemStack stack : inventory.items) {
            if (left <= 0) {
                return;
            }
            if (material.ingredient().test(stack)) {
                int taken = Math.min(left, stack.getCount());
                stack.shrink(taken);
                left -= taken;
            }
        }
    }
}
