package com.chronogenesis.registry;

import java.util.function.Supplier;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.biomech.OrganismCoreBlockEntity;
import com.chronogenesis.essence.EssenceJugBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ChronoGenesis.MODID);

    @SuppressWarnings("DataFlowIssue") // the data fixer type is unused by mods
    public static final Supplier<BlockEntityType<OrganismCoreBlockEntity>> ORGANISM_CORE = BLOCK_ENTITY_TYPES.register("organism_core",
            () -> BlockEntityType.Builder.of(OrganismCoreBlockEntity::new, ModBlocks.ORGANISM_CORE.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<EssenceJugBlockEntity>> ESSENCE_JUG = BLOCK_ENTITY_TYPES.register("essence_jug",
            () -> BlockEntityType.Builder.of(EssenceJugBlockEntity::new, ModBlocks.ESSENCE_JUG.get()).build(null));

    private ModBlockEntities() {}
}
