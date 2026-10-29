package com.chronogenesis.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.Level;

/**
 * The Hearth's iron golems: their iron kept bright, never let rust or grow over (rust was where the unmaking began,
 * the Hearth said). Friendly unless someone hurts one of them or one of the Precursors (see {@link PrecursorGuards}).
 */
public class PrecursorGolem extends IronGolem {
    public PrecursorGolem(EntityType<? extends PrecursorGolem> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
