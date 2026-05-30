package net.kayn.apothic_fishing.crate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class CrateLootEntry {

    public static final Codec<CrateLootEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ForgeRegistries.ITEMS.getCodec()
                    .fieldOf("item").forGetter(e -> e.item),
            Codec.floatRange(0f, 1f)
                    .fieldOf("chance").forGetter(e -> e.chance),
            PlaceboCodecs.nullableField(Codec.intRange(1, 64), "min_count", 1)
                    .forGetter(e -> e.minCount),
            PlaceboCodecs.nullableField(Codec.intRange(1, 64), "max_count", 1)
                    .forGetter(e -> e.maxCount)
    ).apply(inst, CrateLootEntry::new));

    private final Item item;
    private final float chance;
    private final int minCount;
    private final int maxCount;

    public CrateLootEntry(Item item, float chance, int minCount, int maxCount) {
        this.item = item;
        this.chance = chance;
        this.minCount = minCount;
        this.maxCount = maxCount;
    }

    public Item getItem() { return item; }
    public float getChance() { return chance; }
    public int getMinCount() { return minCount; }
    public int getMaxCount() { return maxCount; }

    public ItemStack createStack(RandomSource rand) {
        int count = (minCount >= maxCount)
                ? minCount
                : minCount + rand.nextInt(maxCount - minCount + 1);
        return new ItemStack(item, count);
    }
}