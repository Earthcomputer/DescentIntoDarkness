package net.earthcomputer.descentintodarkness.item;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.registry.fuel.FuelRegistry;
import net.earthcomputer.descentintodarkness.DIDPlatform;
import net.earthcomputer.descentintodarkness.DIDRegistries;
import net.earthcomputer.descentintodarkness.DescentIntoDarkness;
import net.earthcomputer.descentintodarkness.resources.DIDBlocks;
import net.earthcomputer.descentintodarkness.resources.DIDResourceLoader;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DIDItems {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Entry> entries = new LinkedHashMap<>();
    private static boolean registered = false;

    private DIDItems() {
    }

    public static void reload() {
        Map<String, Entry> newEntries;
        try {
            newEntries = DIDResourceLoader.loadResources("items", Entry.CODEC);
        } catch (Exception e) {
            LOGGER.error("Couldn't load items", e);
            return;
        }

        newEntries.forEach((name, entry) -> {
            if (DIDBlocks.itemHasBlock(name)) {
                if (entry.creator != SimpleItemCreator.INSTANCE) {
                    LOGGER.error("Item {} with a block specified a custom item type", name);
                }
                entry.creator = BlockItemCreator.INSTANCE;
            }
        });

        DIDResourceLoader.checkRequiresRestart(entries, newEntries);

        entries.clear();
        entries.putAll(newEntries);
    }

    public static void register() {
        entries.forEach((name, entry) -> {
            ResourceLocation id = DescentIntoDarkness.id(name);
            Item.Properties properties = new Item.Properties();
            for (TypedDataComponent<?> component : entry.components) {
                applyComponent(properties, component);
            }
            Item item = entry.creator.create(id, properties);
            Registry.register(BuiltInRegistries.ITEM, id, item);
            entry.fuelBurnTime.ifPresent(fuelBurnTime -> {
                FuelRegistry.register(fuelBurnTime, item);
            });
        });
        registered = true;
    }

    public static void registerCreativeTab() {
        DIDRegistries.REGISTRAR_MANAGER.get(Registries.CREATIVE_MODE_TAB).register(DescentIntoDarkness.id(DescentIntoDarkness.MOD_ID), () -> DIDPlatform.creativeTabBuilder()
            .title(Component.translatable("itemGroup." + DescentIntoDarkness.MOD_ID))
            .icon(() -> new ItemStack(Items.DEEPSLATE)) // TODO: better icon?
            .displayItems((itemDisplayParameters, output) -> {
                if (!registered) {
                    LOGGER.error("Filling in DID creative tab before items are registered");
                }
                entries.forEach((name, entry) -> {
                    if (entry.inCreativeTab) {
                        output.accept(BuiltInRegistries.ITEM.get(DescentIntoDarkness.id(name)));
                    }
                });
            })
            .build());
    }

    private static <T> void applyComponent(Item.Properties properties, TypedDataComponent<T> component) {
        properties.component(component.type(), component.value());
    }

    private static final class Entry implements DIDResourceLoader.RestartRequiringEntry<Entry> {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("in_creative_tab", true).forGetter(entry -> entry.inCreativeTab),
            Codec.mapEither(ItemCreator.CODEC, DIDCodecs.withoutField("type", SimpleItemCreator.CODEC)).xmap(Either::unwrap, Either::left).forGetter(entry -> entry.creator),
            DataComponentMap.CODEC.optionalFieldOf("components", DataComponentMap.EMPTY).forGetter(entry -> entry.components),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("fuel_burn_time").forGetter(entry -> entry.fuelBurnTime)
        ).apply(instance, Entry::new));

        private final boolean inCreativeTab;
        private ItemCreator creator;
        private final DataComponentMap components;
        private final Optional<Integer> fuelBurnTime;

        private Entry(boolean inCreativeTab, ItemCreator creator, DataComponentMap components, Optional<Integer> fuelBurnTime) {
            this.inCreativeTab = inCreativeTab;
            this.creator = creator;
            this.components = components;
            this.fuelBurnTime = fuelBurnTime;
        }

        @Override
        public int hashCode() {
            return Objects.hash(inCreativeTab, creator, components, fuelBurnTime);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Entry that)) {
                return false;
            }
            return this.inCreativeTab == that.inCreativeTab
                && this.creator.equals(that.creator)
                && this.components.equals(that.components)
                && this.fuelBurnTime.equals(that.fuelBurnTime);
        }
    }
}
