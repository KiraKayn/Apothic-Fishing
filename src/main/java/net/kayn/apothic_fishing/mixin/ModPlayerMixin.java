package net.kayn.apothic_fishing.mixin;

import net.kayn.apothic_fishing.api.ModPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

@Mixin(Player.class)
public abstract class ModPlayerMixin extends LivingEntity implements ModPlayer {

    @Unique
    private final List<FishingHook> apothic_hooks = new ArrayList<>();

    @Unique
    private int apothic_angle = 15;

    protected ModPlayerMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public List<FishingHook> apothic$getHooks() {
        return this.apothic_hooks;
    }

    @Override
    public int apothic$getAngle() {
        return this.apothic_angle;
    }

    @Override
    public void apothic$setAngle(int v) {
        this.apothic_angle = v;
    }

    @Override
    public Player apothic$getPlayer() {
        return (Player) (Object) this;
    }
}