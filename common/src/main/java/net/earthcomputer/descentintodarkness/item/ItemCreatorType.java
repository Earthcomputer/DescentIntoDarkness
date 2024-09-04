package net.earthcomputer.descentintodarkness.item;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.registries.RegistrySupplier;
import net.earthcomputer.descentintodarkness.DIDRegistries;
import net.earthcomputer.descentintodarkness.DescentIntoDarkness;

public interface ItemCreatorType<C extends ItemCreator> {
    RegistrySupplier<ItemCreatorType<SimpleItemCreator>> SIMPLE = register("simple", SimpleItemCreator.CODEC);
    RegistrySupplier<ItemCreatorType<ArmorItemCreator>> ARMOR = register("armor", ArmorItemCreator.CODEC);
    RegistrySupplier<ItemCreatorType<SwordItemCreator>> SWORD = register("sword", SwordItemCreator.CODEC);
    RegistrySupplier<ItemCreatorType<PickaxeItemCreator>> PICKAXE = register("pickaxe", PickaxeItemCreator.CODEC);
    RegistrySupplier<ItemCreatorType<AxeItemCreator>> AXE = register("axe", AxeItemCreator.CODEC);
    RegistrySupplier<ItemCreatorType<ShovelItemCreator>> SHOVEL = register("shovel", ShovelItemCreator.CODEC);
    RegistrySupplier<ItemCreatorType<HoeItemCreator>> HOE = register("hoe", HoeItemCreator.CODEC);

    MapCodec<C> codec();

    static void register() {
        // load class
    }

    static <C extends ItemCreator> RegistrySupplier<ItemCreatorType<C>> register(String id, MapCodec<C> codec) {
        return DIDRegistries.REGISTRAR_MANAGER.get(DIDRegistries.ITEM_CREATOR_TYPE).register(DescentIntoDarkness.id(id), () -> () -> codec);
    }
}
