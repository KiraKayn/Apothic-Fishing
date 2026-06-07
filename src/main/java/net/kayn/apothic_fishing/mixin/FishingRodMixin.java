package net.kayn.apothic_fishing.mixin;

import net.kayn.apothic_fishing.adventure.affix.ModHelper;
import net.kayn.apothic_fishing.api.ModPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(FishingRodItem.class)
public class FishingRodMixin extends Item {

    public FishingRodMixin(Properties props) {
        super(props);
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    public void use(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack stack = player.getItemInHand(hand);
        ModPlayer modPlayer = (ModPlayer) player;

        FishingHook looking = ModHelper.getLookingHook(modPlayer, stack);

        if (looking == null) {
            if (ModHelper.canCastHook(modPlayer, stack)) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL,
                        0.5f, 0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
                if (!level.isClientSide) {
                    int lure = EnchantmentHelper.getFishingSpeedBonus(stack);
                    int luck = EnchantmentHelper.getFishingLuckBonus(stack);
                    FishingHook newHook = new FishingHook(player, level, luck, lure);
                    level.addFreshEntity(newHook);
                    player.fishing = newHook;
                }
                player.awardStat(Stats.ITEM_USED.get(this));
                player.gameEvent(GameEvent.ITEM_INTERACT_START);
                cir.setReturnValue(InteractionResultHolder.sidedSuccess(stack, level.isClientSide()));
            }
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL,
                    1.0f, 0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
            player.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
            if (!level.isClientSide) {
                int damage = looking.retrieve(stack);
                stack.hurtAndBreak(damage, player, p -> p.broadcastBreakEvent(hand));
                List<FishingHook> remaining = modPlayer.apothic$getHooks();
                player.fishing = remaining.isEmpty() ? null : remaining.get(0);
            }
            cir.setReturnValue(InteractionResultHolder.sidedSuccess(stack, level.isClientSide()));
        }
    }
}