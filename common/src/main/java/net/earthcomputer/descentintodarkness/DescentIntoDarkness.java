package net.earthcomputer.descentintodarkness;

import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.earthcomputer.descentintodarkness.instancing.CaveTrackerManager;
import net.earthcomputer.descentintodarkness.resources.DIDResourceLoader;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;

public final class DescentIntoDarkness {
    public static final String MOD_ID = "descent_into_darkness";

    private DescentIntoDarkness() {
    }

    public static void init() {
        DIDRegistries.register();
        DIDResourceLoader.initialLoad();
        CommandRegistrationEvent.EVENT.register(DescentIntoDarkness::registerCommands);
        CaveTrackerManager.registerEvents();
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, Commands.CommandSelection selection) {
        DIDCommand.register(dispatcher, context);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
