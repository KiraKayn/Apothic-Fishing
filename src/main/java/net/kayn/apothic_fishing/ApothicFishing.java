package net.kayn.apothic_fishing;

import net.kayn.apothic_fishing.crate.CrateRegistry;
import net.kayn.apothic_fishing.registry.AFCreativeTabs;
import net.kayn.apothic_fishing.registry.AFItems;
import net.kayn.apothic_fishing.registry.AFLootCategories;
import net.kayn.apothic_fishing.registry.AFModifiers;
import net.kayn.fallen_gems_affixes.loot.StaffLootCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ApothicFishing.MOD_ID)
public class ApothicFishing {

    public static final String MOD_ID = "apothic_fishing";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public ApothicFishing() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);

        AFItems.ITEMS.register(modBus);
        AFModifiers.LOOT_MODIFIERS.register(modBus);

        CrateRegistry.INSTANCE.registerToBus();
        AFCreativeTabs.register(modBus);
       AFLootCategories.FISHING_ROD.toString();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }
}