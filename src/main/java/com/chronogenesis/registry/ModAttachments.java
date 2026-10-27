package com.chronogenesis.registry;

import java.util.function.Supplier;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.DrainedEssence;
import com.chronogenesis.player.PlayerEssence;
import com.chronogenesis.time.TemporalStress;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Data attachments: extra data stored on entities, block entities, chunks or levels.
 * This is how per-creature essence, player pools and chunk stress are tracked.
 */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ChronoGenesis.MODID);

    /** On mobs: how much of each aspect has been drained. Saved with the mob. */
    public static final Supplier<AttachmentType<DrainedEssence>> DRAINED_ESSENCE = ATTACHMENT_TYPES.register("drained_essence",
            () -> AttachmentType.builder(() -> DrainedEssence.NONE)
                    .serialize(DrainedEssence.CODEC, value -> !value.isEmpty())
                    .build());

    /** On players: essence pools (mana), power levels, active powers, Temporal Exposure. Kept on death. */
    public static final Supplier<AttachmentType<PlayerEssence>> PLAYER_ESSENCE = ATTACHMENT_TYPES.register("player_essence",
            () -> AttachmentType.builder(() -> PlayerEssence.EMPTY)
                    .serialize(PlayerEssence.CODEC)
                    .copyOnDeath()
                    .build());

    /** On chunks: Temporal Stress from extracting Time. */
    public static final Supplier<AttachmentType<TemporalStress>> TEMPORAL_STRESS = ATTACHMENT_TYPES.register("temporal_stress",
            () -> AttachmentType.builder(() -> TemporalStress.NONE)
                    .serialize(TemporalStress.CODEC, value -> value.stress() > 0)
                    .build());

    private ModAttachments() {}
}
