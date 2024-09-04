package net.earthcomputer.descentintodarkness.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

public record SwordItemCreator(
    Tier tier
) implements ItemCreator {
    public static final MapCodec<SwordItemCreator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ToolTier.TIER_CODEC.fieldOf("tier").forGetter(creator -> creator.tier)
    ).apply(instance, SwordItemCreator::new));

    @Override
    public ItemCreatorType<?> type() {
        return ItemCreatorType.SWORD.get();
    }

    @Override
    public Item create(ResourceLocation id, Item.Properties properties) {
        return new SwordItem(tier, properties);
    }
}
