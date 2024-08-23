package net.earthcomputer.descentintodarkness.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

public final class DIDEntity extends Mob {
    public DIDEntity(EntityType<? extends DIDEntity> entityType, Level level) {
        super(entityType, level);
    }
}
