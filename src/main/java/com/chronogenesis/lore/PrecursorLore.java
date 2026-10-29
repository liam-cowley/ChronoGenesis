package com.chronogenesis.lore;

import java.util.List;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.registry.ModAttachments;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why the Precursors' kingdoms fell, in fragments (core-systems.md section 8, "The fall of the Precursors"). Each is
 * written on a Precursor Tablet; reading one (in their tongue) records it (on the player), and awards an advancement
 * ({@code chronogenesis:lore/<fragment>}) that opens its page in the journal. Text: {@code lore.chronogenesis.<fragment>.title}
 * and {@code .text}.
 * <p>
 * The story, for whoever pieces it together: the Nails (the obelisks) hold time in place. Every kingdom learned to
 * draw years from them, and each fell to what it drew them for: the Sun Court drank them (and the land around it
 * aged into desert), the Tide Kingdom held its sea still with them (until the sea came down all at once), the Deep
 * Kingdom dug to the Nail's point to listen (and heard), the Verdant Kingdom fed them to engines it grew (which
 * learned to want). Behind them all, a voice from before the beginning of time asked them to draw more: every dram
 * loosened the Nails that keep it out.
 */
public final class PrecursorLore {
    /** Every fragment, in reading order. */
    public static final List<String> FRAGMENTS = List.of(
            "nails_1", "nails_2", "nails_3",
            "sun_1", "sun_2", "sun_3",
            "tide_1", "tide_2", "tide_3",
            "deep_1", "deep_2", "deep_3",
            "verdant_1", "verdant_2", "verdant_3",
            "temple_1");
    /** Read all of these and it falls into place: each kingdom's fall, and the voice behind them. */
    public static final List<String> KEY = List.of("nails_3", "sun_3", "tide_3", "deep_2", "verdant_3");
    public static final String SLIP = "the_slip";

    private PrecursorLore() {}

    public static ResourceLocation id(String fragment) {
        return ChronoGenesis.id(fragment);
    }

    public static ResourceLocation advancement(String fragment) {
        return ChronoGenesis.id("lore/" + fragment);
    }

    public static Component title(ResourceLocation fragment) {
        return Component.translatable("lore." + fragment.getNamespace() + "." + fragment.getPath() + ".title");
    }

    public static Component text(ResourceLocation fragment) {
        return Component.translatable("lore." + fragment.getNamespace() + "." + fragment.getPath() + ".text");
    }

    public static boolean known(ServerPlayer player, String fragment) {
        return player.getData(ModAttachments.READ_LORE).contains(fragment);
    }

    /** The player has read a fragment: record it, and if that was the last piece, say what it all means. */
    public static void learn(ServerPlayer player, ResourceLocation fragment) {
        award(player, fragment.getPath());
        if (!known(player, SLIP) && KEY.stream().allMatch(key -> known(player, key))) {
            award(player, SLIP);
            player.displayClientMessage(Component.translatable("lore.chronogenesis.the_slip.realised")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), false);
        }
    }

    /** Remembered (the player's own record) and awarded as an advancement (which opens its journal page). */
    private static void award(ServerPlayer player, String fragment) {
        List<String> read = player.getData(ModAttachments.READ_LORE);
        if (!read.contains(fragment)) {
            List<String> more = new java.util.ArrayList<>(read);
            more.add(fragment);
            player.setData(ModAttachments.READ_LORE, List.copyOf(more));
        }
        AdvancementHolder holder = player.server.getAdvancements().get(advancement(fragment));
        if (holder != null) {
            for (String criterion : player.getAdvancements().getOrStartProgress(holder).getRemainingCriteria()) {
                player.getAdvancements().award(holder, criterion);
            }
        }
    }
}
