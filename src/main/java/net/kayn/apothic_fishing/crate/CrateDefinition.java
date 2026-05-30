package net.kayn.apothic_fishing.crate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import dev.shadowsoffire.placebo.reload.WeightedDynamicRegistry.ILuckyWeighted;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CrateDefinition implements CodecProvider<CrateDefinition>, ILuckyWeighted {

    public static final Codec<CrateDefinition> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            LootRarity.HOLDER_CODEC
                    .fieldOf("rarity").forGetter(CrateDefinition::getRarityHolder),
            Codec.intRange(0, Integer.MAX_VALUE)
                    .fieldOf("weight").forGetter(CrateDefinition::getWeight),
            PlaceboCodecs.nullableField(Codec.floatRange(0f, Float.MAX_VALUE), "quality", 0f)
                    .forGetter(CrateDefinition::getQuality),
            PlaceboCodecs.nullableField(PlaceboCodecs.setOf(ResourceLocation.CODEC),
                            "dimensions", Collections.emptySet())
                    .forGetter(CrateDefinition::getDimensions),
            PlaceboCodecs.nullableField(PlaceboCodecs.setOf(ResourceLocation.CODEC),
                            "biomes", Collections.emptySet())
                    .forGetter(CrateDefinition::getBiomes),
            PlaceboCodecs.nullableField(CrateLootEntry.CODEC.listOf(),
                            "loot", Collections.emptyList())
                    .forGetter(CrateDefinition::getLoot),
            PlaceboCodecs.nullableField(CrateGemEntry.CODEC.listOf(),
                            "gems", Collections.emptyList())
                    .forGetter(CrateDefinition::getGems),
            PlaceboCodecs.nullableField(CrateRandomGemEntry.CODEC.listOf(),
                            "random_gems", Collections.emptyList())
                    .forGetter(CrateDefinition::getRandomGems),
            PlaceboCodecs.nullableField(CrateGearSetEntry.CODEC.listOf(),
                            "gear_sets", Collections.emptyList())
                    .forGetter(CrateDefinition::getGearSets),
            PlaceboCodecs.nullableField(CrateRandomEnchantmentEntry.CODEC.listOf(),
                            "random_enchantments", Collections.emptyList())
                    .forGetter(CrateDefinition::getRandomEnchantments)
    ).apply(inst, CrateDefinition::new));

    private final DynamicHolder<LootRarity> rarity;
    private final int weight;
    private final float quality;
    private final Set<ResourceLocation> dimensions;
    private final Set<ResourceLocation> biomes;
    private final List<CrateLootEntry> loot;
    private final List<CrateGemEntry> gems;
    private final List<CrateRandomGemEntry> randomGems;
    private final List<CrateGearSetEntry> gearSets;
    private final List<CrateRandomEnchantmentEntry> randomEnchantments;

    public CrateDefinition(DynamicHolder<LootRarity> rarity,
                           int weight,
                           float quality,
                           Set<ResourceLocation> dimensions,
                           Set<ResourceLocation> biomes,
                           List<CrateLootEntry> loot,
                           List<CrateGemEntry> gems,
                           List<CrateRandomGemEntry> randomGems,
                           List<CrateGearSetEntry> gearSets,
                           List<CrateRandomEnchantmentEntry> randomEnchantments) {

        this.rarity = rarity;
        this.weight = weight;
        this.quality = quality;
        this.dimensions = dimensions;
        this.biomes = biomes;
        this.loot = loot;
        this.gems = gems;
        this.randomGems = randomGems;
        this.gearSets = gearSets;
        this.randomEnchantments = randomEnchantments;
    }

    public LootRarity getRarity()                          { return rarity.get(); }
    public DynamicHolder<LootRarity> getRarityHolder()     { return rarity; }

    @Override public int getWeight()                       { return weight; }
    @Override public float getQuality()                    { return quality; }

    public Set<ResourceLocation> getDimensions()           { return dimensions; }
    public Set<ResourceLocation> getBiomes()               { return biomes; }
    public List<CrateLootEntry> getLoot()                  { return loot; }
    public List<CrateGemEntry> getGems()                   { return gems; }
    public List<CrateRandomGemEntry> getRandomGems()       { return randomGems; }
    public List<CrateGearSetEntry> getGearSets()           { return gearSets; }
    public List<CrateRandomEnchantmentEntry> getRandomEnchantments() { return randomEnchantments; }

    @Override
    public Codec<? extends CrateDefinition> getCodec() {
        return CODEC;
    }
}