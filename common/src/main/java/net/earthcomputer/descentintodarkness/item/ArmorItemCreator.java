package net.earthcomputer.descentintodarkness.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;

public record ArmorItemCreator(
    Holder<ArmorMaterial> material,
    ArmorItem.Type armorType
) implements ItemCreator {
    public static final MapCodec<ArmorItemCreator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        BuiltInRegistries.ARMOR_MATERIAL.holderByNameCodec().fieldOf("material").forGetter(ArmorItemCreator::material),
        ArmorItem.Type.CODEC.fieldOf("armor_type").forGetter(ArmorItemCreator::armorType)
    ).apply(instance, ArmorItemCreator::new));

    @Override
    public ItemCreatorType<?> type() {
        return ItemCreatorType.ARMOR.get();
    }

    @Override
    public Item create(ResourceLocation id, Item.Properties properties) {
        return new ArmorItem(material, armorType, properties);
    }
}
