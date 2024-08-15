package net.earthcomputer.descentintodarkness;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.earthcomputer.descentintodarkness.instancing.CaveTrackerManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.RandomSupport;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Set;

import static com.mojang.brigadier.arguments.BoolArgumentType.*;
import static com.mojang.brigadier.arguments.IntegerArgumentType.*;
import static com.mojang.brigadier.arguments.LongArgumentType.*;
import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static net.minecraft.commands.Commands.*;

public final class DIDCommand {
    private DIDCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var command = dispatcher.register(literal("did")
            .then(literal("generate")
                .then(argument("style", string())
                    .executes(ctx -> generate(ctx.getSource(), getString(ctx, "style"), 7, RandomSupport.generateUniqueSeed(), false))
                    .then(argument("size", integer(3, 100))
                        .executes(ctx -> generate(ctx.getSource(), getString(ctx, "style"), getInteger(ctx, "size"), RandomSupport.generateUniqueSeed(), false))
                        .then(argument("seed", longArg())
                            .executes(ctx -> generate(ctx.getSource(), getString(ctx, "style"), getInteger(ctx, "size"), getLong(ctx, "seed"), false))
                            .then(argument("debug", bool())
                                .executes(ctx -> generate(ctx.getSource(), getString(ctx, "style"), getInteger(ctx, "size"), getLong(ctx, "seed"), getBool(ctx, "debug")))))))));
        dispatcher.register(literal("descentintodarkness").redirect(command));
    }

    private static int generate(CommandSourceStack source, String style, int size, long seed, boolean debug) {
        try {
            ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(DescentIntoDarkness.MOD_ID, "cave_" + RandomStringUtils.random(16, "abcdefghijklmnopqrstuvwxyz0123456789")));
            CaveTrackerManager.createCave(source.getServer(), levelKey);
            Entity entity = source.getEntity();
            if (entity != null) {
                entity.teleportTo(source.getServer().getLevel(levelKey), 0, 64, 0, Set.of(), 0, 0);
            }
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
