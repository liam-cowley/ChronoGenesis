package com.chronogenesis.timetravel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.ChronoGenesis;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/** The {@link Era} registry. */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class Eras {
    public static final ResourceKey<Registry<Era>> REGISTRY = ResourceKey.createRegistryKey(ChronoGenesis.id("era"));

    private Eras() {}

    @SubscribeEvent
    static void registerRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(REGISTRY, Era.CODEC, Era.CODEC);
    }

    /** Every era, in order. */
    public static List<Map.Entry<ResourceLocation, Era>> all(RegistryAccess access) {
        List<Map.Entry<ResourceLocation, Era>> list = new ArrayList<>();
        access.registry(REGISTRY).ifPresent(registry -> registry.entrySet().forEach(entry ->
                list.add(Map.entry(entry.getKey().location(), entry.getValue()))));
        list.sort(Comparator.comparingInt((Map.Entry<ResourceLocation, Era> e) -> e.getValue().order())
                .thenComparing(e -> e.getKey().toString()));
        return list;
    }

    @Nullable
    public static Era get(RegistryAccess access, ResourceLocation id) {
        return access.registry(REGISTRY).map(registry -> registry.get(id)).orElse(null);
    }
}
