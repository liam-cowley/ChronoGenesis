package com.chronogenesis.entity;

import javax.annotation.Nullable;

import com.chronogenesis.registry.ModBlockEntities;
import com.chronogenesis.registry.ModDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** What hatches from a dinosaur's egg ({@code dino} component on the item), and how much Time it has been fed. */
public class DinoEggBlockEntity extends BlockEntity {
    @Nullable
    private ResourceLocation species;
    private int fed;

    public DinoEggBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DINO_EGG.get(), pos, state);
    }

    @Nullable
    public ResourceLocation species() {
        return species;
    }

    public void setSpecies(@Nullable ResourceLocation species) {
        this.species = species;
        setChanged();
    }

    public int fed() {
        return fed;
    }

    public void feed(int drams) {
        fed += drams;
        setChanged();
    }

    /** It cracks open: the hatchling, young, remembering whoever hatched it. */
    public void hatch(ServerLevel level, @Nullable Player hatcher) {
        EntityType<?> type = species == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(species).orElse(null);
        level.setBlockAndUpdate(worldPosition, Blocks.AIR.defaultBlockState());
        level.playSound(null, worldPosition, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS, 1.0f, 0.6f);
        level.sendParticles(ParticleTypes.POOF, worldPosition.getX() + 0.5, worldPosition.getY() + 0.4, worldPosition.getZ() + 0.5,
                10, 0.2, 0.2, 0.2, 0.02);
        if (type != null && type.create(level) instanceof Mob mob && mob instanceof Dinos.Hatchling young) {
            mob.moveTo(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5, level.random.nextFloat() * 360.0f, 0.0f);
            Dinos.hatchling(mob, young, hatcher);
            level.addFreshEntity(mob);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        species = input.get(ModDataComponents.DINO.get());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (species != null) {
            builder.set(ModDataComponents.DINO.get(), species);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (species != null) {
            tag.putString("species", species.toString());
        }
        tag.putInt("fed", fed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        species = tag.contains("species") ? ResourceLocation.tryParse(tag.getString("species")) : null;
        fed = tag.getInt("fed");
    }
}
