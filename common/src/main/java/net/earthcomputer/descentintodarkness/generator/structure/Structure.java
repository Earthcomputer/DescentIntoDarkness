package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.DIDRegistries;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.Centroid;
import net.earthcomputer.descentintodarkness.generator.TagList;
import net.earthcomputer.descentintodarkness.generator.Transform;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public abstract class Structure {
    public static final Codec<Structure> CODEC = Codec.lazyInitialized(() -> DIDRegistries.structureType().byNameCodec().dispatch(Structure::type, StructureType::codec));

    private final List<StructurePlacementEdge> edges;
    private final double count;
    private final Optional<BlockPredicate> canPlaceOn;
    private final Optional<BlockPredicate> canReplace;
    private final List<Direction> validDirections;
    private final boolean snapToAxis;
    private final Direction originSide;
    private final boolean shouldTransformBlocks;
    private final boolean shouldTransformPosition;
    private final boolean randomRotation;
    private final TagList tagList;

    protected Structure(StructureProperties props) {
        this.edges = props.edges.isEmpty() ? getDefaultEdges() : props.edges;
        this.count = props.count;
        this.canPlaceOn = props.canPlaceOn;
        this.canReplace = props.canReplace;
        this.validDirections = this.edges.stream().flatMap(edge -> Arrays.stream(edge.directions())).toList();
        this.snapToAxis = props.snapToAxis.orElseGet(this::shouldSnapToAxisByDefault);
        this.originSide = props.originSide.orElseGet(() -> getDefaultOriginSide(this.edges));
        this.shouldTransformBlocks = props.shouldTransformBlocks.orElseGet(this::shouldTransformBlocksByDefault);
        this.shouldTransformPosition = props.shouldTransformPosition.orElseGet(this::shouldTransformPositionByDefault);
        this.randomRotation = props.randomRotation;
        this.tagList = props.tagList;
    }

    public abstract StructureType<?> type();

    public final List<Direction> validDirections() {
        return validDirections;
    }

    public final double count() {
        return count;
    }

    public final boolean canPlaceOn(CaveGenContext ctx, BlockPos pos) {
        return canPlaceOn.map(canPlaceOn -> canPlaceOn.test(ctx.asLevel(), pos)).orElseGet(() -> !ctx.style().isTransparentBlock(ctx, pos));
    }

    protected List<StructurePlacementEdge> getDefaultEdges() {
        return Arrays.asList(StructurePlacementEdge.values());
    }

    protected boolean shouldTransformBlocksByDefault() {
        return false;
    }

    protected boolean shouldTransformPositionByDefault() {
        return true;
    }

    protected boolean shouldSnapToAxisByDefault() {
        return false;
    }

    public final boolean shouldSnapToAxis() {
        return snapToAxis;
    }

    protected Direction getDefaultOriginSide(List<StructurePlacementEdge> edges) {
        if (edges.contains(StructurePlacementEdge.FLOOR)) {
            return Direction.DOWN;
        } else if (edges.contains(StructurePlacementEdge.CEILING)) {
            return Direction.UP;
        } else {
            return Direction.SOUTH;
        }
    }

    public final Direction originSide() {
        return originSide;
    }

    public Direction originPositionSide() {
        return Direction.DOWN;
    }

    public final Transform getBlockTransform(double randomYRotation, BlockPos pos, Direction side) {
        if (!shouldTransformBlocks) {
            return Transform.IDENTITY;
        }
        return getTransform(randomYRotation, pos, originSide, side);
    }

    public final Transform getPositionTransform(double randomYRotation, BlockPos pos, Direction side) {
        if (!shouldTransformPosition) {
            return Transform.IDENTITY;
        }
        return getTransform(randomYRotation, pos, originPositionSide(), side);
    }

    private Transform getTransform(double randomYRotation, BlockPos pos, Direction originSide, Direction side) {
        Transform transform = Transform.translate(pos);

        if (side.getAxis() == Direction.Axis.Y && randomRotation) {
            transform = transform.combine(Transform.rotateY(randomYRotation));
        }

        if (side != originSide) {
            if (originSide == Direction.DOWN) {
                transform = switch (side) {
                    case UP -> transform.combine(Transform.rotateX(Math.PI));
                    case NORTH -> transform.combine(Transform.rotateX(-Math.PI / 2));
                    case SOUTH -> transform.combine(Transform.rotateX(Math.PI / 2));
                    case WEST -> transform.combine(Transform.rotateZ(Math.PI / 2));
                    case EAST -> transform.combine(Transform.rotateZ(-Math.PI / 2));
                    default -> throw new AssertionError("There are too many directions!");
                };
            } else if (originSide == Direction.UP) {
                transform = switch (side) {
                    case DOWN -> transform.combine(Transform.rotateX(Math.PI));
                    case NORTH -> transform.combine(Transform.rotateX(Math.PI / 2));
                    case SOUTH -> transform.combine(Transform.rotateX(-Math.PI / 2));
                    case WEST -> transform.combine(Transform.rotateZ(-Math.PI / 2));
                    case EAST -> transform.combine(Transform.rotateZ(Math.PI / 2));
                    default -> throw new AssertionError("There are too many directions!");
                };
            } else {
                if (side.getAxis() != Direction.Axis.Y) {
                    transform = transform.combine(Transform.rotateY(Math.toRadians(originSide.toYRot() - side.toYRot())));
                } else if (side == Direction.DOWN) {
                    transform = switch (originSide) {
                        case NORTH -> transform.combine(Transform.rotateX(Math.PI / 2));
                        case SOUTH -> transform.combine(Transform.rotateX(-Math.PI / 2));
                        case WEST -> transform.combine(Transform.rotateZ(-Math.PI / 2));
                        case EAST -> transform.combine(Transform.rotateZ(Math.PI / 2));
                        default -> throw new AssertionError("There are too many directions!");
                    };
                } else {
                    transform = switch (originSide) {
                        case NORTH -> transform.combine(Transform.rotateX(-Math.PI / 2));
                        case SOUTH -> transform.combine(Transform.rotateX(Math.PI / 2));
                        case WEST -> transform.combine(Transform.rotateZ(Math.PI / 2));
                        case EAST -> transform.combine(Transform.rotateZ(-Math.PI / 2));
                        default -> throw new AssertionError("There are too many directions!");
                    };
                }
            }
        }

        transform = transform.combine(Transform.translate(pos.multiply(-1)));

        return transform;
    }

    public final TagList tags() {
        return tagList;
    }

    public abstract boolean place(CaveGenContext ctx, BlockPos pos, Centroid centroid, boolean force);

    protected final boolean canReplace(CaveGenContext ctx, BlockPos pos) {
        return canReplace.map(canReplace -> canReplace.test(ctx.asLevel(), pos)).orElseGet(() -> defaultCanReplace(ctx, pos));
    }

    protected boolean defaultCanReplace(CaveGenContext ctx, BlockPos pos) {
        return ctx.style().isTransparentBlock(ctx, pos);
    }

    protected record StructureProperties(
        List<StructurePlacementEdge> edges,
        double count,
        Optional<BlockPredicate> canPlaceOn,
        Optional<BlockPredicate> canReplace,
        Optional<Boolean> snapToAxis,
        Optional<Direction> originSide,
        Optional<Boolean> shouldTransformBlocks,
        Optional<Boolean> shouldTransformPosition,
        boolean randomRotation,
        TagList tagList
    ) {
        public StructureProperties(Structure structure) {
            this(
                structure.edges.equals(structure.getDefaultEdges()) ? List.of() : structure.edges,
                structure.count,
                structure.canPlaceOn,
                structure.canReplace,
                structure.snapToAxis == structure.shouldSnapToAxisByDefault() ? Optional.empty() : Optional.of(structure.snapToAxis),
                structure.originSide == structure.getDefaultOriginSide(structure.edges) ? Optional.empty() : Optional.of(structure.originSide),
                structure.shouldTransformBlocks == structure.shouldTransformBlocksByDefault() ? Optional.empty() : Optional.of(structure.shouldTransformBlocks),
                structure.shouldTransformPosition == structure.shouldTransformPositionByDefault() ? Optional.empty() : Optional.of(structure.shouldTransformPosition),
                structure.randomRotation,
                structure.tagList
            );
        }

        public static final MapCodec<StructureProperties> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DIDCodecs.singleableList(StructurePlacementEdge.CODEC).optionalFieldOf("edges", List.of()).forGetter(StructureProperties::edges),
            Codec.mapEither(
                Codec.doubleRange(0, 1).validate(chance -> chance == 1 ? DataResult.error(() -> "Structure cannot have a chance of 1") : DataResult.success(chance)).fieldOf("chance"),
                DIDCodecs.NON_NEGATIVE_DOUBLE.optionalFieldOf("count", 1.0)
            ).xmap(
                // 1 - chance = e^(-count)
                // -count = ln(1 - chance)
                either -> either.map(chance -> -Math.log(1 - chance), Function.identity()),
                Either::right
            ).forGetter(StructureProperties::count),
            DIDCodecs.BLOCK_PREDICATE.optionalFieldOf("can_place_on").forGetter(StructureProperties::canPlaceOn),
            DIDCodecs.BLOCK_PREDICATE.optionalFieldOf("can_replace").forGetter(StructureProperties::canReplace),
            Codec.BOOL.optionalFieldOf("snap_to_axis").forGetter(StructureProperties::snapToAxis),
            Direction.CODEC.optionalFieldOf("origin_side").forGetter(StructureProperties::originSide),
            Codec.BOOL.optionalFieldOf("should_transform_blocks").forGetter(StructureProperties::shouldTransformBlocks),
            Codec.BOOL.optionalFieldOf("should_transform_position").forGetter(StructureProperties::shouldTransformPosition),
            Codec.BOOL.optionalFieldOf("random_rotation", true).forGetter(StructureProperties::randomRotation),
            TagList.CODEC.forGetter(StructureProperties::tagList)
        ).apply(instance, StructureProperties::new));
    }
}
