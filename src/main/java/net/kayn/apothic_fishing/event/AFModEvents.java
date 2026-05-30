package net.kayn.apothic_fishing.event;

import net.kayn.apothic_fishing.ApothicFishing;
import net.kayn.apothic_fishing.crate.CrateItem;
import net.kayn.apothic_fishing.registry.AFCreativeTabs;
import net.kayn.apothic_fishing.crate.CrateRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApothicFishing.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AFModEvents {

    @SubscribeEvent
    public static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == AFCreativeTabs.CRATE_TAB.get()) {

            CrateRegistry.INSTANCE.getValues().forEach(crate -> {
                ResourceLocation crateId = CrateRegistry.INSTANCE.getKey(crate);
                if (crateId != null) {
                    event.accept(CrateItem.createCrateStack(crateId));
                }
            });

        }
    }
}
