package net.kayn.apothic_fishing.mixin;

import net.kayn.apothic_fishing.adventure.affix.ModHelper;
import net.kayn.apothic_fishing.adventure.affix.SpecialFishingHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingHook.class)
public abstract class FishingHookRetrieveMixin {

    @Inject(method = "retrieve", at = @At("HEAD"), cancellable = true)
    private void onRetrieveHead(ItemStack rod, CallbackInfoReturnable<Integer> cir) {
        FishingHook self = (FishingHook) (Object) this;
        if (self.level().isClientSide()) return;

        Player player = self.getPlayerOwner();
        if (player == null) return;

        ItemStack heldRod = ModHelper.getHeldRod(player);
        if (heldRod == null) return;

        if (SpecialFishingHandler.isSpecialHook(self, heldRod)) {
            int damage = SpecialFishingHandler.retrieveSpecial(self, heldRod);
            cir.setReturnValue(damage);
        }
    }

    @Inject(
            method = "retrieve",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/FishingHook;discard()V"),
            cancellable = true
    )
    private void onRetrieveBeforeDiscard(ItemStack rod, CallbackInfoReturnable<Integer> cir) {
        FishingHook self = (FishingHook) (Object) this;
        if (self.level().isClientSide()) return;

        Player player = self.getPlayerOwner();
        if (player == null) return;

        ItemStack heldRod = ModHelper.getHeldRod(player);
        if (heldRod != null && SpecialFishingHandler.isSpecialHook(self, heldRod)) {
            return;
        }

        Entity hooked = ((FishingHookAccessor) self).getHookedIn();
        if (hooked != null) {
            double dx = player.getX() - hooked.getX();
            double dy = player.getY() - hooked.getY();
            double dz = player.getZ() - hooked.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > 0) {
                double speed = Math.min(1.5, 0.18 * dist);
                hooked.setDeltaMovement(dx / dist * speed, dy / dist * speed + 0.25, dz / dist * speed);
                hooked.hasImpulse = true;
            }
            self.discard();
            cir.setReturnValue(1);
            cir.cancel();
        }
    }
}