package com.chronogenesis.registry;

import java.util.function.Supplier;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.essence.Needle;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item stack data. */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ChronoGenesis.MODID);

    /** Essence held by a syringe, vial, ampoule, jug or culture. Absent when empty. */
    public static final Supplier<DataComponentType<EssenceContents>> ESSENCE = DATA_COMPONENTS.registerComponentType("essence",
            builder -> builder.persistent(EssenceContents.CODEC).networkSynchronized(EssenceContents.STREAM_CODEC));

    /** The needle fitted to a Chrono Syringe. Absent when none is fitted. */
    public static final Supplier<DataComponentType<Needle>> NEEDLE = DATA_COMPONENTS.registerComponentType("needle",
            builder -> builder.persistent(Needle.CODEC).networkSynchronized(Needle.STREAM_CODEC));

    private ModDataComponents() {}
}
