package net.earthcomputer.descentintodarkness.entity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.earthcomputer.descentintodarkness.DescentIntoDarkness;
import net.earthcomputer.descentintodarkness.resources.DIDResourceLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DIDEntities {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Entry> entries = new LinkedHashMap<>();

    private DIDEntities() {
    }

    public static void reload() {
        Map<String, Entry> newEntries;
        try {
            newEntries = DIDResourceLoader.loadResources("entities", Entry.CODEC);
        } catch (Exception e) {
            LOGGER.error("Couldn't load entities", e);
            return;
        }

        DIDResourceLoader.checkRequiresRestart(entries, newEntries);
        entries.clear();
        entries.putAll(newEntries);
    }

    public static void register() {
        entries.forEach((name, entry) -> {
            ResourceLocation id = DescentIntoDarkness.id(name);
            Registry.register(BuiltInRegistries.ENTITY_TYPE, id, EntityType.Builder.of(DIDEntity::new, MobCategory.MONSTER).build(id.toString()));
        });
    }

    private record Entry() implements DIDResourceLoader.RestartRequiringEntry<Entry> {
        static final Codec<Entry> CODEC = Codec.unit(Entry::new);
    }
}
