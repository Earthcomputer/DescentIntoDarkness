package net.earthcomputer.descentintodarkness.style;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Optional;

public record Ore(
    BlockPredicate block,
    int pollution,
    Optional<Holder<LootTable>> dropTable,
    int breakAmount
) {
    public static final Codec<Ore> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        DIDCodecs.BLOCK_PREDICATE.fieldOf("block").forGetter(Ore::block),
        Codec.INT.optionalFieldOf("pollution", 0).forGetter(Ore::pollution),
        LootTable.CODEC.optionalFieldOf("drop_table").forGetter(Ore::dropTable),
        ExtraCodecs.POSITIVE_INT.optionalFieldOf("break_amount", 10).forGetter(Ore::breakAmount)
    ).apply(instance, Ore::new));
}
