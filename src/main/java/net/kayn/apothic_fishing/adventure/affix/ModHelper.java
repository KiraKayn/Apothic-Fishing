package net.kayn.apothic_fishing.adventure.affix;

import net.kayn.apothic_fishing.api.ModPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ModHelper {

    public static boolean isLookingAtHook(ModPlayer player, FishingHook hook, int range) {
        double degrees = Math.toDegrees(getLookAngle(player, hook));
        return degrees < range;
    }

    public static double getLookAngle(ModPlayer player, FishingHook hook) {
        Player p = player.apothic$getPlayer();
        Vec3 looking = p.getLookAngle();
        looking = new Vec3(looking.x, 0, looking.z).normalize();
        Vec3 hookToPlayer = hook.position().subtract(p.position());
        hookToPlayer = new Vec3(hookToPlayer.x, 0, hookToPlayer.z).normalize();
        double dot = looking.dot(hookToPlayer);
        dot = Math.max(-1.0, Math.min(1.0, dot));
        return Math.acos(dot);
    }

    public static FishingHook getLookingHook(ModPlayer player, ItemStack rod) {
        List<FishingHook> allHooks = player.apothic$getHooks();
        if (allHooks.isEmpty()) return null;
        if (canCastHook(player, rod)) {
            return getClosestLookingHook(player, allHooks, true);
        } else {
            return getClosestLookingHook(player, allHooks, false);
        }
    }

    private static FishingHook getClosestLookingHook(ModPlayer player, List<FishingHook> hooks, boolean withinAngle) {
        if (hooks.isEmpty()) return null;
        FishingHook closest = null;
        double closestAngle = 1000.0;
        for (FishingHook hook : hooks) {
            if (hook.isRemoved()) continue;
            double deg = Math.toDegrees(getLookAngle(player, hook));
            if (deg > closestAngle) continue;
            if (withinAngle && deg > player.apothic$getAngle()) continue;
            closestAngle = deg;
            closest = hook;
        }
        return closest;
    }

    public static boolean canCastHook(ModPlayer player, ItemStack rod) {
        int thrown = getTotalThrown(player);
        if (thrown == 0) return true;
        int multiLevel = MultiLineAffix.getTotalExtraLines(rod);
        if (multiLevel == 0) return false;
        return multiLevel >= thrown;
    }

    public static int getTotalThrown(ModPlayer player) {
        return player.apothic$getHooks().size();
    }

    public static ItemStack getHeldRod(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof FishingRodItem) return main;
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof FishingRodItem) return off;
        return null;
    }

    public static void addHook(ModPlayer player, FishingHook hook) {
        player.apothic$getHooks().add(hook);
    }

    public static void removeHook(ModPlayer player, FishingHook hook) {
        player.apothic$getHooks().remove(hook);
    }
}