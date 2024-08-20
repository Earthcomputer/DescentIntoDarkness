package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.Optional;

public final class WaterfallStructure extends Structure {
    public static final MapCodec<WaterfallStructure> CODEC = RecordCodecBuilder.<WaterfallStructure>mapCodec(instance -> instance.group(
        StructureProperties.CODEC.forGetter(StructureProperties::new),
        FluidType.CODEC.fieldOf("fluid_type").forGetter(structure -> structure.fluidType),
        DIDCodecs.BLOCK_STATE_PROVIDER.optionalFieldOf("block").forGetter(structure -> structure.block),
        ExtraCodecs.POSITIVE_INT.optionalFieldOf("flow_distance", 8).forGetter(structure -> structure.flowDistance)
    ).apply(instance, WaterfallStructure::new)).validate(structure -> {
        if (structure.fluidType == FluidType.BLOCK) {
            if (structure.block.isEmpty()) {
                return DataResult.error(() -> "Did not specify the block type for a waterfall block fluid");
            }
        } else {
            if (structure.block.isPresent()) {
                return DataResult.error(() -> "Cannot specify the block type for a waterfall non-block fluid");
            }
            if (structure.flowDistance != 8) {
                return DataResult.error(() -> "Cannot specify the flow distance for a waterfall non-block fluid");
            }
        }
        return DataResult.success(structure);
    });

    private final FluidType fluidType;
    private final Optional<BlockStateProvider> block;
    private final int flowDistance;

    private WaterfallStructure(StructureProperties props, FluidType fluidType, Optional<BlockStateProvider> block, int flowDistance) {
        super(props);
        this.fluidType = fluidType;
        this.block = block;
        this.flowDistance = flowDistance;
    }

    @Override
    protected boolean shouldTransformPositionByDefault() {
        return false;
    }

    @Override
    public StructureType<?> type() {
        return StructureType.WATERFALL.get();
    }

    private enum FluidType implements StringRepresentable {
        WATER("water"),
        LAVA("lava"),
        SNOW_LAYER("snow_layer"),
        BLOCK("block"),
        ;

        public static final Codec<FluidType> CODEC = StringRepresentable.fromEnum(FluidType::values);

        private final String name;

        FluidType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
