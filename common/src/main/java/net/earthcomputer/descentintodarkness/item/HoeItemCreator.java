package net.earthcomputer.descentintodarkness.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;

public record HoeItemCreator(
    Tier tier
) implements ItemCreator {
    public static final MapCodec<HoeItemCreator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ToolTier.TIER_CODEC.fieldOf("tier").forGetter(creator -> creator.tier)
    ).apply(instance, HoeItemCreator::new));

    @Override
    public ItemCreatorType<?> type() {
        return ItemCreatorType.HOE.get();
    }

    @Override
    public Item create(ResourceLocation id, Item.Properties properties) {
        return new HoeItem(tier, properties);
    }
}
