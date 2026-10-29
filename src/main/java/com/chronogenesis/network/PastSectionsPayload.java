package com.chronogenesis.network;

import java.util.Arrays;
import java.util.List;

import com.chronogenesis.ChronoGenesis;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Server to client: chunk sections of the past dimension (or of the era the Lens looks into: {@code source}), for
 * the Temporal Lens. Blocks are serialised exactly
 * as in the game's chunk packets; light as two nibble arrays (sky, block) of final light values.
 */
public record PastSectionsPayload(ResourceLocation source, List<Section> sections) implements CustomPacketPayload {
    public static final Type<PastSectionsPayload> TYPE = new Type<>(ChronoGenesis.id("past_sections"));

    /** One 16x16x16 section of the past at {@code pos} ({@link net.minecraft.core.SectionPos#asLong}). */
    public record Section(long pos, byte[] blocks, byte[] sky, byte[] block) {
        public static final StreamCodec<ByteBuf, Section> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, Section::pos,
                ByteBufCodecs.BYTE_ARRAY, Section::blocks,
                ByteBufCodecs.BYTE_ARRAY, Section::sky,
                ByteBufCodecs.BYTE_ARRAY, Section::block,
                Section::new);

        public static Section of(long pos, LevelChunkSection section, DataLayer sky, DataLayer block) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            section.write(buf);
            byte[] blocks = Arrays.copyOf(buf.array(), buf.writerIndex());
            buf.release();
            return new Section(pos, blocks, sky.getData().clone(), block.getData().clone());
        }

        /** The section's blocks, read back as the client reads a chunk packet's sections. */
        public LevelChunkSection decode(Registry<Biome> biomes) {
            LevelChunkSection section = new LevelChunkSection(biomes);
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(blocks));
            section.read(buf);
            return section;
        }

        public DataLayer skyLight() {
            return new DataLayer(sky.clone());
        }

        public DataLayer blockLight() {
            return new DataLayer(block.clone());
        }
    }

    public static final StreamCodec<ByteBuf, PastSectionsPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, PastSectionsPayload::source,
            Section.STREAM_CODEC.apply(ByteBufCodecs.list()), PastSectionsPayload::sections,
            PastSectionsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
