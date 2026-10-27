package com.chronogenesis.biocomputer;

import java.util.List;

import com.chronogenesis.ChronoGenesis;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;

/**
 * Clues: things the player has noticed, logged in the journal and spent on research (core-systems.md
 * section 7). Until the Temporal Lens exists, clues come from witnessing the strange things your own
 * experiments do. Every clue has a name ({@code clue.<ns>.<path>}) in the lang file.
 */
public final class Clues {
    /** A creature drained of half its Density floats off the ground. */
    public static final ResourceLocation FLOATING = ChronoGenesis.id("floating_creature");
    /** A creature drained of all its Awareness stands Vacant. */
    public static final ResourceLocation VACANT = ChronoGenesis.id("vacant_mind");
    /** A creature drained of all its Time crumbles to dust. */
    public static final ResourceLocation DUST = ChronoGenesis.id("dust_to_dust");
    /** A Time Tear opens nearby. */
    public static final ResourceLocation TORN = ChronoGenesis.id("torn_time");
    /** A Racetrack pulse condenses into Time at the Finish. */
    public static final ResourceLocation LONG_ROAD = ChronoGenesis.id("the_long_road");
    /** Twin Clocks turn missed beats into Time. */
    public static final ResourceLocation HEAVY_HEART = ChronoGenesis.id("heavy_heart");
    /** The Chronocortex wakes. */
    public static final ResourceLocation ALIVE = ChronoGenesis.id("its_alive");

    public static final List<ResourceLocation> ALL = List.of(FLOATING, VACANT, DUST, TORN, LONG_ROAD, HEAVY_HEART, ALIVE);

    /** How close a player must be to notice something. */
    public static final double WITNESS_RANGE = 16.0;

    private Clues() {}

    public static Component name(ResourceLocation clue) {
        return Component.translatable("clue." + clue.getNamespace() + "." + clue.getPath());
    }

    /** Every player close enough to {@code pos} notices the clue. */
    public static void witness(ServerLevel level, BlockPos pos, ResourceLocation clue) {
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(WITNESS_RANGE))) {
            grant(player, clue);
        }
    }

    /** Logs a clue in the player's journal. Nothing happens if they already know it. */
    public static void grant(ServerPlayer player, ResourceLocation clue) {
        PlayerResearch research = Research.get(player);
        if (research.known().contains(clue)) {
            return;
        }
        Research.set(player, research.withKnown(clue));
        player.displayClientMessage(Component.translatable("message.chronogenesis.clue", name(clue))
                .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC), false);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 0.8f);
    }
}
