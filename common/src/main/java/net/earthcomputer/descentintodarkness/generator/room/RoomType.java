package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.registries.RegistrySupplier;
import net.earthcomputer.descentintodarkness.DIDRegistries;
import net.earthcomputer.descentintodarkness.DescentIntoDarkness;

@FunctionalInterface
public interface RoomType<R extends Room> {
    RegistrySupplier<RoomType<SimpleRoom>> SIMPLE = register("simple", SimpleRoom.CODEC);
    RegistrySupplier<RoomType<TurnRoom>> TURN = register("turn", TurnRoom.CODEC);
    RegistrySupplier<RoomType<VerticalRoom>> VERTICAL = register("vertical", VerticalRoom.CODEC);
    RegistrySupplier<RoomType<BranchRoom>> BRANCH = register("branch", BranchRoom.CODEC);
    RegistrySupplier<RoomType<DropshaftRoom>> DROPSHAFT = register("dropshaft", DropshaftRoom.CODEC);
    RegistrySupplier<RoomType<CavernRoom>> CAVERN = register("cavern", CavernRoom.CODEC);
    RegistrySupplier<RoomType<RavineRoom>> RAVINE = register("ravine", RavineRoom.CODEC);
    RegistrySupplier<RoomType<PitMineRoom>> PIT_MINE = register("pit_mine", PitMineRoom.CODEC);
    RegistrySupplier<RoomType<ShelfRoom>> SHELF = register("shelf", ShelfRoom.CODEC);
    RegistrySupplier<RoomType<NilRoom>> NIL = register("nil", NilRoom.CODEC);

    MapCodec<R> codec();

    static void register() {
        // load class
    }

    static <R extends Room> RegistrySupplier<RoomType<R>> register(String id, MapCodec<R> codec) {
        return DIDRegistries.REGISTRAR_MANAGER.get(DIDRegistries.ROOM_TYPE).register(DescentIntoDarkness.id(id), () -> () -> codec);
    }
}
