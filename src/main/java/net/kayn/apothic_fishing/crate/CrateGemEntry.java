package net.kayn.apothic_fishing.crate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.adventure.Adventure;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class CrateGemEntry {

    public static final Codec<CrateGemEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ResourceLocation.CODEC
                    .fieldOf("gem").forGetter(e -> e.gemId),
            LootRarity.HOLDER_CODEC
                    .fieldOf("rarity").forGetter(e -> e.rarity),
            Codec.floatRange(0f, 1f)
                    .fieldOf("chance").forGetter(e -> e.chance)
    ).apply(inst, CrateGemEntry::new));

    private final ResourceLocation gemId;
    private final DynamicHolder<LootRarity> rarity;
    private final float chance;

    public CrateGemEntry(ResourceLocation gemId, DynamicHolder<LootRarity> rarity, float chance) {
        this.gemId = gemId;
        this.rarity = rarity;
        this.chance = chance;
    }

    public ResourceLocation getGemId()                     { return gemId; }
    public LootRarity getRarity()                          { return rarity.get(); }
    public DynamicHolder<LootRarity> getRarityHolder()     { return rarity; }
    public float getChance()                               { return chance; }

    public ItemStack createGemStack() {
        Gem gem = GemRegistry.INSTANCE.getValue(gemId);
        if (gem == null) return ItemStack.EMPTY;

        ItemStack stack = new ItemStack(Adventure.Items.GEM.get());
        GemItem.setGem(stack, gem);
        AffixHelper.setRarity(stack, rarity.get());
        return stack;
    }
}