package com.chronogenesis.registry;

import java.util.function.Supplier;

import com.chronogenesis.ChronoGenesis;
import com.mojang.serialization.Codec;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Data attachments: extra data stored on entities, block entities, chunks or levels.
 * This is how per-creature essence pools are tracked (no capabilities/NBT hacks needed).
 */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ChronoGenesis.MODID);

    /**
     * Temporal essence stored in a living entity. Saved with the entity.
     * Read: {@code entity.getData(ModAttachments.TEMPORAL_ESSENCE)}
     * Write: {@code entity.setData(ModAttachments.TEMPORAL_ESSENCE, value)}
     */
    public static final Supplier<AttachmentType<Integer>> TEMPORAL_ESSENCE = ATTACHMENT_TYPES.register("temporal_essence",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build());

    private ModAttachments() {}
}
