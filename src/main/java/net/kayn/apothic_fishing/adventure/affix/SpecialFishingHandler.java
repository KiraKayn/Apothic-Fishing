package net.kayn.apothic_fishing.adventure.affix;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import net.kayn.apothic_fishing.ApothicFishing;
import net.kayn.apothic_fishing.mixin.FishingHookAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.ItemFishedEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpecialFishingHandler {

    public static final ResourceLocation LAVA_LOOT_TABLE = new ResourceLocation(ApothicFishing.MOD_ID, "gameplay/lava_fishing");
    public static final ResourceLocation VOID_LOOT_TABLE = new ResourceLocation(ApothicFishing.MOD_ID, "gameplay/void_fishing");

    private static final Set<FishingHook> tickingHooks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<UUID, Double> lavaHookSurfaceY = new ConcurrentHashMap<>();
    private static final Map<UUID, double[]> lockedPositions = new ConcurrentHashMap<>();
    private static final Map<Integer, double[]> clientLockedPositions = new ConcurrentHashMap<>();
    private static final Map<UUID, Object> hookStates = new ConcurrentHashMap<>();

    private static final Object STATE_FLYING;
    private static final Object STATE_BOBBING;
    private static final Object STATE_HOOKED;

    static {
        Object flying = null, bobbing = null, hooked = null;
        for (Class<?> inner : FishingHook.class.getDeclaredClasses()) {
            if (!inner.isEnum()) continue;
            try {
                flying = Enum.valueOf((Class<? extends Enum>) inner, "FLYING");
                bobbing = Enum.valueOf((Class<? extends Enum>) inner, "BOBBING");
                hooked = Enum.valueOf((Class<? extends Enum>) inner, "HOOKED_IN_ENTITY");
                break;
            } catch (IllegalArgumentException ignored) {
            }
        }
        STATE_FLYING = flying;
        STATE_BOBBING = bobbing;
        STATE_HOOKED = hooked;
    }

    public static boolean handleSpecialTick(FishingHook hook) {
        if (tickingHooks.contains(hook)) return false;
        if (!(hook instanceof FishingHookAccessor a)) return false;

        Player owner = hook.getPlayerOwner();
        if (owner == null) return false;

        ItemStack rod = ModHelper.getHeldRod(owner);
        if (rod == null) return false;

        boolean hasLava = hasAffix(rod, LavaFishAffix.class);
        boolean hasVoid = hasAffix(rod, VoidFishAffix.class);
        if (!hasLava && !hasVoid) return false;

        boolean inLava = isInLava(hook);
        boolean inVoid = isInVoid(hook);

        if (hook.level().getFluidState(hook.blockPosition()).is(FluidTags.WATER)) return false;

        Object state = hookStates.getOrDefault(hook.getUUID(), STATE_FLYING);
        boolean isFlying = STATE_FLYING.equals(state);
        boolean isHooked = STATE_HOOKED.equals(state);

        if (isHooked) {
            Entity hookedIn = a.getHookedIn();
            if (hookedIn != null && hookedIn.getPersistentData().getBoolean("apoth.boss")) {
                tickingHooks.add(hook);
                try {
                    hookedEntityTick(hook, owner);
                } finally {
                    tickingHooks.remove(hook);
                }
                return true;
            }
            return false;
        }

        if (!inLava && !inVoid && !isFlying) return false;

        tickingHooks.add(hook);
        try {
            if (inLava && hasLava) {
                lavaFishingTick(hook, owner);
            } else if (inVoid && hasVoid) {
                voidFishingTick(hook, owner);
            } else if (isFlying) {
                flyingTick(hook, owner);
            }
        } finally {
            tickingHooks.remove(hook);
        }
        return true;
    }

    private static void flyingTick(FishingHook hook, Player player) {
        if (shouldStopFishing(hook, player)) {
            discardHook(hook);
            return;
        }

        Object state = hookStates.getOrDefault(hook.getUUID(), STATE_FLYING);
        if (!STATE_FLYING.equals(state)) return;

        hook.setDeltaMovement(hook.getDeltaMovement().add(0, -0.03, 0));
        hook.move(net.minecraft.world.entity.MoverType.SELF, hook.getDeltaMovement());
        hook.setDeltaMovement(hook.getDeltaMovement().scale(0.92));
        checkCollision(hook);
        reapplyPosition(hook);
    }

    private static void lavaFishingTick(FishingHook hook, Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        FishingHookAccessor a = (FishingHookAccessor) hook;
        UUID id = hook.getUUID();

        if (shouldStopFishing(hook, player)) {
            if (player.level() instanceof ServerLevel sl && trySpawnAndHookBoss(hook, sl)) {
                Entity hookedIn = a.getHookedIn();
                if (hookedIn != null) {
                    pullTowards(player, hookedIn);
                }
            }
            discardHook(hook);
            return;
        }

        if (hook.onGround()) {
            int life = a.getLife() + 1;
            a.setLife(life);
            if (life >= 1200) {
                discardHook(hook);
                return;
            }
        } else {
            a.setLife(0);
        }

        Object state = hookStates.getOrDefault(id, STATE_FLYING);
        double surfaceY = getOrComputeLavaSurfaceY(hook);
        boolean inLava = isInLavaLoose(hook);

        if (STATE_FLYING.equals(state)) {
            if (inLava) {
                hook.setDeltaMovement(hook.getDeltaMovement().multiply(0.3D, 0.2D, 0.3D));
                hookStates.put(id, STATE_BOBBING);
                hook.setPos(hook.getX(), surfaceY, hook.getZ());
                hook.setDeltaMovement(Vec3.ZERO);
                lockedPositions.put(id, new double[]{hook.getX(), surfaceY, hook.getZ()});
            } else {
                hook.setDeltaMovement(hook.getDeltaMovement().add(0, -0.03, 0));
            }
            hook.move(net.minecraft.world.entity.MoverType.SELF, hook.getDeltaMovement());
            hook.setDeltaMovement(hook.getDeltaMovement().scale(0.92));
            if (STATE_FLYING.equals(state) && (hook.onGround() || hook.horizontalCollision)) {
                hook.setDeltaMovement(Vec3.ZERO);
            }
            reapplyPosition(hook);
            return;
        }

        if (STATE_HOOKED.equals(state)) {
            hookedEntityTick(hook, player);
            return;
        }

        if (STATE_BOBBING.equals(state)) {
            hook.setPos(hook.getX(), surfaceY, hook.getZ());
            hook.setDeltaMovement(Vec3.ZERO);
            if (inLava) {
                a.setOutOfWaterTime(Math.max(0, a.getOutOfWaterTime() - 1));
                lavaCatchingFish(hook, serverLevel);
            } else {
                a.setOutOfWaterTime(Math.min(10, a.getOutOfWaterTime() + 1));
            }
        }
    }

    private static void voidFishingTick(FishingHook hook, Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        FishingHookAccessor a = (FishingHookAccessor) hook;
        UUID id = hook.getUUID();

        if (shouldStopFishing(hook, player)) {
            if (player.level() instanceof ServerLevel sl && trySpawnAndHookBoss(hook, sl)) {
                Entity hookedIn = a.getHookedIn();
                if (hookedIn != null) {
                    pullTowards(player, hookedIn);
                }
            }
            discardHook(hook);
            return;
        }

        Object state = hookStates.getOrDefault(id, STATE_FLYING);

        if (STATE_FLYING.equals(state)) {
            hookStates.put(id, STATE_BOBBING);
            hook.setDeltaMovement(Vec3.ZERO);
            hook.setPos(hook.getX(), hook.getY(), hook.getZ());
            lockedPositions.put(id, new double[]{hook.getX(), hook.getY(), hook.getZ()});
            return;
        }

        if (STATE_HOOKED.equals(state)) {
            hookedEntityTick(hook, player);
            return;
        }

        if (STATE_BOBBING.equals(state)) {
            double[] pos = lockedPositions.get(id);
            if (pos != null) {
                hook.setPos(pos[0], pos[1], pos[2]);
            }
            hook.setDeltaMovement(Vec3.ZERO);
            voidCatchingFish(hook, serverLevel);
        }
    }

    private static void lavaCatchingFish(FishingHook hook, ServerLevel level) {
        FishingHookAccessor a = (FishingHookAccessor) hook;
        int nibble = a.getNibble();
        int untilHooked = a.getTimeUntilHooked();
        int untilLured = a.getTimeUntilLured();

        if (nibble > 0) {
            a.setNibble(nibble - 1);
            if (nibble - 1 <= 0) {
                a.setTimeUntilLured(0);
                a.setTimeUntilHooked(0);
                setBiting(hook, false);
            }
            return;
        }

        if (untilHooked > 0) {
            a.setTimeUntilHooked(untilHooked - 1);
            if (untilHooked - 1 > 0) {
                float angle = a.getFishAngle() + (float) level.getRandom().nextGaussian() * 4f;
                a.setFishAngle(angle);
                float rad = angle * (float) (Math.PI / 180f);
                float sin = Mth.sin(rad), cos = Mth.cos(rad);
                double px = hook.getX() + sin * (untilHooked - 1) * 0.1;
                double py = Mth.floor(hook.getY()) + 1.0;
                double pz = hook.getZ() + cos * (untilHooked - 1) * 0.1;
                level.sendParticles(ParticleTypes.LAVA, px, py, pz, 1, sin * 0.04, 0.1, cos * 0.04, 0);
                level.sendParticles(ParticleTypes.SMOKE, px, py, pz, 2, sin * 0.04, 0.1, -cos * 0.04, 0);
                level.sendParticles(ParticleTypes.ASH, px, py, pz, 2, -sin * 0.04, 0.1, cos * 0.04, 1.0);
            } else {
                if (level.getRandom().nextFloat() < net.kayn.apothic_fishing.loot.BossFishingLootModifier.getBossChance()) {
                    hook.getPersistentData().putBoolean("apothic_fishing.boss_bite", true);
                }
                hook.playSound(SoundEvents.LAVA_POP, 0.25f, 1.0f + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.4f);
                double d = hook.getY() + 0.5;
                level.sendParticles(ParticleTypes.LAVA, hook.getX(), d, hook.getZ(), (int) (1f + hook.getBbWidth() * 20f), hook.getBbWidth(), 0, hook.getBbWidth(), 0.2f);
                level.sendParticles(ParticleTypes.SMOKE, hook.getX(), d, hook.getZ(), (int) (1f + hook.getBbWidth() * 20f), hook.getBbWidth(), 0, hook.getBbWidth(), 0.2f);
                a.setNibble(level.getRandom().nextInt(20, 40));
                setBiting(hook, true);
            }
            return;
        }

        if (untilLured > 0) {
            a.setTimeUntilLured(untilLured - 1);
            float chance = 0.15f;
            if (untilLured < 20) chance += (20 - untilLured) * 0.05f;
            else if (untilLured < 40) chance += (40 - untilLured) * 0.02f;
            else if (untilLured < 60) chance += (60 - untilLured) * 0.01f;
            if (level.getRandom().nextFloat() < chance) {
                float r = Mth.nextFloat(level.getRandom(), 0f, 360f) * (float) (Math.PI / 180f);
                float dist = Mth.nextFloat(level.getRandom(), 25f, 60f);
                level.sendParticles(ParticleTypes.LAVA, hook.getX() + Mth.sin(r) * dist * 0.1, Mth.floor(hook.getY()) + 1.0, hook.getZ() + Mth.cos(r) * dist * 0.1, 2, 0.1, 0, 0.1, 0);
            }
            if (untilLured - 1 <= 0) {
                a.setFishAngle(level.getRandom().nextFloat() * 360f);
                a.setTimeUntilHooked(level.getRandom().nextInt(20, 80));
            }
            return;
        }

        int lureSpeed = a.getLureSpeed();
        a.setTimeUntilLured(Math.max(1, level.getRandom().nextInt(100, 600) - lureSpeed * 20 * 5));
    }

    private static void voidCatchingFish(FishingHook hook, ServerLevel level) {
        FishingHookAccessor a = (FishingHookAccessor) hook;
        int nibble = a.getNibble();
        int untilHooked = a.getTimeUntilHooked();
        int untilLured = a.getTimeUntilLured();

        if (nibble > 0) {
            a.setNibble(nibble - 1);
            if (nibble - 1 <= 0) {
                a.setTimeUntilLured(0);
                a.setTimeUntilHooked(0);
                setBiting(hook, false);
            }
            return;
        }

        if (untilHooked > 0) {
            a.setTimeUntilHooked(untilHooked - 1);
            if (untilHooked - 1 > 0) {
                float angle = a.getFishAngle() + (float) level.getRandom().nextGaussian() * 4f;
                a.setFishAngle(angle);
                float rad = angle * (float) (Math.PI / 180f);
                float sin = Mth.sin(rad), cos = Mth.cos(rad);
                double px = hook.getX() + sin * (untilHooked - 1) * 0.1;
                double pz = hook.getZ() + cos * (untilHooked - 1) * 0.1;
                level.sendParticles(ParticleTypes.DRAGON_BREATH, px, hook.getY(), pz, 1, sin * 0.04, 0.1, cos * 0.04, 0);
                level.sendParticles(ParticleTypes.PORTAL, px, hook.getY(), pz, 2, sin * 0.04, 0.1, -cos * 0.04, 0.5);
            } else {
                if (level.getRandom().nextFloat() < net.kayn.apothic_fishing.loot.BossFishingLootModifier.getBossChance()) {
                    hook.getPersistentData().putBoolean("apothic_fishing.boss_bite", true);
                }
                hook.playSound(SoundEvents.ENDERMAN_AMBIENT, 0.25f, 1.0f + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.4f);
                level.sendParticles(ParticleTypes.DRAGON_BREATH, hook.getX(), hook.getY(), hook.getZ(), (int) (1f + hook.getBbWidth() * 20f), hook.getBbWidth(), 0, hook.getBbWidth(), 0.2f);
                level.sendParticles(ParticleTypes.PORTAL, hook.getX(), hook.getY(), hook.getZ(), (int) (1f + hook.getBbWidth() * 20f), hook.getBbWidth(), 0, hook.getBbWidth(), 0.2f);
                a.setNibble(level.getRandom().nextInt(20, 40));
                setBiting(hook, true);
            }
            return;
        }

        if (untilLured > 0) {
            a.setTimeUntilLured(untilLured - 1);
            float chance = 0.15f;
            if (untilLured < 20) chance += (20 - untilLured) * 0.05f;
            else if (untilLured < 40) chance += (40 - untilLured) * 0.02f;
            else if (untilLured < 60) chance += (60 - untilLured) * 0.01f;
            if (level.getRandom().nextFloat() < chance) {
                float r = Mth.nextFloat(level.getRandom(), 0f, 360f) * (float) (Math.PI / 180f);
                float dist = Mth.nextFloat(level.getRandom(), 25f, 60f);
                level.sendParticles(ParticleTypes.PORTAL, hook.getX() + Mth.sin(r) * dist * 0.1, hook.getY(), hook.getZ() + Mth.cos(r) * dist * 0.1, 4, 0.1, 0, 0.1, 0.5);
            }
            if (untilLured - 1 <= 0) {
                a.setFishAngle(level.getRandom().nextFloat() * 360f);
                a.setTimeUntilHooked(level.getRandom().nextInt(20, 80));
            }
            return;
        }

        int lureSpeed = a.getLureSpeed();
        a.setTimeUntilLured(Math.max(1, level.getRandom().nextInt(100, 600) - lureSpeed * 20 * 5));
    }

    public static void tryAcquireClientLock(FishingHook hook) {
        if (clientLockedPositions.containsKey(hook.getId())) return;

        if (isInVoid(hook)) {
            clientLockedPositions.put(hook.getId(), new double[]{hook.getX(), hook.getY(), hook.getZ()});
            return;
        }

        BlockPos bp = hook.blockPosition();
        boolean strictlyInLava = hook.level().getFluidState(bp).is(FluidTags.LAVA) || hook.isInLava();
        if (!strictlyInLava) return;

        double surfaceY = computeLavaSurfaceY(hook);
        clientLockedPositions.put(hook.getId(), new double[]{hook.getX(), surfaceY, hook.getZ()});
        hook.setPos(hook.getX(), surfaceY, hook.getZ());
        hook.setDeltaMovement(Vec3.ZERO);
    }

    public static void correctClientPosition(FishingHook hook) {
        double[] locked = clientLockedPositions.get(hook.getId());
        if (locked == null) return;
        hook.setPos(locked[0], locked[1], locked[2]);
        hook.setDeltaMovement(Vec3.ZERO);
    }

    public static void clearClientLock(int entityId) {
        clientLockedPositions.remove(entityId);
    }

    public static boolean hasClientLock(int entityId) {
        return clientLockedPositions.containsKey(entityId);
    }

    public static void cleanupState(FishingHook hook) {
        hookStates.remove(hook.getUUID());
    }

    public static int retrieveSpecial(FishingHook hook, ItemStack rod) {
        Player player = hook.getPlayerOwner();
        if (player == null || player.level().isClientSide) return 0;
        if (!(hook instanceof FishingHookAccessor a)) return 0;

        int damage = 0;

        if (a.getHookedIn() != null) {
            Entity entity = a.getHookedIn();
            pullTowards(player, entity);
            CriteriaTriggers.FISHING_ROD_HOOKED.trigger((ServerPlayer) player, rod, hook, Collections.emptyList());
            hook.level().broadcastEntityEvent(hook, (byte) 31);
            damage = entity instanceof ItemEntity ? 3 : 5;
            discardHook(hook);
            return damage;
        }

        if (a.getNibble() > 0) {
            if (hook.getPersistentData().getBoolean("apothic_fishing.boss_bite")) {
                if (player.level() instanceof ServerLevel serverLevel) {
                    if (trySpawnAndHookBoss(hook, serverLevel)) {
                        Entity hookedIn = a.getHookedIn();
                        if (hookedIn != null) {
                            pullTowards(player, hookedIn);
                            CriteriaTriggers.FISHING_ROD_HOOKED.trigger((ServerPlayer) player, rod, hook, Collections.emptyList());
                            hook.level().broadcastEntityEvent(hook, (byte) 31);
                            damage = 5;
                            discardHook(hook);
                            return damage;
                        }
                    }
                }
                discardHook(hook);
                return 0;
            }

            if (player.level() instanceof ServerLevel serverLevel) {
                List<ItemStack> loot = generateLoot(hook, rod, serverLevel);
                ItemFishedEvent event = new ItemFishedEvent(loot, hook.onGround() ? 2 : 1, hook);
                if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event)) {
                    discardHook(hook);
                    return event.getRodDamage();
                }
                CriteriaTriggers.FISHING_ROD_HOOKED.trigger((ServerPlayer) player, rod, hook, loot);
                deliverLoot(loot, hook, player, serverLevel);
                damage = 1;
            }
        }

        if (hook.onGround()) {
            damage = 2;
        }

        discardHook(hook);
        return damage;
    }

    private static void deliverLoot(List<ItemStack> loot, FishingHook hook, Player player, ServerLevel level) {
        for (ItemStack stack : loot) {
            double dx = player.getX() - hook.getX();
            double dy = player.getY() - hook.getY();
            double dz = player.getZ() - hook.getZ();
            ItemEntity item = new ItemEntity(level, hook.getX(), hook.getY(), hook.getZ(), stack);
            item.setDeltaMovement(dx * 0.1, dy * 0.1 + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08 + 0.2, dz * 0.1);
            level.addFreshEntity(item);
            if (stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(Items.TROPICAL_FISH) || stack.is(Items.PUFFERFISH)) {
                player.awardStat(Stats.FISH_CAUGHT, 1);
            }
        }
        if (!loot.isEmpty()) {
            level.addFreshEntity(new ExperienceOrb(level, player.getX(), player.getY() + 0.5, player.getZ() + 0.5, hook.level().getRandom().nextInt(6) + 1));
        }
    }

    private static void hookedEntityTick(FishingHook hook, Player player) {
        FishingHookAccessor a = (FishingHookAccessor) hook;
        if (shouldStopFishing(hook, player)) {
            Entity hookedIn = a.getHookedIn();
            if (hookedIn != null) {
                pullTowards(player, hookedIn);
            }
            discardHook(hook);
            return;
        }

        Entity hookedIn = a.getHookedIn();
        if (hookedIn != null && !hookedIn.isRemoved()) {
            double dx = hook.getX() - hookedIn.getX();
            double dy = hook.getY() - hookedIn.getY();
            double dz = hook.getZ() - hookedIn.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > 0.5) {
                double speed = 0.12;
                hookedIn.setDeltaMovement(hookedIn.getDeltaMovement().add(dx / dist * speed, dy / dist * speed, dz / dist * speed));
                hookedIn.hasImpulse = true;
            }
        }
    }

    public static boolean trySpawnAndHookBoss(FishingHook hook, ServerLevel level) {
        Player player = hook.getPlayerOwner();
        if (player == null) return false;

        if (!hook.getPersistentData().getBoolean("apothic_fishing.boss_bite")) return false;
        hook.getPersistentData().remove("apothic_fishing.boss_bite");

        dev.shadowsoffire.apotheosis.adventure.boss.ApothBoss bossDef =
                dev.shadowsoffire.apotheosis.adventure.boss.BossRegistry.INSTANCE.getRandomItem(
                        level.getRandom(), player.getLuck(),
                        dev.shadowsoffire.placebo.reload.WeightedDynamicRegistry.IDimensional.matches(level)
                );
        if (bossDef == null) return false;

        BlockPos hookPos = hook.blockPosition();
        BlockPos spawnPos = findSpawnAboveLava(hook, hookPos, level);

        net.minecraft.world.entity.Mob mob;
        try {
            mob = bossDef.createBoss(level, spawnPos, level.getRandom(), player.getLuck(), null);
        } catch (Throwable t) {
            return false;
        }

        if (mob.getPersistentData().getBoolean("apoth_hostility.discard")) {
            mob.discard();
            return false;
        }

        level.addFreshEntityWithPassengers(mob);

        if (hook instanceof FishingHookAccessor a) {
            a.invokeSetHookedEntity(mob);
        } else {
            return false;
        }

        hookStates.put(hook.getUUID(), STATE_HOOKED);
        setBiting(hook, false);
        return true;
    }

    private static BlockPos findSpawnAboveLava(FishingHook hook, BlockPos hookPos, ServerLevel level) {
        BlockPos check = hookPos;
        for (int i = 0; i <= 10; i++) {
            BlockPos above = hookPos.above(i);
            if (!level.getFluidState(above).is(FluidTags.LAVA)
                    && !level.getFluidState(above.above()).is(FluidTags.LAVA)) {
                check = above;
                break;
            }
        }
        return check;
    }

    public static Object getHookedIn(FishingHook hook) {
        if (hook instanceof FishingHookAccessor a) {
            return a.getHookedIn();
        }
        return null;
    }

    public static void hookEntity(FishingHook hook, Entity entity) {
        if (hook instanceof FishingHookAccessor a) {
            a.invokeSetHookedEntity(entity);
        }
    }

    public static List<ItemStack> generateLoot(FishingHook hook, ItemStack rod, ServerLevel level) {
        Player player = hook.getPlayerOwner();
        if (player == null) return Collections.emptyList();

        ResourceLocation table = getLootTable(hook, rod);
        if (table == null) return Collections.emptyList();

        LootTable lootTable = level.getServer().getLootData().getLootTable(table);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, hook.position())
                .withParameter(LootContextParams.TOOL, rod)
                .withParameter(LootContextParams.THIS_ENTITY, hook)
                .withParameter(LootContextParams.KILLER_ENTITY, player)
                .withLuck((float) ((FishingHookAccessor) hook).getLuck() + player.getLuck())
                .create(LootContextParamSets.FISHING);

        it.unimi.dsi.fastutil.objects.ObjectArrayList<ItemStack> loot =
                new it.unimi.dsi.fastutil.objects.ObjectArrayList<>(lootTable.getRandomItems(params));

        if (!loot.isEmpty() && level.getRandom().nextFloat() <= 0.15f) {
            BlockPos hookPos = hook.blockPosition();
            net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biomeHolder = level.getBiome(hookPos);
            net.minecraft.resources.ResourceLocation biomeId = biomeHolder.unwrapKey()
                    .map(net.minecraft.resources.ResourceKey::location)
                    .orElse(null);
            net.minecraft.resources.ResourceLocation dimensionId = level.dimension().location();

            if (biomeId != null && !net.kayn.apothic_fishing.crate.CrateRegistry.INSTANCE.getValues().isEmpty()) {
                net.kayn.apothic_fishing.crate.CrateRegistry.getRandomCrateForLocation(
                        level.getRandom(), player.getLuck(), dimensionId, biomeId
                ).ifPresent(crate -> {
                    net.minecraft.resources.ResourceLocation crateId =
                            net.kayn.apothic_fishing.crate.CrateRegistry.INSTANCE.getKey(crate);
                    if (crateId != null) {
                        loot.clear();
                        loot.add(net.kayn.apothic_fishing.crate.CrateItem.createCrateStack(crateId));
                    }
                });
            }
        }

        return loot;
    }

    public static ResourceLocation getLootTable(FishingHook hook, ItemStack rod) {
        if (isInLava(hook) && hasAffix(rod, LavaFishAffix.class)) return LAVA_LOOT_TABLE;
        if (isInVoid(hook) && hasAffix(rod, VoidFishAffix.class)) return VOID_LOOT_TABLE;
        return null;
    }

    public static boolean isSpecialHook(FishingHook hook, ItemStack rod) {
        return getLootTable(hook, rod) != null;
    }

    public static boolean isInLava(FishingHook hook) {
        BlockPos bp = hook.blockPosition();
        return hook.level().getFluidState(bp).is(FluidTags.LAVA)
                || hook.level().getFluidState(bp.below()).is(FluidTags.LAVA)
                || hook.isInLava();
    }

    public static boolean isInVoid(FishingHook hook) {
        return hook.getY() < hook.level().getMinBuildHeight();
    }

    private static boolean shouldStopFishing(FishingHook hook, Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        boolean hasRod = main.canPerformAction(net.minecraftforge.common.ToolActions.FISHING_ROD_CAST)
                || off.canPerformAction(net.minecraftforge.common.ToolActions.FISHING_ROD_CAST);
        return player.isRemoved() || !player.isAlive() || !hasRod || hook.distanceToSqr(player) > 1024.0;
    }

    private static void discardHook(FishingHook hook) {
        lavaHookSurfaceY.remove(hook.getUUID());
        lockedPositions.remove(hook.getUUID());
        hookStates.remove(hook.getUUID());
        hook.discard();
    }

    private static void checkCollision(FishingHook hook) {
        try {
            java.lang.reflect.Method m = FishingHook.class.getDeclaredMethod("checkCollision");
            m.setAccessible(true);
            m.invoke(hook);
        } catch (Exception ignored) {}
    }

    private static void reapplyPosition(FishingHook hook) {
        try {
            java.lang.reflect.Method m = FishingHook.class.getDeclaredMethod("reapplyPosition");
            m.setAccessible(true);
            m.invoke(hook);
        } catch (Exception ignored) {}
    }

    private static void setBiting(FishingHook hook, boolean biting) {
        hook.getEntityData().set(FishingHookAccessor.getDATA_BITING(), biting);
    }

    private static double getOrComputeLavaSurfaceY(FishingHook hook) {
        Double cached = lavaHookSurfaceY.get(hook.getUUID());
        if (cached != null) return cached;
        double surfaceY = computeLavaSurfaceY(hook);
        lavaHookSurfaceY.put(hook.getUUID(), surfaceY);
        return surfaceY;
    }

    private static boolean isInLavaLoose(FishingHook hook) {
        if (lavaHookSurfaceY.containsKey(hook.getUUID())) return true;
        BlockPos bp = hook.blockPosition();
        return hook.level().getFluidState(bp).is(FluidTags.LAVA)
                || hook.level().getFluidState(bp.below()).is(FluidTags.LAVA)
                || hook.isInLava();
    }

    private static double computeLavaSurfaceY(FishingHook hook) {
        BlockPos base = BlockPos.containing(hook.getX(), hook.getY() - 5, hook.getZ());
        BlockPos topLava = null;
        for (int i = 0; i <= 10; i++) {
            BlockPos check = base.above(i);
            if (hook.level().getFluidState(check).is(FluidTags.LAVA)) {
                topLava = check;
            }
        }
        return (topLava != null) ? topLava.getY() + 0.75 : hook.getY();
    }

    private static void pullTowards(Player player, Entity entity) {
        double dx = player.getX() - entity.getX();
        double dy = player.getY() - entity.getY();
        double dz = player.getZ() - entity.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > 0) {
            double speed = Math.min(1.2, 0.15 * dist);
            entity.setDeltaMovement(dx / dist * speed, dy / dist * speed + 0.2, dz / dist * speed);
            entity.hasImpulse = true;
        }
    }

    public static <T> boolean hasAffix(ItemStack rod, Class<T> affixClass) {
        for (var inst : AffixHelper.getAffixes(rod).values()) {
            if (!inst.isValid()) continue;
            if (affixClass.isInstance(inst.affix().get())) return true;
        }
        return false;
    }
}