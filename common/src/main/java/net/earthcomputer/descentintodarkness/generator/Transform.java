package net.earthcomputer.descentintodarkness.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

public final class Transform {
    public static final Transform IDENTITY = new Transform(1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0);

    private final double m00;
    private final double m01;
    private final double m02;
    private final double m10;
    private final double m11;
    private final double m12;
    private final double m20;
    private final double m21;
    private final double m22;
    private final double m30;
    private final double m31;
    private final double m32;

    private Transform(double m00, double m01, double m02, double m10, double m11, double m12, double m20, double m21, double m22, double m30, double m31, double m32) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
        this.m30 = m30;
        this.m31 = m31;
        this.m32 = m32;
    }

    public static Transform translate(Vec3 amount) {
        return new Transform(1, 0, 0, 0, 1, 0, 0, 0, 1, amount.x, amount.y, amount.z);
    }

    public static Transform translate(BlockPos amount) {
        return translate(Vec3.atLowerCornerOf(amount));
    }

    public static Transform rotateX(double angle) {
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        return new Transform(1, 0, 0, 0, cos, sin, 0, -sin, cos, 0, 0, 0);
    }

    public static Transform rotateY(double angle) {
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        return new Transform(cos, 0, -sin, 0, 1, 0, sin, 0, cos, 0, 0, 0);
    }

    public static Transform rotateZ(double angle) {
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        return new Transform(cos, sin, 0, -sin, cos, 0, 0, 0, 1, 0, 0, 0);
    }

    public Transform combine(Transform other) {
        return new Transform(
            Math.fma(m00, other.m00, Math.fma(m10, other.m01, m20 * other.m02)),
            Math.fma(m01, other.m00, Math.fma(m11, other.m01, m21 * other.m02)),
            Math.fma(m02, other.m00, Math.fma(m12, other.m01, m22 * other.m02)),
            Math.fma(m00, other.m10, Math.fma(m10, other.m11, m20 * other.m12)),
            Math.fma(m01, other.m10, Math.fma(m11, other.m11, m21 * other.m12)),
            Math.fma(m02, other.m10, Math.fma(m12, other.m11, m22 * other.m12)),
            Math.fma(m00, other.m20, Math.fma(m10, other.m21, m20 * other.m22)),
            Math.fma(m01, other.m20, Math.fma(m11, other.m21, m21 * other.m22)),
            Math.fma(m02, other.m20, Math.fma(m12, other.m21, m22 * other.m22)),
            Math.fma(m00, other.m30, Math.fma(m10, other.m31, Math.fma(m20, other.m32, m30))),
            Math.fma(m01, other.m30, Math.fma(m11, other.m31, Math.fma(m21, other.m32, m31))),
            Math.fma(m02, other.m30, Math.fma(m12, other.m31, Math.fma(m22, other.m32, m32)))
        );
    }

    public Transform inverse() {
        double m11m00 = m00 * m11, m10m01 = m01 * m10, m10m02 = m02 * m10;
        double m12m00 = m00 * m12, m12m01 = m01 * m12, m11m02 = m02 * m11;
        double s = 1.0 / ((m11m00 - m10m01) * m22 + (m10m02 - m12m00) * m21 + (m12m01 - m11m02) * m20);
        double m10m22 = m10 * m22, m10m21 = m10 * m21, m11m22 = m11 * m22;
        double m11m20 = m11 * m20, m12m21 = m12 * m21, m12m20 = m12 * m20;
        double m20m02 = m20 * m02, m20m01 = m20 * m01, m21m02 = m21 * m02;
        double m21m00 = m21 * m00, m22m01 = m22 * m01, m22m00 = m22 * m00;
        double nm00 = (m11m22 - m12m21) * s;
        double nm01 = (m21m02 - m22m01) * s;
        double nm02 = (m12m01 - m11m02) * s;
        double nm10 = (m12m20 - m10m22) * s;
        double nm11 = (m22m00 - m20m02) * s;
        double nm12 = (m10m02 - m12m00) * s;
        double nm20 = (m10m21 - m11m20) * s;
        double nm21 = (m20m01 - m21m00) * s;
        double nm22 = (m11m00 - m10m01) * s;
        double nm30 = (m10m22 * m31 - m10m21 * m32 + m11m20 * m32 - m11m22 * m30 + m12m21 * m30 - m12m20 * m31) * s;
        double nm31 = (m20m02 * m31 - m20m01 * m32 + m21m00 * m32 - m21m02 * m30 + m22m01 * m30 - m22m00 * m31) * s;
        double nm32 = (m11m02 * m30 - m12m01 * m30 + m12m00 * m31 - m10m02 * m31 + m10m01 * m32 - m11m00 * m32) * s;
        return new Transform(nm00, nm01, nm02, nm10, nm11, nm12, nm20, nm21, nm22, nm30, nm31, nm32);
    }

    public Vec3 transform(Vec3 position) {
        return new Vec3(m00 * position.x + m10 * position.y + m20 * position.z + m30, m01 * position.x + m11 * position.y + m21 * position.z + m31, m02 * position.x + m12 * position.y + m22 * position.z + m32);
    }

    public BlockPos transform(BlockPos position) {
        Vec3 transformed = transform(Vec3.atLowerCornerOf(position));
        return new BlockPos((int) Math.round(transformed.x), (int) Math.round(transformed.y), (int) Math.round(transformed.z));
    }

    public Vec3 transformDirection(Vec3 direction) {
        return new Vec3(m00 * direction.x + m10 * direction.y + m20 * direction.z, m01 * direction.x + m11 * direction.y + m21 * direction.z, m02 * direction.x + m12 * direction.y + m22 * direction.z);
    }

    public Direction transformDirection(Direction direction) {
        return Direction.getNearest(transformDirection(Vec3.atLowerCornerOf(direction.getNormal())));
    }

    public BlockState transform(BlockState state) {
        Vec3 right = transformDirection(new Vec3(1, 0, 0));
        Vec3 forward = transformDirection(new Vec3(0, 0, 1));
        if (right.horizontalDistanceSqr() < 0.0001 || forward.horizontalDistanceSqr() < 0.0001) {
            return state; // we've gone vertical, don't bother transforming the state
        }
        Direction rightDir = Direction.getNearest(new Vec3(right.x, 0, right.z)); // east for identity
        Direction forwardDir = Direction.getNearest(new Vec3(forward.x, 0, forward.z)); // south for identity
        return switch (forwardDir) {
            case SOUTH -> switch (rightDir) {
                case EAST -> state;
                case WEST -> state.mirror(Mirror.FRONT_BACK);
                case NORTH, SOUTH -> state; // inconsistent
                default -> throw new IllegalStateException("Not a horizontal dir: " + rightDir);
            };
            case NORTH -> switch (rightDir) {
                case WEST -> state.rotate(Rotation.CLOCKWISE_180);
                case EAST -> state.mirror(Mirror.LEFT_RIGHT);
                case NORTH, SOUTH -> state; // inconsistent
                default -> throw new IllegalStateException("Not a horizontal dir: " + rightDir);
            };
            case WEST -> switch (rightDir) {
                case SOUTH -> state.rotate(Rotation.CLOCKWISE_90);
                case NORTH -> state.mirror(Mirror.FRONT_BACK).rotate(Rotation.CLOCKWISE_90);
                case WEST, EAST -> state; // inconsistent
                default -> throw new IllegalStateException("Not a horizontal dir: " + rightDir);
            };
            case EAST -> switch (rightDir) {
                case SOUTH -> state.mirror(Mirror.FRONT_BACK).rotate(Rotation.COUNTERCLOCKWISE_90);
                case NORTH -> state.rotate(Rotation.COUNTERCLOCKWISE_90);
                case WEST, EAST -> state; // inconsistent
                default -> throw new IllegalStateException("Not a horizontal dir: " + rightDir);
            };
            default -> throw new IllegalStateException("Not a horizontal dir: " + forwardDir);
        };
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new double[] { m00, m01, m02, m10, m11, m12, m20, m21, m30, m31, m32 });
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Transform that)) {
            return false;
        }
        return this.m00 == that.m00
            && this.m01 == that.m01
            && this.m02 == that.m02
            && this.m10 == that.m10
            && this.m11 == that.m11
            && this.m12 == that.m12
            && this.m20 == that.m20
            && this.m21 == that.m21
            && this.m22 == that.m22
            && this.m30 == that.m30
            && this.m31 == that.m31
            && this.m32 == that.m32;
    }

    @Override
    public String toString() {
        return "[" + m00 + ", " + m01 + ", " + m02 + "; " + m10 + ", " + m11 + ", " + m12 + "; " + m20 + ", " + m21 + ", " + m22 + "; " + m30 + ", " + m31 + ", " + m32 + "]";
    }
}
