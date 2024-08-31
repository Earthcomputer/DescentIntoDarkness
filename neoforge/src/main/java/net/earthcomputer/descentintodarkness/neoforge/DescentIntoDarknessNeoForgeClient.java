package net.earthcomputer.descentintodarkness.neoforge;

import net.earthcomputer.descentintodarkness.DescentIntoDarkness;
import net.earthcomputer.descentintodarkness.client.DescentIntoDarknessClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = DescentIntoDarkness.MOD_ID, dist = Dist.CLIENT)
public final class DescentIntoDarknessNeoForgeClient {
    public DescentIntoDarknessNeoForgeClient() {
        DescentIntoDarknessClient.init();
    }
}
