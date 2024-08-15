package net.earthcomputer.descentintodarkness;

import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.earthcomputer.descentintodarkness.instancing.CaveTrackerManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class DescentIntoDarkness {
    public static final String MOD_ID = "descent_into_darkness";

    public static void init() {
        CommandRegistrationEvent.EVENT.register(DescentIntoDarkness::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, Commands.CommandSelection selection) {
        DIDCommand.register(dispatcher);
        CaveTrackerManager.registerEvents();
    }
}
