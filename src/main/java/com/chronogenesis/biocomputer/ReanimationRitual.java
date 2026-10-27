package com.chronogenesis.biocomputer;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.registry.ModBlocks;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * "It's alive!" (early-game.md, Hours 3.5-4.5). Lightning striking near a Dissection Table that has lightning
 * rods on its frame jolts the brain. If the brain has soaked up enough Awareness and Time it wakes, and the
 * table becomes the Chronocortex. If not, the brain burns. A thunderstorm will find the rods by itself; a
 * Channeling trident works too.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class ReanimationRitual {
    /** How far from the strike (the rod's tip) a table is still jolted. */
    private static final int REACH = 3;
    public static final ResourceLocation ADVANCEMENT = ChronoGenesis.id("its_alive");

    private ReanimationRitual() {}

    @SubscribeEvent
    static void onLightning(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LightningBolt bolt && event.getLevel() instanceof ServerLevel level && !event.loadedFromDisk()) {
            BlockPos strike = bolt.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(strike.offset(-REACH, -REACH, -REACH), strike.offset(REACH, REACH, REACH))) {
                if (level.getBlockEntity(pos) instanceof DissectionTableBlockEntity table && table.hasBrain() && table.rods() > 0) {
                    jolt(level, pos.immutable(), table);
                }
            }
        }
    }

    private static void jolt(ServerLevel level, BlockPos pos, DissectionTableBlockEntity table) {
        if (!table.isReady()) {
            // Not enough of a mind to hold the current: the brain cooks.
            level.setBlockAndUpdate(pos, level.getBlockState(pos).setValue(DissectionTableBlock.BRAIN, false));
            table.clear();
            level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 20, 0.2, 0.2, 0.2, 0.02);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.6f);
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(Whispers.HEARING_RANGE))) {
                player.displayClientMessage(Component.translatable("message.chronogenesis.table.burnt")
                        .withStyle(ChatFormatting.GRAY), false);
            }
            return;
        }
        wake(level, pos);
    }

    /** The brain wakes: the table becomes the Chronocortex, stage 0. */
    public static void wake(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, ModBlocks.CHRONOCORTEX.get().defaultBlockState());
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 60, 0.6, 0.6, 0.6, 0.3);
        level.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.3, 0.4, 0.3, 0.02);
        level.playSound(null, pos, SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.BLOCKS, 1.0f, 0.5f);
        level.playSound(null, pos, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS, 1.0f, 1.0f);

        AdvancementHolder advancement = level.getServer().getAdvancements().get(ADVANCEMENT);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(Whispers.HEARING_RANGE))) {
            Whispers.say(player, "wake");
            Clues.grant(player, Clues.ALIVE);
            if (advancement != null) {
                player.getAdvancements().award(advancement, "woke");
            }
        }
    }
}
