package net.kayn.apothic_fishing.crate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootController;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.placebo.json.GearSet;
import dev.shadowsoffire.placebo.json.GearSetRegistry;
import dev.shadowsoffire.placebo.json.WeightedItemStack;
import net.kayn.apothic_fishing.ApothicFishing;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CrateGearSetEntry {

    public static final Codec<CrateGearSetEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            GearSet.SetPredicate.CODEC
                    .fieldOf("gear_set").forGetter(e -> e.predicate),
            Codec.floatRange(0f, 1f)
                    .fieldOf("chance").forGetter(e -> e.chance)
    ).apply(inst, CrateGearSetEntry::new));

    private final GearSet.SetPredicate predicate;
    private final float chance;

    public CrateGearSetEntry(GearSet.SetPredicate predicate, float chance) {
        this.predicate = predicate;
        this.chance = chance;
    }

    public GearSet.SetPredicate getPredicate() {
        return predicate;
    }

    public float getChance() {
        return chance;
    }

    public List<ItemStack> generateItems(RandomSource rand, LootRarity rarity) {
        GearSet gearSet = GearSetRegistry.INSTANCE.getRandomSet(
                rand, 0f, Collections.singletonList(predicate));
        if (gearSet == null) return Collections.emptyList();

        List<WeightedItemStack> pool = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            pool.addAll(gearSet.getPotentials(slot));
        }

        if (pool.isEmpty()) return Collections.emptyList();

        Optional<WeightedItemStack> picked = WeightedRandom.getRandomItem(rand, pool);
        if (picked.isEmpty()) return Collections.emptyList();

        ItemStack stack = picked.get().getStack().copy();
        if (stack.isEmpty()) return Collections.emptyList();

        LootCategory cat = LootCategory.forItem(stack);
        if (!cat.isNone()) {
            try {
                stack = LootController.createLootItem(stack, cat, rarity, rand);
            } catch (Exception e) {
                ApothicFishing.LOGGER.warn(
                        "[ApothicFishing] Could not affix gear-set item {}: {}",
                        BuiltInRegistries.ITEM.getKey(stack.getItem()), e.getMessage());
            }
        }

        return Collections.singletonList(stack);
    }

    public List<ItemStack> getAllPotentialItems() {
        try {
            Set<String> seen = new LinkedHashSet<>();
            List<ItemStack> result = new ArrayList<>();

            for (GearSet set : GearSetRegistry.INSTANCE.getValues()) {
                if (!predicate.test(set)) continue;

                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    for (WeightedItemStack wis : set.getPotentials(slot)) {
                        ItemStack s = wis.getStack();
                        if (s.isEmpty()) continue;
                        String registryName = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
                        if (seen.add(registryName)) {
                            result.add(s.copy());
                        }
                    }
                }
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}