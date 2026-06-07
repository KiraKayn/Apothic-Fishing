package net.kayn.apothic_fishing.mixin;

import net.kayn.apothic_fishing.adventure.affix.ModHelper;
import net.kayn.apothic_fishing.api.ModPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHook.class)
public class FishingHookOwnerMixin {

    @Inject(method = "setOwner", at = @At("TAIL"))
    private void onSetOwner(Entity owner, CallbackInfo ci) {
        FishingHook self = (FishingHook) (Object) this;
        Player player = self.getPlayerOwner();
        if (player == null) return;

        ModPlayer modPlayer = (ModPlayer) player;
        int thrown = ModHelper.getTotalThrown(modPlayer);
        if (thrown == 0) {
            player.fishing = self;
        }
        ModHelper.addHook(modPlayer, self);
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        FishingHook self = (FishingHook) (Object) this;
        Player player = self.getPlayerOwner();
        if (player == null) return;

        ModPlayer modPlayer = (ModPlayer) player;
        ModHelper.removeHook(modPlayer, self);

        int remaining = ModHelper.getTotalThrown(modPlayer);
        if (remaining == 0) {
            player.fishing = null;
        } else if (player.fishing == self) {
            player.fishing = modPlayer.apothic$getHooks().get(0);
        }
    }
}