package net.earthcomputer.descentintodarkness.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;

public enum ToolTier implements StringRepresentable {
    WOOD("wood"),
    STONE("stone"),
    IRON("iron"),
    DIAMOND("diamond"),
    GOLD("gold"),
    NETHERITE("netherite"),
    ;

    public static final Codec<ToolTier> CODEC = StringRepresentable.fromEnum(ToolTier::values);
    public static final Codec<Tier> TIER_CODEC = CODEC.flatComapMap(ToolTier::toTier, ToolTier::fromTier);

    private final String name;

    ToolTier(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    private Tier toTier() {
        return switch (this) {
            case WOOD -> Tiers.WOOD;
            case STONE -> Tiers.STONE;
            case IRON -> Tiers.IRON;
            case DIAMOND -> Tiers.DIAMOND;
            case GOLD -> Tiers.GOLD;
            case NETHERITE -> Tiers.NETHERITE;
        };
    }

    private static DataResult<ToolTier> fromTier(Tier tier) {
        if (!(tier instanceof Tiers tiers)) {
            return DataResult.error(() -> "Cannot serialize tier " + tier);
        }
        return DataResult.success(switch (tiers) {
            case WOOD -> WOOD;
            case STONE -> STONE;
            case IRON -> IRON;
            case DIAMOND -> DIAMOND;
            case GOLD -> GOLD;
            case NETHERITE -> NETHERITE;
        });
    }
}
