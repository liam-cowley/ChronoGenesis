package com.chronogenesis.essence;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.item.EssenceContainerItem;
import com.chronogenesis.registry.ModBlockEntities;
import com.chronogenesis.registry.ModDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Stores up to {@value #CAPACITY} drams of one stable aspect. Synced to the client for the liquid tint. */
public class EssenceJugBlockEntity extends BlockEntity implements EssenceStorage {
    public static final int CAPACITY = 64;
    private static final String TAG = "Essence";

    @Nullable
    private EssenceContents contents;

    public EssenceJugBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ESSENCE_JUG.get(), pos, state);
    }

    @Override
    @Nullable
    public EssenceContents contents() {
        return contents;
    }

    @Override
    public int capacity() {
        return CAPACITY;
    }

    @Override
    public boolean accepts(Aspect aspect) {
        return EssenceContainerItem.stableOnly(aspect) && (contents == null || contents.aspect() == aspect);
    }

    @Override
    public int insert(Aspect aspect, int drams, boolean simulate) {
        if (!accepts(aspect)) {
            return 0;
        }
        int amount = Math.min(drams, CAPACITY - stored());
        if (amount > 0 && !simulate) {
            setContents(new EssenceContents(aspect, stored() + amount));
        }
        return Math.max(amount, 0);
    }

    @Override
    public int extract(int drams, boolean simulate) {
        if (contents == null) {
            return 0;
        }
        int amount = Math.min(drams, contents.drams());
        if (!simulate) {
            int left = contents.drams() - amount;
            setContents(left > 0 ? new EssenceContents(contents.aspect(), left) : null);
        }
        return amount;
    }

    private void setContents(@Nullable EssenceContents contents) {
        this.contents = contents;
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            int fill = EssenceJugBlock.fillLevel(stored());
            if (state.getValue(EssenceJugBlock.FILL) != fill) {
                level.setBlock(worldPosition, state.setValue(EssenceJugBlock.FILL, fill), Block.UPDATE_ALL);
            }
            level.sendBlockUpdated(worldPosition, state, getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        contents = tag.contains(TAG)
                ? EssenceContents.CODEC.parse(NbtOps.INSTANCE, tag.get(TAG)).result().orElse(null)
                : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (contents != null) {
            EssenceContents.CODEC.encodeStart(NbtOps.INSTANCE, contents).result().ifPresent(encoded -> tag.put(TAG, encoded));
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        contents = input.get(ModDataComponents.ESSENCE);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (contents != null) {
            components.set(ModDataComponents.ESSENCE, contents);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove(TAG);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** The liquid tint comes from here, so re-render once the client hears what is inside. */
    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        super.onDataPacket(connection, packet, registries);
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
