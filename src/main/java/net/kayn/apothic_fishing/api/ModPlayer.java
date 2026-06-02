package net.kayn.apothic_fishing.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;

import java.util.List;

public interface ModPlayer {
    List<FishingHook> apothic$getHooks();
    int apothic$getAngle();
    void apothic$setAngle(int v);
    Player apothic$getPlayer();
}