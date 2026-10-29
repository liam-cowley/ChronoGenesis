package com.chronogenesis.registry;

import java.util.List;
import java.util.function.Supplier;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.biocomputer.PlayerResearch;
import com.chronogenesis.essence.DrainedEssence;
import com.chronogenesis.player.PlayerEssence;
import com.chronogenesis.time.TemporalStress;
import com.chronogenesis.timetravel.TemporalShift;

import net.minecraft.resources.ResourceLocation;
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

    /** On players: clues noticed and spent, research completed. Kept on death. */
    public static final Supplier<AttachmentType<PlayerResearch>> PLAYER_RESEARCH = ATTACHMENT_TYPES.register("player_research",
            () -> AttachmentType.builder(() -> PlayerResearch.EMPTY)
                    .serialize(PlayerResearch.CODEC)
                    .copyOnDeath()
                    .build());

    /** On players: a temporal shift into another era (where home is, how long it holds). Lost on death. */
    public static final Supplier<AttachmentType<TemporalShift.Shift>> TIME_SHIFT = ATTACHMENT_TYPES.register("time_shift",
            () -> AttachmentType.builder(() -> TemporalShift.Shift.NONE)
                    .serialize(TemporalShift.Shift.CODEC, TemporalShift.Shift::active)
                    .build());

    /** On players: the eras they have stood in (so the Temporal Lens can look into them). Kept through death. */
    public static final Supplier<AttachmentType<List<ResourceLocation>>> VISITED_ERAS = ATTACHMENT_TYPES.register("visited_eras",
            () -> AttachmentType.<List<ResourceLocation>>builder(() -> List.of())
                    .serialize(ResourceLocation.CODEC.listOf(), list -> !list.isEmpty())
                    .copyOnDeath()
                    .build());

    /** On players: the fragments of the Precursors' story they have read (lore/PrecursorLore). Kept through death. */
    public static final Supplier<AttachmentType<List<String>>> READ_LORE = ATTACHMENT_TYPES.register("read_lore",
            () -> AttachmentType.<List<String>>builder(() -> List.of())
                    .serialize(com.mojang.serialization.Codec.STRING.listOf(), list -> !list.isEmpty())
                    .copyOnDeath()
                    .build());

    private ModAttachments() {}
}
