package net.earthcomputer.descentintodarkness.neoforge;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import net.earthcomputer.descentintodarkness.DescentIntoDarkness;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@Mod(DescentIntoDarkness.MOD_ID)
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class DescentIntoDarknessNeoForge {
    public DescentIntoDarknessNeoForge() {
        DescentIntoDarkness.init();
    }

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        DescentIntoDarkness.postRegistryInit();
    }
}
