package net.earthcomputer.descentintodarkness.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;

public record ShovelItemCreator(
    Tier tier
) implements ItemCreator {
    public static final MapCodec<ShovelItemCreator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ToolTier.TIER_CODEC.fieldOf("tier").forGetter(creator -> creator.tier)
    ).apply(instance, ShovelItemCreator::new));

    @Override
    public ItemCreatorType<?> type() {
        return ItemCreatorType.SHOVEL.get();
    }

    @Override
    public Item create(ResourceLocation id, Item.Properties properties) {
        return new ShovelItem(tier, properties);
    }
}
