package net.kayn.apothic_fishing.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

@Mixin(FishingHook.class)
public abstract class FishingHookRetrieveMixin {

    @Inject(
            method = "retrieve",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/FishingHook;discard()V"),
            cancellable = true
    )
    private void onRetrieveBeforeDiscard(ItemStack rod, CallbackInfoReturnable<Integer> cir) {
        FishingHook self = (FishingHook) (Object) this;
        if (self.level().isClientSide()) return;
        if (!self.getPersistentData().contains("apothic_fishing.hooked_boss_id")) return;

        int bossId = self.getPersistentData().getInt("apothic_fishing.hooked_boss_id");
        self.getPersistentData().remove("apothic_fishing.hooked_boss_id");

        Entity boss = self.level().getEntity(bossId);
        if (boss == null || boss.isRemoved()) {
            return;
        }

        Player player = self.getPlayerOwner();
        if (player != null) {
            double dx = player.getX() - boss.getX();
            double dy = player.getY() - boss.getY();
            double dz = player.getZ() - boss.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > 0) {
                double speed = Math.min(1.5, 0.18 * dist);
                boss.setDeltaMovement(dx / dist * speed, dy / dist * speed + 0.25, dz / dist * speed);
                boss.hasImpulse = true;
            }
        }

        self.discard();
        cir.setReturnValue(1);
        cir.cancel();
    }
}