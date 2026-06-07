package net.kayn.apothic_fishing.mixin;

import net.kayn.apothic_fishing.adventure.affix.ModHelper;
import net.kayn.apothic_fishing.api.ModPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin extends Projectile {

    protected FishingHookMixin(EntityType<? extends Projectile> p_37248_, Level p_37249_) {
        super(p_37248_, p_37249_);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    protected boolean canHitEntity(Entity targetEntity) {
        Entity owner = this.getOwner();
        if (targetEntity == owner) {
            if (owner instanceof Player player) {
                ModPlayer modPlayer = (ModPlayer) player;
                if (ModHelper.getTotalThrown(modPlayer) > 1) return false;
            }
        }

        if (!super.canHitEntity(targetEntity) && !(targetEntity.isAlive() && targetEntity instanceof ItemEntity)) {
            return false;
        }

        if (owner instanceof Player player) {
            ModPlayer modPlayer = (ModPlayer) player;
            List<FishingHook> hooks = modPlayer.apothic$getHooks();
            for (FishingHook hook : hooks) {
                if (this == (Object) hook) continue;
                if (hook.getHookedIn() == targetEntity) return false;
                if (targetEntity instanceof ItemEntity itemEntity) {
                    Vec3 itemVelocity = itemEntity.getDeltaMovement();
                    if (itemVelocity.length() > 0.1) {
                        Vec3 toPlayer = player.position().subtract(itemEntity.position()).normalize();
                        Vec3 velocityDir = itemVelocity.normalize();
                        if (toPlayer.dot(velocityDir) > 0.5) return false;
                    }
                }
            }
        }

        return true;
    }
}