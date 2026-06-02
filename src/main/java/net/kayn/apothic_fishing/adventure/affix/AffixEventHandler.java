package net.kayn.apothic_fishing.adventure.affix;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import net.kayn.apothic_fishing.api.ModPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AffixEventHandler {

    private static final Map<UUID, Deque<float[]>> pendingRecastAngles = new ConcurrentHashMap<>();
    private static final Set<UUID> recastingPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<FishingHook> retrievingHooks = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final Field NIBBLE;

    static {
        Field f = null;
        try { f = FishingHook.class.getDeclaredField("nibble"); f.setAccessible(true); }
        catch (NoSuchFieldException e) {
            try { f = ObfuscationReflectionHelper.findField(FishingHook.class, "f_37113_"); f.setAccessible(true); }
            catch (Exception ignored) {}
        }
        NIBBLE = f;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        UUID uuid = player.getUUID();
        ItemStack rod = ModHelper.getHeldRod(player);

        Deque<float[]> angleQueue = pendingRecastAngles.get(uuid);
        if (angleQueue != null && !angleQueue.isEmpty()) {
            if (rod != null) {
                ModPlayer modPlayer = (ModPlayer) player;

                float savedYaw   = player.getYRot();
                float savedPitch = player.getXRot();

                InteractionHand hand = player.getMainHandItem().getItem() instanceof FishingRodItem
                        ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;

                while (!angleQueue.isEmpty() && ModHelper.canCastHook(modPlayer, rod)) {
                    float[] angles = angleQueue.poll();
                    player.setYRot(angles[0]);
                    player.setXRot(angles[1]);

                    recastingPlayers.add(uuid);
                    try {
                        player.gameMode.useItem(player, player.level(), player.getItemInHand(hand), hand);
                    } finally {
                        recastingPlayers.remove(uuid);
                    }
                }

                player.setYRot(savedYaw);
                player.setXRot(savedPitch);
            }
            angleQueue.clear();
            pendingRecastAngles.remove(uuid);
        }

        if (rod == null) return;
        if (!hasAffix(rod, AutoFishAffix.class)) return;

        ModPlayer modPlayer = (ModPlayer) player;
        List<FishingHook> hooks = new ArrayList<>(modPlayer.apothic$getHooks());

        for (FishingHook hook : hooks) {
            if (hook.isRemoved()) continue;
            if (retrievingHooks.contains(hook)) continue;
            if (getNibble(hook) <= 0) continue;

            float[] recastAngles = computeRecastAngles(player, hook);

            retrievingHooks.add(hook);
            try {
                hook.retrieve(rod);
            } finally {
                retrievingHooks.remove(hook);
            }

            pendingRecastAngles.computeIfAbsent(uuid, k -> new ArrayDeque<>()).add(recastAngles);
        }
    }

    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack rod = ModHelper.getHeldRod(player);
        if (rod == null) return;
        if (hasAffix(rod, AutoFishAffix.class)) {
            event.damageRodBy(0);
        }
    }

    public static boolean isRecasting(UUID uuid) {
        return recastingPlayers.contains(uuid);
    }

    private static float[] computeRecastAngles(Player player, FishingHook hook) {
        double dx = hook.getX() - player.getX();
        double dy = hook.getY() - player.getEyeY();
        double dz = hook.getZ() - player.getZ();
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        float yaw   = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horizDist)));
        return new float[]{ yaw, pitch };
    }

    private static int getNibble(FishingHook hook) {
        if (NIBBLE == null) return 0;
        try { return (int) NIBBLE.get(hook); } catch (Exception e) { return 0; }
    }

    private static <T> boolean hasAffix(ItemStack rod, Class<T> affixClass) {
        for (var inst : AffixHelper.getAffixes(rod).values()) {
            if (!inst.isValid()) continue;
            if (affixClass.isInstance(inst.affix().get())) return true;
        }
        return false;
    }
}