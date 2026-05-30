package net.kayn.apothic_fishing.crate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.adventure.Adventure;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemRegistry;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CrateRandomGemEntry {

    public static final Codec<CrateRandomGemEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            LootRarity.HOLDER_CODEC
                    .fieldOf("rarity").forGetter(e -> e.rarity),
            Codec.floatRange(0f, 1f)
                    .fieldOf("chance").forGetter(e -> e.chance),
            PlaceboCodecs.nullableField(
                            PlaceboCodecs.setOf(ResourceLocation.CODEC), "blacklist", Collections.emptySet())
                    .forGetter(e -> e.blacklist)
    ).apply(inst, CrateRandomGemEntry::new));

    private final DynamicHolder<LootRarity> rarity;
    private final float chance;
    private final Set<ResourceLocation> blacklist;

    public CrateRandomGemEntry(DynamicHolder<LootRarity> rarity, float chance, Set<ResourceLocation> blacklist) {
        this.rarity = rarity;
        this.chance = chance;
        this.blacklist = blacklist;
    }

    public LootRarity getRarity() {
        return rarity.get();
    }

    public DynamicHolder<LootRarity> getRarityHolder() {
        return rarity;
    }

    public float getChance() {
        return chance;
    }

    public Set<ResourceLocation> getBlacklist() {
        return blacklist;
    }

    public ItemStack createRandomGemStack(RandomSource rand) {
        LootRarity r = rarity.get();
        List<Gem> eligible = getEligibleGems(r);

        if (eligible.isEmpty()) return ItemStack.EMPTY;

        Gem chosen = eligible.get(rand.nextInt(eligible.size()));
        ItemStack stack = new ItemStack(Adventure.Items.GEM.get());
        GemItem.setGem(stack, chosen);
        AffixHelper.setRarity(stack, r);
        return stack;
    }

    public List<Gem> getEligibleGems(LootRarity r) {
        return GemRegistry.INSTANCE.getValues().stream()
                .filter(gem -> {
                    return r.isAtLeast(gem.getMinRarity()) && r.isAtMost(gem.getMaxRarity());
                })
                .filter(gem -> {
                    ResourceLocation key = GemRegistry.INSTANCE.getKey(gem);
                    return key == null || !blacklist.contains(key);
                })
                .collect(Collectors.toList());
    }
}