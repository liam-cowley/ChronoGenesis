package com.chronogenesis.registry;

import java.util.function.Supplier;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceContents;

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

    private ModDataComponents() {}
}
