package net.earthcomputer.descentintodarkness.entity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.earthcomputer.descentintodarkness.DIDRegistries;
import net.earthcomputer.descentintodarkness.DescentIntoDarkness;
import net.earthcomputer.descentintodarkness.resources.DIDResourceLoader;
import net.minecraft.core.registries.Registries;
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
        entries.forEach((id, entry) -> {
            DIDRegistries.REGISTRAR_MANAGER.get(Registries.ENTITY_TYPE).register(DescentIntoDarkness.id(id), () -> EntityType.Builder.of(DIDEntity::new, MobCategory.MONSTER).build(id));
        });
    }

    private record Entry() implements DIDResourceLoader.RestartRequiringEntry<Entry> {
        static final Codec<Entry> CODEC = Codec.unit(Entry::new);
    }
}
