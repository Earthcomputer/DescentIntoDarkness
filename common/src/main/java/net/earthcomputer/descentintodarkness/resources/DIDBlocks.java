package net.earthcomputer.descentintodarkness.resources;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.earthcomputer.descentintodarkness.DIDRegistries;
import net.earthcomputer.descentintodarkness.DescentIntoDarkness;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DIDBlocks {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Entry> entries = new LinkedHashMap<>();

    private DIDBlocks() {
    }

    public static void reload() {
        Map<String, Entry> newEntries;
        try {
            newEntries = DIDResourceLoader.loadResources("blocks", Entry.CODEC);
        } catch (Exception e) {
            LOGGER.error("Couldn't load blocks", e);
            return;
        }

        if (!newEntries.keySet().equals(entries.keySet())) {
            DIDResourceLoader.setRestartRequired();
        }
        entries.clear();
        entries.putAll(newEntries);
    }

    public static void register() {
        entries.forEach((id, entry) -> {
            DIDRegistries.REGISTRAR_MANAGER.get(Registries.BLOCK).register(DescentIntoDarkness.id(id), () -> new Block(BlockBehaviour.Properties.of()));
        });
    }

    private record Entry() {
        static final Codec<Entry> CODEC = Codec.unit(Entry::new);
    }
}
