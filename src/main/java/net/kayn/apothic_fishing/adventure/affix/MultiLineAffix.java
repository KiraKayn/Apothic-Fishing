package net.kayn.apothic_fishing.adventure.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
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

public class MultiLineAffix extends Affix {

    public static final Codec<MultiLineAffix> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(GemBonus.VALUES_CODEC.fieldOf("values").forGetter(a -> a.values))
            .apply(inst, MultiLineAffix::new));

    protected final Map<LootRarity, StepFunction> values;

    public MultiLineAffix(Map<LootRarity, StepFunction> values) {
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
        int extraLines = this.values.get(rarity).getInt(level);
        return Component.translatable("affix." + this.getId() + ".desc", extraLines);
    }

    public static int getTotalExtraLines(ItemStack rod) {
        int count = 0;
        for (var inst : AffixHelper.getAffixes(rod).values()) {
            if (!inst.isValid()) continue;
            if (!(inst.affix().get() instanceof MultiLineAffix affix)) continue;
            var stepFn = affix.getValues().get(inst.rarity().get());
            if (stepFn == null) continue;
            count += stepFn.getInt(inst.level());
        }
        return count;
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    public Map<LootRarity, StepFunction> getValues() {
        return values;
    }
}