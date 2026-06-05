package net.kayn.apothic_fishing.mixin;

import net.kayn.apothic_fishing.adventure.affix.LavaFishAffix;
import net.kayn.apothic_fishing.adventure.affix.ModHelper;
import net.kayn.apothic_fishing.adventure.affix.SpecialFishingHandler;
import net.kayn.apothic_fishing.adventure.affix.VoidFishAffix;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHook.class)
public abstract class FishingHookSpecialMixin {

    private static final EntityDataAccessor<Boolean> IS_SPECIAL_HOOK =
            SynchedEntityData.defineId(FishingHook.class, EntityDataSerializers.BOOLEAN);

    @Inject(method = "defineSynchedData", at = @At("HEAD"))
    private void onDefineSynchedData(CallbackInfo ci) {
        ((FishingHook) (Object) this).getEntityData().define(IS_SPECIAL_HOOK, false);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;II)V", at = @At("RETURN"))
    private void onConstructed(Player player, Level level, int luck, int lureSpeed, CallbackInfo ci) {
        if (level.isClientSide) return;
        ItemStack rod = ModHelper.getHeldRod(player);
        if (rod != null && (SpecialFishingHandler.hasAffix(rod, LavaFishAffix.class) || SpecialFishingHandler.hasAffix(rod, VoidFishAffix.class))) {
            ((FishingHook) (Object) this).getEntityData().set(IS_SPECIAL_HOOK, true);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTickHead(CallbackInfo ci) {
        FishingHook self = (FishingHook) (Object) this;
        if (self.level().isClientSide()) {
            if (self.getEntityData().get(IS_SPECIAL_HOOK)) {
                if (SpecialFishingHandler.hasClientLock(self.getId())) {
                    SpecialFishingHandler.correctClientPosition(self);
                    self.clearFire();
                    ci.cancel();
                }
            }
            return;
        }
        if (SpecialFishingHandler.handleSpecialTick(self)) {
            ci.cancel();
        }
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        FishingHook self = (FishingHook) (Object) this;
        if (self.level().isClientSide()) {
            SpecialFishingHandler.clearClientLock(self.getId());
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickTail(CallbackInfo ci) {
        FishingHook self = (FishingHook) (Object) this;
        if (!self.level().isClientSide()) return;
        if (!self.getEntityData().get(IS_SPECIAL_HOOK)) return;

        SpecialFishingHandler.tryAcquireClientLock(self);
        self.clearFire();
    }
}