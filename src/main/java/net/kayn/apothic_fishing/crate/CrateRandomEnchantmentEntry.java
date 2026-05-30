package net.kayn.apothic_fishing.crate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CrateRandomEnchantmentEntry {

    public static final Codec<CrateRandomEnchantmentEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.floatRange(0f, 1f)
                    .fieldOf("chance").forGetter(e -> e.chance),
            PlaceboCodecs.nullableField(Codec.INT, "min_level", 1)
                    .forGetter(e -> e.minLevel),
            PlaceboCodecs.nullableField(Codec.INT, "max_level", -1)
                    .forGetter(e -> e.maxLevel),
            PlaceboCodecs.nullableField(Codec.BOOL, "allow_curses", false)
                    .forGetter(e -> e.allowCurses),
            PlaceboCodecs.nullableField(
                            PlaceboCodecs.setOf(ResourceLocation.CODEC), "blacklist", Collections.emptySet())
                    .forGetter(e -> e.blacklist)
    ).apply(inst, CrateRandomEnchantmentEntry::new));

    private final float chance;
    private final int minLevel;
    private final int maxLevel;
    private final boolean allowCurses;
    private final Set<ResourceLocation> blacklist;

    public CrateRandomEnchantmentEntry(float chance, int minLevel, int maxLevel, boolean allowCurses, Set<ResourceLocation> blacklist) {
        this.chance = chance;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.allowCurses = allowCurses;
        this.blacklist = blacklist;
    }

    public float getChance()            { return chance; }
    public int getMinLevel()            { return minLevel; }
    public int getMaxLevel()            { return maxLevel; }
    public boolean isAllowCurses()      { return allowCurses; }
    public Set<ResourceLocation> getBlacklist() { return blacklist; }

    public ItemStack createEnchantedBook(RandomSource rand) {
        List<Enchantment> eligible = ForgeRegistries.ENCHANTMENTS.getValues().stream()
                .filter(enchant -> this.allowCurses || !enchant.isCurse())
                .filter(enchant -> {
                    ResourceLocation key = ForgeRegistries.ENCHANTMENTS.getKey(enchant);
                    return key == null || !blacklist.contains(key);
                })
                .collect(Collectors.toList());

        if (eligible.isEmpty()) return ItemStack.EMPTY;

        Enchantment chosen = eligible.get(rand.nextInt(eligible.size()));

        int absoluteMin = Math.max(chosen.getMinLevel(), this.minLevel);
        int absoluteMax = this.maxLevel <= 0 ? chosen.getMaxLevel() : Math.min(chosen.getMaxLevel(), this.maxLevel);

        int finalLevel = absoluteMin <= absoluteMax ? Mth.nextInt(rand, absoluteMin, absoluteMax) : chosen.getMinLevel();

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantedBookItem.addEnchantment(book, new EnchantmentInstance(chosen, finalLevel));
        return book;
    }
}