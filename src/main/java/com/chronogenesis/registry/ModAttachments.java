package com.chronogenesis.registry;

import java.util.function.Supplier;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.EntityEssence;

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
     * How much of each aspect has been drained from a living entity. Saved with the entity.
     * Use {@link com.chronogenesis.essence.EssenceDrain} rather than touching it directly.
     */
    public static final Supplier<AttachmentType<EntityEssence>> ESSENCE = ATTACHMENT_TYPES.register("essence",
            () -> AttachmentType.builder(() -> EntityEssence.NONE).serialize(EntityEssence.CODEC).build());

    private ModAttachments() {}
}
