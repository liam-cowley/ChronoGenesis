package com.chronogenesis.biocomputer;

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
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/** The {@link Lesson} registry, and the lessons whose meaning is checked in code. */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class Lessons {
    public static final ResourceKey<Registry<Lesson>> REGISTRY = ResourceKey.createRegistryKey(ChronoGenesis.id("lesson"));

    /** A book: it learns words, and can speak. */
    public static final ResourceLocation WORDS = ChronoGenesis.id("words");
    /** A named name tag: it learns it is someone, and takes the name. */
    public static final ResourceLocation SELF = ChronoGenesis.id("self");
    /** A clock: it knows the time of day. */
    public static final ResourceLocation TIME = ChronoGenesis.id("time");

    private Lessons() {}

    @SubscribeEvent
    static void registerRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(REGISTRY, Lesson.CODEC, Lesson.CODEC);
    }

    /** Every lesson, in order. */
    public static List<Map.Entry<ResourceLocation, Lesson>> all(RegistryAccess access) {
        List<Map.Entry<ResourceLocation, Lesson>> list = new ArrayList<>();
        access.registry(REGISTRY).ifPresent(registry -> registry.entrySet().forEach(entry ->
                list.add(Map.entry(entry.getKey().location(), entry.getValue()))));
        list.sort(Comparator.comparingInt((Map.Entry<ResourceLocation, Lesson> e) -> e.getValue().order())
                .thenComparing(e -> e.getKey().toString()));
        return list;
    }

    @Nullable
    public static Lesson get(RegistryAccess access, ResourceLocation id) {
        return access.registry(REGISTRY).map(registry -> registry.get(id)).orElse(null);
    }

    /** The lesson an item would teach, learned or not: the first in order whose item matches. */
    @Nullable
    public static Map.Entry<ResourceLocation, Lesson> taughtBy(RegistryAccess access, ItemStack stack) {
        for (Map.Entry<ResourceLocation, Lesson> entry : all(access)) {
            if (entry.getValue().item().test(stack)) {
                return entry;
            }
        }
        return null;
    }
}
