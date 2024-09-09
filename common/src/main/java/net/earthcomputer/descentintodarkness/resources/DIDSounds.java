package net.earthcomputer.descentintodarkness.resources;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.DescentIntoDarkness;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ExtraCodecs;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class DIDSounds {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Entry> entries = new LinkedHashMap<>();

    private DIDSounds() {
    }

    public static void reload() {
        Map<String, Entry> newEntries;
        try {
            newEntries = DIDResourceLoader.loadResources("sound_events", Entry.CODEC);
        } catch (Exception e) {
            LOGGER.error("Couldn't load sound events", e);
            return;
        }

        DIDResourceLoader.checkRequiresRestart(entries, newEntries);
        entries.clear();
        entries.putAll(newEntries);
    }

    public static void register() {
        entries.forEach((name, entry) -> {
            ResourceLocation id = DescentIntoDarkness.id(name);
            Registry.register(BuiltInRegistries.SOUND_EVENT, id, entry.range.map(range -> SoundEvent.createFixedRangeEvent(id, range)).orElseGet(() -> SoundEvent.createVariableRangeEvent(id)));
        });
    }

    private record Entry(Optional<Float> range) implements DIDResourceLoader.RestartRequiringEntry<Entry> {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("range").forGetter(Entry::range)
        ).apply(instance, Entry::new));
    }
}
