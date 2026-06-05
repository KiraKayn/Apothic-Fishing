package net.kayn.apothic_fishing.client;

import dev.shadowsoffire.placebo.color.GradientColor;
import net.kayn.apothic_fishing.ApothicFishing;
import net.kayn.apothic_fishing.crate.CrateDefinition;
import net.kayn.apothic_fishing.crate.CrateItem;
import net.kayn.apothic_fishing.crate.CrateRegistry;
import net.kayn.apothic_fishing.registry.AFItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD,
        modid = ApothicFishing.MOD_ID)
public class AFClientEvents {

    private static final ResourceLocation ANCIENT_RARITY_ID =
            new ResourceLocation("apotheosis", "ancient");

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                (stack, tintIndex) -> {
                    if (tintIndex != 0) return 0xFFFFFF;

                    ResourceLocation crateId = CrateItem.getCrateId(stack);
                    if (crateId == null) return 0xFFFFFF;

                    CrateDefinition crate = CrateRegistry.INSTANCE.getValue(crateId);
                    if (crate == null) return 0xFFFFFF;

                    ResourceLocation rarityId = crate.getRarityHolder().getId();
                    if (rarityId != null && rarityId.equals(ANCIENT_RARITY_ID)) {
                        return GradientColor.RAINBOW.getValue() & 0xFFFFFF;
                    }

                    return 0xFFFFFF;
                },
                AFItems.CRATE.get()
        );
    }
}