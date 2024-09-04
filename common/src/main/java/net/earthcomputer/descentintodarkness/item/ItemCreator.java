package net.earthcomputer.descentintodarkness.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.earthcomputer.descentintodarkness.DIDRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public interface ItemCreator {
    MapCodec<ItemCreator> CODEC = MapCodec.assumeMapUnsafe(Codec.lazyInitialized(() -> DIDRegistries.itemCreatorType().byNameCodec().dispatch(ItemCreator::type, ItemCreatorType::codec)));

    ItemCreatorType<?> type();
    Item create(ResourceLocation id, Item.Properties properties);
}
