package net.earthcomputer.descentintodarkness.resources;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class DIDBlock extends Block {
    private final VoxelShape shape;
    private final VoxelShape collisionShape;
    private final VoxelShape outlineShape;

    public DIDBlock(Properties properties, VoxelShape shape, VoxelShape collisionShape, VoxelShape outlineShape) {
        super(properties);
        this.shape = shape;
        this.collisionShape = collisionShape;
        this.outlineShape = outlineShape;
    }

    @Override
    protected VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return this.hasCollision ? collisionShape : Shapes.empty();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return outlineShape;
    }
}
