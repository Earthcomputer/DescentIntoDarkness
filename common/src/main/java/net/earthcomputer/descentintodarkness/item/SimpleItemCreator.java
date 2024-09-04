package net.earthcomputer.descentintodarkness.item;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class SimpleItemCreator implements ItemCreator {
    public static final SimpleItemCreator INSTANCE = new SimpleItemCreator();
    public static final MapCodec<SimpleItemCreator> CODEC = MapCodec.unit(INSTANCE);

    private SimpleItemCreator() {
    }

    @Override
    public ItemCreatorType<?> type() {
        return ItemCreatorType.SIMPLE.get();
    }

    @Override
    public Item create(ResourceLocation id, Item.Properties properties) {
        return new Item(properties);
    }
}
