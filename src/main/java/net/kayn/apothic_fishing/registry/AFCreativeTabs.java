package net.kayn.apothic_fishing.registry;

import net.kayn.apothic_fishing.ApothicFishing;
import net.kayn.apothic_fishing.crate.CrateItem;
import net.kayn.apothic_fishing.crate.CrateRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class AFCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ApothicFishing.MOD_ID);

    public static final RegistryObject<CreativeModeTab> CRATE_TAB = TABS.register("tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + ApothicFishing.MOD_ID + ".tab"))
                    .icon(() -> {
                        ResourceLocation firstCrateId = CrateRegistry.INSTANCE.getKeys().stream()
                                .findFirst()
                                .orElse(new ResourceLocation(ApothicFishing.MOD_ID, "default_crate"));
                        return CrateItem.createCrateStack(firstCrateId);
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }
}
