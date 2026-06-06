package net.kayn.apothic_fishing.event;

import dev.shadowsoffire.apotheosis.adventure.boss.ApothBoss;
import dev.shadowsoffire.apotheosis.adventure.boss.BossRegistry;
import dev.shadowsoffire.placebo.reload.WeightedDynamicRegistry.IDimensional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import org.jetbrains.annotations.Nullable;

public class BossFishingHandler {

    @Nullable
    public static Mob tryFishBoss(FishingHook hook, Player player, ServerLevel level, float luck) {
        ApothBoss boss = BossRegistry.INSTANCE.getRandomItem(
                level.getRandom(),
                luck,
                IDimensional.matches(level)
        );
        if (boss == null) return null;

        BlockPos spawnPos = hook.blockPosition();


        Mob mob;
        try {
            mob = boss.createBoss(level, spawnPos, level.getRandom(), luck, null);
        } catch (Throwable t) {
            return null;
        }

        if (mob.getPersistentData().getBoolean("apoth_hostility.discard")) {
            mob.discard();
            return null;
        }

        level.addFreshEntityWithPassengers(mob);
        return mob;
    }

    public static boolean isHostilityLoaded() {
        try {
            Class.forName("net.kayn.apotheotic_hostility.ApotheoticHostility");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}