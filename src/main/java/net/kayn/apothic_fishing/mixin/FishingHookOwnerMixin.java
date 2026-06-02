package net.kayn.apothic_fishing.mixin;

import net.kayn.apothic_fishing.adventure.affix.ModHelper;
import net.kayn.apothic_fishing.api.ModPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import javax.annotation.Nullable;

@Mixin(FishingHook.class)
public class FishingHookOwnerMixin {

    /**
     * @author
     * @reason
     */

    @Overwrite
    private void updateOwnerInfo(@Nullable FishingHook hook) {
        FishingHook self = (FishingHook) (Object) this;
        Player player = self.getPlayerOwner();
        if (player == null) return;

        ModPlayer modPlayer = (ModPlayer) player;

        if (hook == null) {
            ModHelper.removeHook(modPlayer, self);
            int remaining = ModHelper.getTotalThrown(modPlayer);
            if (remaining == 0) {
                player.fishing = null;
            } else if (player.fishing == self) {
                player.fishing = modPlayer.apothic$getHooks().get(0);
            }
        } else {
            int thrown = ModHelper.getTotalThrown(modPlayer);
            if (thrown == 0) {
                player.fishing = hook;
            }
            ModHelper.addHook(modPlayer, hook);
        }
    }
}