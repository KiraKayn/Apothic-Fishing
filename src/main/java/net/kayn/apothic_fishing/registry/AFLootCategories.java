package net.kayn.apothic_fishing.registry;

import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.FishingRodItem;

public class AFLootCategories {


    public static final LootCategory FISHING_ROD = LootCategory.register(
            LootCategory.NONE,
            "fishing_rod",
            stack -> stack.getItem() instanceof FishingRodItem,
            new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}
    );

    public static void init() {
    }
}