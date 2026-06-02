package net.kayn.apothic_fishing.mixin;

import net.kayn.apothic_fishing.api.ModPlayer;
import net.kayn.apothic_fishing.adventure.affix.ModHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingHook.class)
public class FishingHookCollisionMixin {

    @Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
    private void skipOwnerCollision(Entity target, CallbackInfoReturnable<Boolean> cir) {
        FishingHook self = (FishingHook) (Object) this;
        if (target == self.getOwner()) {
            Player owner = self.getPlayerOwner();
            if (owner != null) {
                ModPlayer modPlayer = (ModPlayer) owner;
                if (ModHelper.getTotalThrown(modPlayer) > 1) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}