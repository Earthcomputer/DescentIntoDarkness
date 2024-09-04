package net.earthcomputer.descentintodarkness.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class BlockItemCreator implements ItemCreator {
    public static final BlockItemCreator INSTANCE = new BlockItemCreator();

    private BlockItemCreator() {
    }

    @Override
    public ItemCreatorType<?> type() {
        throw new UnsupportedOperationException("This class shouldn't be serialized");
    }

    @Override
    public Item create(ResourceLocation id, Item.Properties properties) {
        return new BlockItem(BuiltInRegistries.BLOCK.get(id), properties);
    }
}
