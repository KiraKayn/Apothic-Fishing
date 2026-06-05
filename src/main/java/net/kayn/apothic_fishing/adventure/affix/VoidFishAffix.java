package net.kayn.apothic_fishing.adventure.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.bonus.GemBonus;
import dev.shadowsoffire.placebo.util.StepFunction;
import net.kayn.apothic_fishing.registry.AFLootCategories;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class VoidFishAffix extends Affix {

    public static final Codec<VoidFishAffix> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(GemBonus.VALUES_CODEC.fieldOf("values").forGetter(a -> a.values))
            .apply(inst, VoidFishAffix::new));

    protected final Map<LootRarity, StepFunction> values;

    public VoidFishAffix(Map<LootRarity, StepFunction> values) {
        super(AffixType.ABILITY);
        this.values = values;
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        return (stack.getItem() instanceof FishingRodItem || cat == AFLootCategories.FISHING_ROD)
                && this.values.containsKey(rarity);
    }

    @Override
    public MutableComponent getDescription(ItemStack stack, LootRarity rarity, float level) {
        return Component.translatable("affix." + this.getId() + ".desc");
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }
}