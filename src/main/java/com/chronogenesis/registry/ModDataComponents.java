package com.chronogenesis.registry;

import java.util.function.Supplier;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.lens.Blueprint;
import com.chronogenesis.lens.LensDepth;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item stack data. Never use custom NBT on stacks in 1.21.1. */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ChronoGenesis.MODID);

    /** Essence held by a syringe, vial, ampoule or jug item. */
    public static final Supplier<DataComponentType<EssenceContents>> ESSENCE = DATA_COMPONENTS.registerComponentType("essence",
            builder -> builder.persistent(EssenceContents.CODEC).networkSynchronized(EssenceContents.STREAM_CODEC));

    /** The aspect of a culture, or of the needle fitted in a syringe. */
    public static final Supplier<DataComponentType<Aspect>> ASPECT = DATA_COMPONENTS.registerComponentType("aspect",
            builder -> builder.persistent(Aspect.CODEC).networkSynchronized(Aspect.STREAM_CODEC));

    /** Where an Umbilical Locket is tied to: an organism core, in a dimension. */
    public static final Supplier<DataComponentType<GlobalPos>> LOCKET_LINK = DATA_COMPONENTS.registerComponentType("locket_link",
            builder -> builder.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));

    /** A Nerve Spool's loose end: the Fossil Cortex it took root in. */
    public static final Supplier<DataComponentType<GlobalPos>> SPOOL_END = DATA_COMPONENTS.registerComponentType("spool_end",
            builder -> builder.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));

    /** How far back a Temporal Lens looks. */
    public static final Supplier<DataComponentType<LensDepth>> LENS_DEPTH = DATA_COMPONENTS.registerComponentType("lens_depth",
            builder -> builder.persistent(LensDepth.CODEC).networkSynchronized(LensDepth.STREAM_CODEC));

    /** Which machine an Intact Blueprint shows. */
    public static final Supplier<DataComponentType<Blueprint>> BLUEPRINT = DATA_COMPONENTS.registerComponentType("blueprint",
            builder -> builder.persistent(Blueprint.CODEC).networkSynchronized(Blueprint.STREAM_CODEC));

    private ModDataComponents() {}
}
