package net.earthcomputer.descentintodarkness.neoforge;

import net.earthcomputer.descentintodarkness.DescentIntoDarkness;
import net.earthcomputer.descentintodarkness.client.DescentIntoDarknessClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = DescentIntoDarkness.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class DIDNeoForgeClientEvents {
    @SubscribeEvent
    public static void onClientInit(FMLClientSetupEvent event) {
        event.enqueueWork(DescentIntoDarknessClient::init);
    }
}
