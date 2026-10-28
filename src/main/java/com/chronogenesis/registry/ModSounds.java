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

    private ModSounds() {}
}
