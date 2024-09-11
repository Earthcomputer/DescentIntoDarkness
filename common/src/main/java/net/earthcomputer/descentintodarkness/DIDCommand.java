package net.earthcomputer.descentintodarkness;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import net.earthcomputer.descentintodarkness.generator.CaveGenProgressListener;
import net.earthcomputer.descentintodarkness.generator.DIDChunkGenerator;
import net.earthcomputer.descentintodarkness.generator.PlayerProgressListener;
import net.earthcomputer.descentintodarkness.instancing.CaveTrackerManager;
import net.earthcomputer.descentintodarkness.style.CaveStyle;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.RandomSupport;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Set;

import static com.mojang.brigadier.arguments.BoolArgumentType.*;
import static com.mojang.brigadier.arguments.IntegerArgumentType.*;
import static com.mojang.brigadier.arguments.LongArgumentType.*;
import static net.minecraft.commands.Commands.*;
import static net.minecraft.commands.arguments.ResourceArgument.*;

public final class DIDCommand {
    private static final Logger LOGGER = LogUtils.getLogger();

    private DIDCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        var command = dispatcher.register(literal("did")
            .then(literal("generate")
                .then(argument("style", resource(context, DIDRegistries.CAVE_STYLE))
                    .executes(ctx -> generate(ctx.getSource(), getResource(ctx, "style", DIDRegistries.CAVE_STYLE), null, RandomSupport.generateUniqueSeed(), false))
                    .then(argument("size", integer(3, 100))
                        .executes(ctx -> generate(ctx.getSource(), getResource(ctx, "style", DIDRegistries.CAVE_STYLE), getInteger(ctx, "size"), RandomSupport.generateUniqueSeed(), false))
                        .then(argument("seed", longArg())
                            .executes(ctx -> generate(ctx.getSource(), getResource(ctx, "style", DIDRegistries.CAVE_STYLE), getInteger(ctx, "size"), getLong(ctx, "seed"), false))
                            .then(argument("debug", bool())
                                .executes(ctx -> generate(ctx.getSource(), getResource(ctx, "style", DIDRegistries.CAVE_STYLE), getInteger(ctx, "size"), getLong(ctx, "seed"), getBool(ctx, "debug")))))))));
        dispatcher.register(literal("descentintodarkness").redirect(command));
    }

    private static int generate(CommandSourceStack source, Holder<CaveStyle> style, @Nullable Integer size, long seed, boolean debug) {
        Entity entity = source.getEntity();
        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, DescentIntoDarkness.id("cave_" + RandomStringUtils.random(16, "abcdefghijklmnopqrstuvwxyz0123456789")));
        CaveGenProgressListener listener = entity instanceof ServerPlayer player ? new PlayerProgressListener(player) : CaveGenProgressListener.EMPTY;
        CaveTrackerManager.createCave(source.getServer(), levelKey, style, size == null ? style.value().size().sample(RandomSource.create()) : size, seed, debug, listener);
        if (entity != null) {
            ServerLevel destLevel = Objects.requireNonNull(source.getServer().getLevel(levelKey), "Dimension was not created");
            destLevel.getChunkSource().addRegionTicket(DIDChunkGenerator.DID_CAVE_GEN, ChunkPos.ZERO, 0, ChunkPos.ZERO);
            destLevel.getChunkSource().getChunkFutureMainThread(0, 0, ChunkStatus.FULL, true).whenComplete((result, throwable) -> source.getServer().execute(() -> {
                destLevel.getChunkSource().removeRegionTicket(DIDChunkGenerator.DID_CAVE_GEN, ChunkPos.ZERO, 0, ChunkPos.ZERO);
                listener.finish();
                if (result != null && result.isSuccess()) {
                    entity.teleportTo(destLevel, 0, 64, 0, Set.of(), 0, 0);
                } else {
                    if (throwable != null) {
                        LOGGER.error("Failed to generate cave chunk", throwable);
                    }
                }
            }));
        }
        return Command.SINGLE_SUCCESS;
    }
}
