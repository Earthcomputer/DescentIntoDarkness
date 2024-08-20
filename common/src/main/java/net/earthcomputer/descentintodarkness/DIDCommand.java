package net.earthcomputer.descentintodarkness;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.earthcomputer.descentintodarkness.instancing.CaveTrackerManager;
import net.earthcomputer.descentintodarkness.style.CaveStyle;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.RandomSupport;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Objects;
import java.util.Set;

import static com.mojang.brigadier.arguments.BoolArgumentType.*;
import static com.mojang.brigadier.arguments.IntegerArgumentType.*;
import static com.mojang.brigadier.arguments.LongArgumentType.*;
import static net.minecraft.commands.Commands.*;
import static net.minecraft.commands.arguments.ResourceArgument.*;

public final class DIDCommand {
    private DIDCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        var command = dispatcher.register(literal("did")
            .then(literal("generate")
                .then(argument("style", resource(context, DIDRegistries.CAVE_STYLE))
                    .executes(ctx -> generate(ctx.getSource(), getResource(ctx, "style", DIDRegistries.CAVE_STYLE), 7, RandomSupport.generateUniqueSeed(), false))
                    .then(argument("size", integer(3, 100))
                        .executes(ctx -> generate(ctx.getSource(), getResource(ctx, "style", DIDRegistries.CAVE_STYLE), getInteger(ctx, "size"), RandomSupport.generateUniqueSeed(), false))
                        .then(argument("seed", longArg())
                            .executes(ctx -> generate(ctx.getSource(), getResource(ctx, "style", DIDRegistries.CAVE_STYLE), getInteger(ctx, "size"), getLong(ctx, "seed"), false))
                            .then(argument("debug", bool())
                                .executes(ctx -> generate(ctx.getSource(), getResource(ctx, "style", DIDRegistries.CAVE_STYLE), getInteger(ctx, "size"), getLong(ctx, "seed"), getBool(ctx, "debug")))))))));
        dispatcher.register(literal("descentintodarkness").redirect(command));
    }

    private static int generate(CommandSourceStack source, Holder<CaveStyle> style, int size, long seed, boolean debug) {
        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, DescentIntoDarkness.id("cave_" + RandomStringUtils.random(16, "abcdefghijklmnopqrstuvwxyz0123456789")));
        CaveTrackerManager.createCave(source.getServer(), levelKey);
        Entity entity = source.getEntity();
        if (entity != null) {
            ServerLevel destLevel = Objects.requireNonNull(source.getServer().getLevel(levelKey), "Dimension was not created");
            entity.teleportTo(destLevel, 0, 64, 0, Set.of(), 0, 0);
        }
        return Command.SINGLE_SUCCESS;
    }
}
