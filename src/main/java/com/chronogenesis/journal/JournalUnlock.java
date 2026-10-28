package com.chronogenesis.journal;

import com.chronogenesis.ChronoGenesis;
import com.mojang.brigadier.Command;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Creative mode only: unlocks the whole Experimenter's Journal at once. Journal entries open as their
 * advancements are earned, so this grants every ChronoGenesis advancement. Two ways in: using the Eternal
 * Hourglass in creative mode, or {@code /chronogenesis journal unlock} (creative players, or operators).
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class JournalUnlock {
    private JournalUnlock() {}

    /** Grants every remaining criterion of every ChronoGenesis advancement. Returns how many were completed. */
    public static int unlockAll(ServerPlayer player) {
        int completed = 0;
        for (AdvancementHolder advancement : player.server.getAdvancements().getAllAdvancements()) {
            if (!advancement.id().getNamespace().equals(ChronoGenesis.MODID)) {
                continue;
            }
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
            if (progress.isDone()) {
                continue;
            }
            for (String criterion : progress.getRemainingCriteria()) {
                player.getAdvancements().award(advancement, criterion);
            }
            completed++;
        }
        player.displayClientMessage(Component.translatable("message.chronogenesis.journal.unlocked", completed)
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return completed;
    }

    @SubscribeEvent
    static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(ChronoGenesis.MODID)
                .then(Commands.literal("journal")
                        .then(Commands.literal("unlock")
                                .requires(JournalUnlock::allowed)
                                .executes(context -> {
                                    unlockAll(context.getSource().getPlayerOrException());
                                    return Command.SINGLE_SUCCESS;
                                }))));
    }

    /** Creative players may unlock their own journal; operators may anyway. */
    private static boolean allowed(CommandSourceStack source) {
        return source.hasPermission(Commands.LEVEL_GAMEMASTERS)
                || source.getPlayer() != null && source.getPlayer().isCreative();
    }
}
