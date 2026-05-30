package net.kayn.apothic_fishing.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.kayn.apothic_fishing.crate.CrateItem;
import net.kayn.apothic_fishing.registry.AFItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

@JeiPlugin
public class AFJeiPlugin implements IModPlugin {

    private static final String MODID = "apothic_fishing";

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(MODID, "crate_compat");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration reg) {
        reg.registerSubtypeInterpreter(AFItems.CRATE.get(), new CrateSubtypes());
    }

    private static class CrateSubtypes implements IIngredientSubtypeInterpreter<ItemStack> {

        @Override
        public String apply(ItemStack stack, UidContext context) {
            ResourceLocation crateId = CrateItem.getCrateId(stack);
            if (crateId == null) {
                return ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
            }
            return crateId.toString();
        }
    }
}