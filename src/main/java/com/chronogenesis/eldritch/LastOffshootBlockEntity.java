package com.chronogenesis.eldritch;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.biocomputer.Whispers;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceMoteOptions;
import com.chronogenesis.registry.ModBlockEntities;
import com.chronogenesis.timetravel.ElderArena;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A Last Offshoot planted in the Cradle Stone. Linked through time to the Chronocortex (a threaded Nerve Spool), it is
 * that Chronocortex, here, with its name. Woken (used), it feels everything there is, and drinks the time out of the
 * arena's four nails in streams of motes; it grows as it drinks, swelling, mutating, opening eyes, until it bursts out of
 * its jar as the Unbegotten ({@link ElderArena#beginFight}). What it says on the way is in the lang file
 * ({@code whisper.chronogenesis.unbound.<n>}), at the ticks in {@link #LINES}.
 */
public class LastOffshootBlockEntity extends BlockEntity {
    /** How long the waking takes, from being used to the fight. */
    public static final int WAKING = 520;
    /** When it says each line while it wakes. */
    private static final int[] LINES = {0, 60, 160, 260, 360, 440};
    /** It drinks from the nails between these ticks. */
    private static final int DRINK_FROM = 100;
    private static final int DRINK_TO = 470;

    private boolean linked;
    @Nullable
    private String name;
    /** Ticks since it was woken, or -1. */
    private int waking = -1;

    public LastOffshootBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAST_OFFSHOOT.get(), pos, state);
    }

    public boolean linked() {
        return linked;
    }

    @Nullable
    public String name() {
        return name;
    }

    public int waking() {
        return waking;
    }

    /** How far grown it is (0 to 1), for drawing. */
    public float growth(float partialTick) {
        return waking < 0 ? 0 : Math.min(1.0f, (waking + partialTick) / WAKING);
    }

    /** A threaded nerve from the Chronocortex is laid into it: it is that cortex now. */
    public void link(@Nullable String cortexName) {
        linked = true;
        name = cortexName == null || cortexName.isEmpty() ? null : cortexName;
        changed();
    }

    /** Woken: it begins. */
    public void wake() {
        waking = 0;
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void say(ServerLevel level, BlockPos centre, int line) {
        Component text = Whispers.voiced(name, Component.translatable("whisper.chronogenesis.unbound." + line));
        for (ServerPlayer player : ElderArena.players(level, centre)) {
            player.sendSystemMessage(text);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, LastOffshootBlockEntity offshoot) {
        if (offshoot.waking >= 0 && offshoot.waking < WAKING) {
            offshoot.waking++;
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LastOffshootBlockEntity offshoot) {
        if (offshoot.waking < 0) {
            return;
        }
        ServerLevel server = (ServerLevel) level;
        BlockPos centre = pos.below(2);
        ElderArena.Arena arena = ElderArena.arenas(server).at(centre);
        int t = offshoot.waking++;
        for (int i = 0; i < LINES.length; i++) {
            if (t == LINES[i]) {
                offshoot.say(server, centre, i);
            }
        }
        Vec3 heart = Vec3.atCenterOf(pos).add(0, 0.2 + 2.0 * offshoot.growth(0), 0);
        if (t >= DRINK_FROM && t < DRINK_TO && t % 2 == 0) {
            // The nails' time pours into it, a stream from each.
            for (BlockPos nail : ElderArena.nails(centre)) {
                Vec3 from = Vec3.atCenterOf(nail);
                server.sendParticles(EssenceMoteOptions.of(Aspect.TIME, heart), from.x, from.y, from.z, 3, 0.3, 0.3, 0.3, 0.0);
                if (t % 20 == 0) {
                    server.sendParticles(ParticleTypes.ELECTRIC_SPARK, from.x, from.y + 0.5, from.z, 6, 0.3, 0.3, 0.3, 0.05);
                }
            }
            if (t % 40 == 0) {
                server.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 3.0f, 0.4f + t / (float) WAKING);
                server.playSound(null, pos, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 3.0f, 0.6f);
            }
        }
        if (t == DRINK_TO) {
            server.playSound(null, pos, SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 4.0f, 0.4f);
        }
        if (t >= WAKING) {
            // It bursts out of its jar.
            server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, heart.x, heart.y, heart.z, 1, 0, 0, 0, 0);
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, heart.x, heart.y, heart.z, 300, 2, 2, 2, 0.6);
            server.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3.0f, 0.5f);
            String name = offshoot.name;
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            if (arena != null) {
                ElderArena.beginFight(server, centre, name);
            }
        }
    }

    // ---------------------------------------------------------------- saving and syncing

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("linked", linked);
        if (name != null) {
            tag.putString("name", name);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        linked = tag.getBoolean("linked");
        name = tag.contains("name") ? tag.getString("name") : null;
        // Waking is not saved: interrupted, it sleeps again, and must be woken again.
        waking = tag.contains("waking") ? tag.getInt("waking") : -1;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        tag.putInt("waking", waking);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** The message when it is used: what it needs, or that it wakes. */
    public static net.minecraft.network.chat.MutableComponent hint(String key) {
        return Component.translatable("message.chronogenesis.last_offshoot." + key).withStyle(ChatFormatting.DARK_AQUA);
    }
}
