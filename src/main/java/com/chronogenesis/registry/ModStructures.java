package com.chronogenesis.registry;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.world.PlacedTemplatePiece;
import com.chronogenesis.world.PlacedTemplateStructure;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Structure types and structure piece types. The structures themselves are data (worldgen/structure). */
public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, ChronoGenesis.MODID);
    public static final DeferredRegister<StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, ChronoGenesis.MODID);

    /** One template centred on its chunk, lining up across dimensions of one seed (obelisks/temples, cities/ruins). */
    public static final DeferredHolder<StructureType<?>, StructureType<PlacedTemplateStructure>> PLACED_TEMPLATE =
            STRUCTURE_TYPES.register("placed_template", () -> () -> PlacedTemplateStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> PLACED_TEMPLATE_PIECE =
            PIECE_TYPES.register("placed_template", () -> (StructurePieceType.StructureTemplateType) PlacedTemplatePiece::new);

    private ModStructures() {}
}
