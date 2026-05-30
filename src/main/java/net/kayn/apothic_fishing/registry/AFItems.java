package net.kayn.apothic_fishing.registry;

import net.kayn.apothic_fishing.ApothicFishing;
import net.kayn.apothic_fishing.crate.CrateItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class AFItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ApothicFishing.MOD_ID);


    public static final RegistryObject<CrateItem> CRATE = ITEMS.register("crate",
            () -> new CrateItem(new Item.Properties().stacksTo(16)));
}