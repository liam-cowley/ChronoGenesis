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
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
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

    /** A Nerve Spool threaded through a swollen (level 3) gland: its nerve can reach through time. */
    public static final Supplier<DataComponentType<Boolean>> SPOOL_THREADED = DATA_COMPONENTS.registerComponentType("spool_threaded",
            builder -> builder.persistent(com.mojang.serialization.Codec.BOOL).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL));

    /** A threaded spool's loose end, rooted in the Chronocortex: the cortex's name (empty if it has none). */
    public static final Supplier<DataComponentType<String>> SPOOL_CORTEX = DATA_COMPONENTS.registerComponentType("spool_cortex",
            builder -> builder.persistent(com.mojang.serialization.Codec.STRING).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8));

    /** How far back a Temporal Lens looks. */
    public static final Supplier<DataComponentType<LensDepth>> LENS_DEPTH = DATA_COMPONENTS.registerComponentType("lens_depth",
            builder -> builder.persistent(LensDepth.CODEC).networkSynchronized(LensDepth.STREAM_CODEC));

    /** An era the Temporal Lens looks into instead of its depth (one the holder has visited). */
    public static final Supplier<DataComponentType<ResourceLocation>> LENS_ERA = DATA_COMPONENTS.registerComponentType("lens_era",
            builder -> builder.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));

    /** Which fragment of the Precursors' story a tablet holds (see lore/PrecursorLore). */
    public static final Supplier<DataComponentType<ResourceLocation>> LORE = DATA_COMPONENTS.registerComponentType("lore",
            builder -> builder.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));

    /** Which dinosaur a fossil is of, or an egg holds (an entity type's id). */
    public static final Supplier<DataComponentType<ResourceLocation>> DINO = DATA_COMPONENTS.registerComponentType("dino",
            builder -> builder.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));

    /** Which machine an Intact Blueprint shows. */
    public static final Supplier<DataComponentType<Blueprint>> BLUEPRINT = DATA_COMPONENTS.registerComponentType("blueprint",
            builder -> builder.persistent(Blueprint.CODEC).networkSynchronized(Blueprint.STREAM_CODEC));

    /** A Chrono-Insulated Suit piece infused with Time at an Enhancing Gland: Time-warded (and grown tendrils). */
    public static final Supplier<DataComponentType<Unit>> TEMPORAL_WARD = DATA_COMPONENTS.registerComponentType("temporal_ward",
            builder -> builder.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

    private ModDataComponents() {}
}
