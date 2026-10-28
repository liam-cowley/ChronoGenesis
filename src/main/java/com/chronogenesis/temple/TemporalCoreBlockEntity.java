package com.chronogenesis.temple;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import com.chronogenesis.biocomputer.Clues;
import com.chronogenesis.registry.ModBlockEntities;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * A temple's Temporal Core: while it runs, nothing within its barrier can be placed or broken ({@link TempleBarrier}).
 * Every seal broken in the temple weakens it; at {@value #SEALS_NEEDED} it stops, the barrier falls, and the core
 * itself can be taken.
 */
public class TemporalCoreBlockEntity extends BlockEntity {
    /** Seals that must be broken: any of the temple's five, so there is more than one way in. */
    public static final int SEALS_NEEDED = 3;
    /** The barrier: this far around the core, and from a little below it to the temple's top. */
    public static final int RADIUS = 18;
    public static final int BELOW = 3;
    public static final int ABOVE = 24;

    static final Set<TemporalCoreBlockEntity> LOADED = Collections.newSetFromMap(new WeakHashMap<>());

    private int sealsBroken;

    public TemporalCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEMPORAL_CORE.get(), pos, state);
    }

    public boolean active() {
        return getBlockState().getValue(TemporalCoreBlock.ACTIVE);
    }

    public int sealsBroken() {
        return sealsBroken;
    }

    /** The space its barrier holds. */
    public AABB barrier() {
        return new AABB(worldPosition.getX() - RADIUS, worldPosition.getY() - BELOW, worldPosition.getZ() - RADIUS,
                worldPosition.getX() + RADIUS + 1, worldPosition.getY() + ABOVE + 1, worldPosition.getZ() + RADIUS + 1);
    }

    public boolean holds(BlockPos pos) {
        return active() && barrier().contains(pos.getCenter());
    }

    /** A seal in its temple was broken. */
    void sealBroken(ServerLevel level) {
        sealsBroken++;
        setChanged();
        level.playSound(null, worldPosition, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.5f, 0.5f + 0.15f * sealsBroken);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, barrier().inflate(8))) {
            player.displayClientMessage(Component.translatable("message.chronogenesis.temple.seal_broken",
                    sealsBroken, SEALS_NEEDED).withStyle(ChatFormatting.GOLD), true);
        }
        if (sealsBroken >= SEALS_NEEDED && active()) {
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(TemporalCoreBlock.ACTIVE, false));
            level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 2.0f, 0.4f);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5,
                    200, 2.0, 2.0, 2.0, 0.3);
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, barrier().inflate(8))) {
                player.displayClientMessage(Component.translatable("message.chronogenesis.temple.barrier_down")
                        .withStyle(ChatFormatting.DARK_AQUA), false);
                Clues.grant(player, Clues.SEALED_TEMPLE);
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            LOADED.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        LOADED.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        LOADED.remove(this);
    }

    /** The nearest core whose barrier holds a position, if any. */
    static TemporalCoreBlockEntity holding(Level level, BlockPos pos) {
        for (TemporalCoreBlockEntity core : LOADED) {
            if (!core.isRemoved() && core.getLevel() == level && core.holds(pos)) {
                return core;
            }
        }
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("seals_broken", sealsBroken);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        sealsBroken = tag.getInt("seals_broken");
    }
}
