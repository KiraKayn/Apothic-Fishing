package net.kayn.apothic_fishing.event;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixRegistry;
import net.kayn.apothic_fishing.adventure.affix.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = "apothic_fishing", bus = Mod.EventBusSubscriber.Bus.MOD)
public class AFCodecs {

    @SubscribeEvent
    public static void init(FMLCommonSetupEvent e) {
        e.enqueueWork(() -> {
            AffixRegistry.INSTANCE.registerCodec(new ResourceLocation("apothic_fishing", "multi_line"), MultiLineAffix.CODEC);
            AffixRegistry.INSTANCE.registerCodec(new ResourceLocation("apothic_fishing", "auto_fish"), AutoFishAffix.CODEC);
            AffixRegistry.INSTANCE.registerCodec(new ResourceLocation("apothic_fishing", "lava_fish"), LavaFishAffix.CODEC);
            AffixRegistry.INSTANCE.registerCodec(new ResourceLocation("apothic_fishing", "void_fish"), VoidFishAffix.CODEC);
        });
    }
}