package com.chronogenesis.registry;

import java.util.function.Supplier;

import com.chronogenesis.ChronoGenesis;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sound events. What each one plays is set in assets/chronogenesis/sounds.json, so packs can swap the audio. */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, ChronoGenesis.MODID);

    /** A pen scratching a faded blueprint into the journal, read through the Temporal Lens. */
    public static final Supplier<SoundEvent> LENS_SCRIBBLE = SOUND_EVENTS.register("lens.scribble",
            () -> SoundEvent.createVariableRangeEvent(ChronoGenesis.id("lens.scribble")));

    /** The Chronocortex speaking: a wet click of thought and a hollow, wordless voice under the words. */
    public static final Supplier<SoundEvent> CORTEX_SPEAKS = SOUND_EVENTS.register("cortex.speaks",
            () -> SoundEvent.createVariableRangeEvent(ChronoGenesis.id("cortex.speaks")));

    private ModSounds() {}
}
