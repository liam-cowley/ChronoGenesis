package com.chronogenesis.registry;

import java.util.function.Supplier;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.biocomputer.ChronocortexBlockEntity;
import com.chronogenesis.biocomputer.DissectionTableBlockEntity;
import com.chronogenesis.organism.EssenceJugBlockEntity;
import com.chronogenesis.organism.GraftBlockEntity;
import com.chronogenesis.organism.OrganismCoreBlockEntity;
import com.chronogenesis.time.TimeTearBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ChronoGenesis.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<OrganismCoreBlockEntity>> ORGANISM_CORE = BLOCK_ENTITY_TYPES.register("organism_core",
            () -> BlockEntityType.Builder.of(OrganismCoreBlockEntity::new, ModBlocks.ORGANISM_CORE.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<GraftBlockEntity>> GRAFT = BLOCK_ENTITY_TYPES.register("graft",
            () -> BlockEntityType.Builder.of(GraftBlockEntity::new,
                    ModBlocks.TAP.get(), ModBlocks.MAW.get(), ModBlocks.LEECH_TENDRIL.get(), ModBlocks.ENHANCING_GLAND.get(),
                    ModBlocks.STASIS_BLADDER.get(), ModBlocks.STARTING_GATE.get(), ModBlocks.FINISH.get(),
                    ModBlocks.HEART.get(), ModBlocks.EYE.get(), ModBlocks.CLOCK.get(), ModBlocks.GANGLION.get(),
                    ModBlocks.TEMPORAL_FIELD.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<EssenceJugBlockEntity>> ESSENCE_JUG = BLOCK_ENTITY_TYPES.register("essence_jug",
            () -> BlockEntityType.Builder.of(EssenceJugBlockEntity::new, ModBlocks.ESSENCE_JUG.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<TimeTearBlockEntity>> TIME_TEAR = BLOCK_ENTITY_TYPES.register("time_tear",
            () -> BlockEntityType.Builder.of(TimeTearBlockEntity::new, ModBlocks.TIME_TEAR.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<DissectionTableBlockEntity>> DISSECTION_TABLE =
            BLOCK_ENTITY_TYPES.register("dissection_table", () -> BlockEntityType.Builder.of(
                    DissectionTableBlockEntity::new, ModBlocks.DISSECTION_TABLE.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<ChronocortexBlockEntity>> CHRONOCORTEX =
            BLOCK_ENTITY_TYPES.register("chronocortex", () -> BlockEntityType.Builder.of(
                    ChronocortexBlockEntity::new, ModBlocks.CHRONOCORTEX.get()).build(null));

    private ModBlockEntities() {}
}
