package net.kayn.apothic_fishing.util;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemRegistry;
import net.kayn.apothic_fishing.crate.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;

public final class CrateTooltipUtil {

    private CrateTooltipUtil() {}

    public static void appendLootTooltip(CrateDefinition crate, Consumer<Component> sink) {
        sink.accept(Component.empty());

        sink.accept(Component.translatable("tooltip.apothic_fishing.crate.possible_loot")
                .withStyle(ChatFormatting.GOLD));

        for (CrateLootEntry entry : crate.getLoot()) {
            String countStr = range(entry.getMinCount(), entry.getMaxCount());
            String nameStr  = Component.translatable(entry.getItem().getDescriptionId()).getString();

            sink.accept(bullet()
                    .append(chanceTag(pct(entry.getChance())))
                    .append(text(" " + nameStr, ChatFormatting.YELLOW))
                    .append(text(" ×" + countStr, ChatFormatting.GRAY)));
        }

        for (CrateGemEntry entry : crate.getGems()) {
            Component gemName = resolveGemDisplayName(entry);
            sink.accept(bullet()
                    .append(chanceTag(pct(entry.getChance())))
                    .append(Component.literal(" ").withStyle(ChatFormatting.RESET))
                    .append(gemName));
        }

        for (CrateRandomGemEntry entry : crate.getRandomGems()) {
            Component label = Component.translatable("tooltip.apothic_fishing.crate.random_gem", entry.getRarity().toComponent()).withStyle(ChatFormatting.YELLOW);
            sink.accept(chanceEntry(pct(entry.getChance()), label));
        }

        for (CrateRandomEnchantmentEntry entry : crate.getRandomEnchantments()) {
            String maxLevelStr = entry.getMaxLevel() <= 0 ? "Max" : String.valueOf(entry.getMaxLevel());
            String levelRangeStr = (entry.getMinLevel() == entry.getMaxLevel()) ? String.valueOf(entry.getMinLevel()) : (entry.getMinLevel() + "\u2013" + maxLevelStr);
            String levelStr = " (Levels " + levelRangeStr + ")";

            MutableComponent label = Component.translatable("tooltip.apothic_fishing.crate.random_enchantment")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(text(levelStr, ChatFormatting.GRAY));

            sink.accept(chanceEntry(pct(entry.getChance()), label));
        }

        for (CrateGearSetEntry entry : crate.getGearSets()) {
            String rarityName = crate.getRarity().toComponent().getString();

            sink.accept(bullet()
                    .append(chanceTag(pct(entry.getChance())))
                    .append(text(" " + rarityName + " Gear Set", ChatFormatting.YELLOW)));

            List<ItemStack> potentials = entry.getAllPotentialItems();
            for (int i = 0; i < potentials.size(); i += 4) {
                MutableComponent lineComp = Component.literal("   ").withStyle(ChatFormatting.GRAY);
                int end = Math.min(i + 4, potentials.size());
                for (int j = i; j < end; j++) {
                    String itemName = Component.translatable(
                            potentials.get(j).getItem().getDescriptionId()).getString();
                    if (j > i) lineComp = lineComp.append(text(", ", ChatFormatting.GRAY));
                    lineComp = lineComp.append(text(itemName, ChatFormatting.GRAY));
                }
                sink.accept(lineComp);
            }
        }
    }

    private static MutableComponent chanceEntry(String chance, Component label) {
        return bullet()
                .append(chanceTag(chance))
                .append(Component.literal(" "))
                .append(label);
    }

    private static MutableComponent chanceTag(String chance) {
        return Component.literal("[").withStyle(ChatFormatting.GREEN)
                .append(text(chance, ChatFormatting.GREEN))
                .append(text("]", ChatFormatting.GREEN));
    }

    private static Component resolveGemDisplayName(CrateGemEntry entry) {
        try {
            ItemStack preview = entry.createGemStack();
            if (!preview.isEmpty()) return preview.getHoverName();
        } catch (Exception ignored) {}
        return text(entry.getGemId().toString(), ChatFormatting.AQUA);
    }

    private static MutableComponent bullet() {
        return Component.literal("• ").withStyle(ChatFormatting.GREEN);
    }

    private static MutableComponent text(String str, ChatFormatting colour) {
        return Component.literal(str).withStyle(colour);
    }

    private static String pct(float f) {
        return String.format("%.1f%%", f * 100f);
    }

    private static String range(int min, int max) {
        return (min == max) ? String.valueOf(min) : (min + "\u2013" + max);
    }
}