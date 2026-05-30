package net.kayn.apothic_fishing.crate;

import net.kayn.apothic_fishing.registry.AFItems;
import net.kayn.apothic_fishing.util.CrateTooltipUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.ResourceLocationException;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CrateItem extends Item {

    public static final String CRATE_TAG = "crate";

    public CrateItem(Properties properties) {
        super(properties);
    }


    @Override
    public void appendHoverText(ItemStack stack,
                                @Nullable Level level,
                                List<Component> tooltip,
                                TooltipFlag flag) {

        ResourceLocation crateId = getCrateId(stack);
        if (crateId == null) {
            tooltip.add(Component.translatable("tooltip.apothic_fishing.crate.invalid")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        CrateDefinition crate = CrateRegistry.INSTANCE.getValue(crateId);
        if (crate == null) {
            tooltip.add(Component.translatable("tooltip.apothic_fishing.crate.not_loaded")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        if (Screen.hasShiftDown()) {
            CrateTooltipUtil.appendLootTooltip(crate, tooltip::add);
        } else {
            tooltip.add(Component.translatable("tooltip.apothic_fishing.crate.shift")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation crateId = getCrateId(stack);
        if (crateId != null) {
            CrateDefinition crate = CrateRegistry.INSTANCE.getValue(crateId);
            if (crate != null) {
                String translKey = "crate."
                        + crateId.getNamespace()
                        + "."
                        + crateId.getPath().replace('/', '.');
                return Component.translatable(translKey)
                        .withStyle(Style.EMPTY.withColor(crate.getRarity().getColor()));
            }
        }
        return super.getName(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        ResourceLocation crateId = getCrateId(stack);
        if (crateId == null) return InteractionResultHolder.fail(stack);

        if (!level.isClientSide()) {
            CrateDefinition crate = CrateRegistry.INSTANCE.getValue(crateId);
            if (crate == null) return InteractionResultHolder.fail(stack);

            openCrate(level, player, crate);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static void openCrate(Level level, Player player, CrateDefinition crate) {
        var rand = level.getRandom();

        for (CrateLootEntry entry : crate.getLoot()) {
            if (rand.nextFloat() < entry.getChance()) {
                giveItem(player, entry.createStack(rand));
            }
        }

        for (CrateGemEntry entry : crate.getGems()) {
            if (rand.nextFloat() < entry.getChance()) {
                ItemStack gem = entry.createGemStack();
                if (!gem.isEmpty()) giveItem(player, gem);
            }
        }

        for (CrateRandomGemEntry entry : crate.getRandomGems()) {
            if (rand.nextFloat() < entry.getChance()) {
                ItemStack gem = entry.createRandomGemStack(rand);
                if (!gem.isEmpty()) giveItem(player, gem);
            }
        }

        for (CrateRandomEnchantmentEntry entry : crate.getRandomEnchantments()) {
            if (rand.nextFloat() < entry.getChance()) {
                ItemStack book = entry.createEnchantedBook(rand);
                if (!book.isEmpty()) giveItem(player, book);
            }
        }

        for (CrateGearSetEntry entry : crate.getGearSets()) {
            if (rand.nextFloat() < entry.getChance()) {
                for (ItemStack gearItem : entry.generateItems(rand, crate.getRarity())) {
                    if (!gearItem.isEmpty()) giveItem(player, gearItem);
                }
            }
        }

        level.playSound(null, player.blockPosition(),
                SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private static void giveItem(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }


    @Nullable
    public static ResourceLocation getCrateId(ItemStack stack) {
        if (!stack.hasTag()) return null;
        String raw = stack.getOrCreateTag().getString(CRATE_TAG);
        if (raw.isEmpty()) return null;
        try {
            return new ResourceLocation(raw);
        } catch (ResourceLocationException e) {
            return null;
        }
    }

    public static ItemStack createCrateStack(ResourceLocation crateId) {
        ItemStack stack = new ItemStack(AFItems.CRATE.get());
        stack.getOrCreateTag().putString(CRATE_TAG, crateId.toString());
        return stack;
    }
}