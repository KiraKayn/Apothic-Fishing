package net.kayn.apothic_fishing.client;

import net.kayn.apothic_fishing.ApothicFishing;
import net.kayn.apothic_fishing.crate.CrateItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = ApothicFishing.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {


    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        try {
            var rm = Minecraft.getInstance().getResourceManager();
            var found = rm.listResources("models/item/crates", rl -> rl.getPath().endsWith(".json"));

            for (ResourceLocation fileRl : found.keySet()) {
                String path = fileRl.getPath();
                path = path.substring("models/".length(), path.length() - ".json".length());
                ResourceLocation modelRl = new ResourceLocation(fileRl.getNamespace(), path);
                event.register(modelRl);
                ApothicFishing.LOGGER.debug("[ApothicFishing] Registered crate model: {}", modelRl);
            }
        } catch (Exception e) {
            ApothicFishing.LOGGER.warn("[ApothicFishing] Failed to scan for crate models: {}", e.getMessage());
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        ModelResourceLocation crateMrl = new ModelResourceLocation(
                new ResourceLocation(ApothicFishing.MOD_ID, "crate"), "inventory");

        BakedModel base = event.getModels().get(crateMrl);
        if (base == null) {
            ApothicFishing.LOGGER.error("[ApothicFishing] Base crate model not found – dynamic textures disabled.");
            return;
        }

        event.getModels().put(crateMrl, new CrateDynamicModel(base));
    }

    static final class CrateDynamicModel extends BakedModelWrapper<BakedModel> {

        private final ItemOverrides overrides;

        CrateDynamicModel(BakedModel base) {
            super(base);
            BakedModel fallback = base;

            this.overrides = new ItemOverrides() {
                @Override
                public @Nullable BakedModel resolve(BakedModel model,
                                                    ItemStack stack,
                                                    @Nullable ClientLevel level,
                                                    @Nullable LivingEntity entity,
                                                    int seed) {
                    ResourceLocation crateId = CrateItem.getCrateId(stack);
                    if (crateId == null) return fallback;

                    ResourceLocation key = new ResourceLocation(
                            crateId.getNamespace(),
                            "item/crates/" + crateId.getPath()
                    );

                    Minecraft mc = Minecraft.getInstance();
                    BakedModel found = mc.getModelManager().getModel(key);

                    return (found != mc.getModelManager().getMissingModel()) ? found : fallback;
                }
            };
        }

        @Override
        public ItemOverrides getOverrides() {
            return overrides;
        }
    }
}