package com.chronogenesis.organism;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.registry.ModBlockEntities;
import com.chronogenesis.registry.ModDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A placeable jug holding up to 64 drams of one glass-safe aspect. Keeps its contents as an item. */
public class EssenceJugBlockEntity extends BlockEntity {
    public static final int CAPACITY = 64;

    @Nullable
    private EssenceContents contents;

    public EssenceJugBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ESSENCE_JUG.get(), pos, state);
    }

    @Nullable
    public EssenceContents contents() {
        return contents;
    }

    public int space(Aspect aspect) {
        if (!aspect.fitsInGlass()) {
            return 0;
        }
        if (contents == null) {
            return CAPACITY;
        }
        return contents.aspect() == aspect ? CAPACITY - contents.amount() : 0;
    }

    public int insert(Aspect aspect, int amount) {
        int moved = Math.min(amount, space(aspect));
        if (moved > 0) {
            contents = new EssenceContents(aspect, (contents == null ? 0 : contents.amount()) + moved);
            changed();
        }
        return moved;
    }

    public int extract(int amount) {
        if (contents == null) {
            return 0;
        }
        int moved = Math.min(amount, contents.amount());
        contents = contents.amount() - moved <= 0 ? null : contents.withAmount(contents.amount() - moved);
        changed();
        return moved;
    }

    public int comparatorSignal() {
        return contents == null ? 0 : Math.max(1, Mth.floor(15.0f * contents.amount() / CAPACITY));
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    // Keep contents when the jug is broken and placed again (as an item data component).

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        contents = input.get(ModDataComponents.ESSENCE.get());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (contents != null) {
            builder.set(ModDataComponents.ESSENCE.get(), contents);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("contents");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (contents != null) {
            EssenceContents.CODEC.encodeStart(NbtOps.INSTANCE, contents).ifSuccess(nbt -> tag.put("contents", nbt));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        contents = tag.contains("contents")
                ? EssenceContents.CODEC.parse(NbtOps.INSTANCE, tag.get("contents")).result().orElse(null)
                : null;
    }
}
