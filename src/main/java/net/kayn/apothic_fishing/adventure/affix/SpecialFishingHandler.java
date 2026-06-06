package net.kayn.apothic_fishing.adventure.affix;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import net.kayn.apothic_fishing.ApothicFishing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SpecialFishingHandler {

    public static final ResourceLocation LAVA_LOOT_TABLE = new ResourceLocation(ApothicFishing.MOD_ID, "gameplay/lava_fishing");
    public static final ResourceLocation VOID_LOOT_TABLE = new ResourceLocation(ApothicFishing.MOD_ID, "gameplay/void_fishing");

    private static final Set<FishingHook> tickingHooks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final java.util.Map<java.util.UUID, Double> lavaHookSurfaceY = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<java.util.UUID, double[]> lockedPositions = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<Integer, double[]> clientLockedPositions = new ConcurrentHashMap<>();
    private static final java.util.Map<java.util.UUID, net.minecraft.world.entity.Entity> hookedBosses = new java.util.concurrent.ConcurrentHashMap<>();

    private static final Field NIBBLE;
    private static final Field TIME_UNTIL_LURED;
    private static final Field TIME_UNTIL_HOOKED;
    private static final Field FISH_ANGLE;
    private static final Field OUT_OF_WATER_TIME;
    private static final Field CURRENT_STATE;
    private static final Field LURE_SPEED;
    private static final Field LUCK;
    private static final Field LIFE;
    private static final Field DATA_BITING;
    private static final Method CHECK_COLLISION;
    private static final Method SHOULD_STOP_FISHING;
    private static final Method REAPPLY_POSITION;
    private static final Object STATE_FLYING;
    private static final Object STATE_BOBBING;
    private static final Object STATE_HOOKED;
    private static final Field HOOKED_IN;

    static {
        NIBBLE = field(FishingHook.class, "nibble", "f_37113_");
        TIME_UNTIL_LURED = field(FishingHook.class, "timeUntilLured", "f_37116_");
        TIME_UNTIL_HOOKED = field(FishingHook.class, "timeUntilHooked", "f_37117_");
        FISH_ANGLE = field(FishingHook.class, "fishAngle", "f_37118_");
        OUT_OF_WATER_TIME = field(FishingHook.class, "outOfWaterTime", "f_150157_");
        CURRENT_STATE = field(FishingHook.class, "currentState", "f_37115_");
        LURE_SPEED = field(FishingHook.class, "lureSpeed", "f_37114_");
        LUCK = field(FishingHook.class, "luck", "f_37112_");
        LIFE = field(FishingHook.class, "life", "f_37120_");
        DATA_BITING = field(FishingHook.class, "DATA_BITING", "f_37110_");
        CHECK_COLLISION = method(FishingHook.class, "checkCollision", "m_37173_");
        SHOULD_STOP_FISHING = method(FishingHook.class, "shouldStopFishing", "m_37137_", Player.class);
        REAPPLY_POSITION = method(FishingHook.class, "reapplyPosition", "m_6296_");
        HOOKED_IN = field(FishingHook.class, "hookedIn", "f_37111_");

        Object flying = null, bobbing = null, hooked = null;
        try {
            for (Class<?> inner : FishingHook.class.getDeclaredClasses()) {
                if (inner.isEnum() && inner.getSimpleName().equals("FishHookState")) {
                    for (Object c : inner.getEnumConstants()) {
                        String n = ((Enum<?>) c).name();
                        if (n.equals("FLYING")) flying = c;
                        if (n.equals("BOBBING")) bobbing = c;
                        if (n.equals("HOOKED_IN_ENTITY")) hooked = c;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        STATE_FLYING = flying;
        STATE_BOBBING = bobbing;
        STATE_HOOKED = hooked;
    }

    public static boolean handleSpecialTick(FishingHook hook) {
        if (tickingHooks.contains(hook)) return false;

        Player owner = hook.getPlayerOwner();
        if (owner == null) return false;

        ItemStack rod = ModHelper.getHeldRod(owner);
        if (rod == null) return false;

        boolean hasLava = hasAffix(rod, LavaFishAffix.class);
        boolean hasVoid = hasAffix(rod, VoidFishAffix.class);
        if (!hasLava && !hasVoid) return false;

        boolean inLava = isInLava(hook);
        boolean inVoid = isInVoid(hook);

        BlockPos bp = hook.blockPosition();
        boolean inWater = hook.level().getFluidState(bp).is(net.minecraft.tags.FluidTags.WATER);
        if (inWater) return false;

        Object state = get(CURRENT_STATE, hook);
        boolean isFlying = STATE_FLYING.equals(state);
        boolean isHooked = STATE_HOOKED.equals(state);

        if (isHooked) {

            Object hookedIn = get(HOOKED_IN, hook);
            if (hookedIn instanceof net.minecraft.world.entity.Entity entity
                    && entity.getPersistentData().getBoolean("apoth.boss")) {
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
            hook.discard();
            return;
        }

        Object state = get(CURRENT_STATE, hook);
        if (!STATE_FLYING.equals(state)) return;

        hook.setDeltaMovement(hook.getDeltaMovement().add(0, -0.03, 0));
        hook.move(net.minecraft.world.entity.MoverType.SELF, hook.getDeltaMovement());
        hook.setDeltaMovement(hook.getDeltaMovement().scale(0.92));
        invokeVoid(CHECK_COLLISION, hook);
        reapply(hook);
    }

    private static void lavaFishingTick(FishingHook hook, Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        if (shouldStopFishing(hook, player)) {
            if (player.level() instanceof ServerLevel sl && trySpawnAndHookBoss(hook, sl)) {
                Object hookedIn = get(HOOKED_IN, hook);
                if (hookedIn instanceof net.minecraft.world.entity.Entity entity) {
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
                lavaHookSurfaceY.remove(hook.getUUID());
                lockedPositions.remove(hook.getUUID());
                hook.discard();
                return;
            }
            lavaHookSurfaceY.remove(hook.getUUID());
            lockedPositions.remove(hook.getUUID());
            hook.discard();
            return;
        }

        if (hook.onGround()) {
            int life = getInt(LIFE, hook) + 1;
            setInt(LIFE, hook, life);
            if (life >= 1200) {
                lavaHookSurfaceY.remove(hook.getUUID());
                lockedPositions.remove(hook.getUUID());
                hook.discard();
                return;
            }
        } else {
            setInt(LIFE, hook, 0);
        }

        Object state = get(CURRENT_STATE, hook);
        double surfaceY = getOrComputeLavaSurfaceY(hook);
        boolean inLava = isInLavaLoose(hook);

        if (STATE_FLYING.equals(state)) {
            if (inLava) {
                hook.setDeltaMovement(hook.getDeltaMovement().multiply(0.3D, 0.2D, 0.3D));
                set(CURRENT_STATE, hook, STATE_BOBBING);
                hook.setPos(hook.getX(), surfaceY, hook.getZ());
                hook.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                lockedPositions.put(hook.getUUID(), new double[]{hook.getX(), surfaceY, hook.getZ()});
            } else {
                hook.setDeltaMovement(hook.getDeltaMovement().add(0, -0.03, 0));
            }
            hook.move(net.minecraft.world.entity.MoverType.SELF, hook.getDeltaMovement());
            hook.setDeltaMovement(hook.getDeltaMovement().scale(0.92));
            if (STATE_FLYING.equals(state) && (hook.onGround() || hook.horizontalCollision)) {
                hook.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            }
            reapply(hook);
            return;
        }

        if (STATE_HOOKED.equals(state)) {
            hookedEntityTick(hook, player);
            return;
        }

        if (STATE_BOBBING.equals(state)) {
            hook.setPos(hook.getX(), surfaceY, hook.getZ());
            hook.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);

            if (inLava) {
                setInt(OUT_OF_WATER_TIME, hook, Math.max(0, getInt(OUT_OF_WATER_TIME, hook) - 1));
                lavaCatchingFish(hook, serverLevel);
            } else {
                setInt(OUT_OF_WATER_TIME, hook, Math.min(10, getInt(OUT_OF_WATER_TIME, hook) + 1));
            }
        }
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

    private static void snapToLavaSurface(FishingHook hook) {
        Double cached = lavaHookSurfaceY.get(hook.getUUID());
        if (cached != null) {
            hook.setPos(hook.getX(), cached, hook.getZ());
            hook.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            lockedPositions.put(hook.getUUID(), new double[]{hook.getX(), cached, hook.getZ()});
            return;
        }

        BlockPos base = BlockPos.containing(hook.getX(), hook.getY() - 5, hook.getZ());
        BlockPos topLava = null;
        for (int i = 0; i <= 10; i++) {
            BlockPos check = base.above(i);
            if (hook.level().getFluidState(check).is(FluidTags.LAVA)) {
                topLava = check;
            }
        }

        double surfaceY = (topLava != null) ? topLava.getY() + 0.75 : hook.getY();
        lavaHookSurfaceY.put(hook.getUUID(), surfaceY);
        lockedPositions.put(hook.getUUID(), new double[]{hook.getX(), surfaceY, hook.getZ()});
        hook.setPos(hook.getX(), surfaceY, hook.getZ());
        hook.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
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

    private static void lavaCatchingFish(FishingHook hook, ServerLevel level) {
        int nibble = getInt(NIBBLE, hook);
        int untilHooked = getInt(TIME_UNTIL_HOOKED, hook);
        int untilLured = getInt(TIME_UNTIL_LURED, hook);

        if (nibble > 0) {
            setInt(NIBBLE, hook, nibble - 1);
            if (nibble - 1 <= 0) {
                setInt(TIME_UNTIL_LURED, hook, 0);
                setInt(TIME_UNTIL_HOOKED, hook, 0);
                setBiting(hook, false);
            }
            return;
        }

        if (untilHooked > 0) {
            setInt(TIME_UNTIL_HOOKED, hook, untilHooked - 1);
            if (untilHooked - 1 > 0) {
                float angle = getFloat(FISH_ANGLE, hook) + (float) hook.level().getRandom().nextGaussian() * 4f;
                setFloat(FISH_ANGLE, hook, angle);
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
                hook.playSound(SoundEvents.LAVA_POP, 0.25f, 1.0f + (hook.level().getRandom().nextFloat() - hook.level().getRandom().nextFloat()) * 0.4f);
                double d = hook.getY() + 0.5;
                level.sendParticles(ParticleTypes.LAVA, hook.getX(), d, hook.getZ(), (int) (1f + hook.getBbWidth() * 20f), hook.getBbWidth(), 0, hook.getBbWidth(), 0.2f);
                level.sendParticles(ParticleTypes.SMOKE, hook.getX(), d, hook.getZ(), (int) (1f + hook.getBbWidth() * 20f), hook.getBbWidth(), 0, hook.getBbWidth(), 0.2f);
                setInt(NIBBLE, hook, Mth.nextInt(hook.level().getRandom(), 20, 40));
                setBiting(hook, true);
            }
            return;
        }

        if (untilLured > 0) {
            setInt(TIME_UNTIL_LURED, hook, untilLured - 1);
            float chance = 0.15f;
            if (untilLured < 20) chance += (20 - untilLured) * 0.05f;
            else if (untilLured < 40) chance += (40 - untilLured) * 0.02f;
            else if (untilLured < 60) chance += (60 - untilLured) * 0.01f;
            if (hook.level().getRandom().nextFloat() < chance) {
                float r = Mth.nextFloat(hook.level().getRandom(), 0f, 360f) * (float) (Math.PI / 180f);
                float dist = Mth.nextFloat(hook.level().getRandom(), 25f, 60f);
                level.sendParticles(ParticleTypes.LAVA, hook.getX() + Mth.sin(r) * dist * 0.1, Mth.floor(hook.getY()) + 1.0, hook.getZ() + Mth.cos(r) * dist * 0.1, 2, 0.1, 0, 0.1, 0);
            }
            if (untilLured - 1 <= 0) {
                setFloat(FISH_ANGLE, hook, Mth.nextFloat(hook.level().getRandom(), 0f, 360f));
                setInt(TIME_UNTIL_HOOKED, hook, Mth.nextInt(hook.level().getRandom(), 20, 80));
            }
            return;
        }

        int lureSpeed = getInt(LURE_SPEED, hook);
        setInt(TIME_UNTIL_LURED, hook, Math.max(1, Mth.nextInt(hook.level().getRandom(), 100, 600) - lureSpeed * 20 * 5));
    }

    private static void voidFishingTick(FishingHook hook, Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        if (shouldStopFishing(hook, player)) {
            if (player.level() instanceof ServerLevel sl && trySpawnAndHookBoss(hook, sl)) {
                Object hookedIn = get(HOOKED_IN, hook);
                if (hookedIn instanceof net.minecraft.world.entity.Entity entity) {
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
                lockedPositions.remove(hook.getUUID());
                hook.discard();
                return;
            }
            lockedPositions.remove(hook.getUUID());
            hook.discard();
            return;
        }

        Object state = get(CURRENT_STATE, hook);

        if (STATE_FLYING.equals(state)) {
            set(CURRENT_STATE, hook, STATE_BOBBING);
            hook.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            hook.setPos(hook.getX(), hook.getY(), hook.getZ());
            lockedPositions.put(hook.getUUID(), new double[]{hook.getX(), hook.getY(), hook.getZ()});
            return;
        }

        if (STATE_HOOKED.equals(state)) {
            hookedEntityTick(hook, player);
            return;
        }

        if (STATE_BOBBING.equals(state)) {
            double[] pos = lockedPositions.get(hook.getUUID());
            if (pos != null) {
                hook.setPos(pos[0], pos[1], pos[2]);
            }
            hook.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            voidCatchingFish(hook, serverLevel);
        }
    }


    private static void voidCatchingFish(FishingHook hook, ServerLevel level) {
        int nibble = getInt(NIBBLE, hook);
        int untilHooked = getInt(TIME_UNTIL_HOOKED, hook);
        int untilLured = getInt(TIME_UNTIL_LURED, hook);

        if (nibble > 0) {
            setInt(NIBBLE, hook, nibble - 1);
            if (nibble - 1 <= 0) {
                setInt(TIME_UNTIL_LURED, hook, 0);
                setInt(TIME_UNTIL_HOOKED, hook, 0);
                setBiting(hook, false);
            }
            return;
        }

        if (untilHooked > 0) {
            setInt(TIME_UNTIL_HOOKED, hook, untilHooked - 1);
            if (untilHooked - 1 > 0) {
                float angle = getFloat(FISH_ANGLE, hook) + (float) hook.level().getRandom().nextGaussian() * 4f;
                setFloat(FISH_ANGLE, hook, angle);
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
                hook.playSound(SoundEvents.ENDERMAN_AMBIENT, 0.25f, 1.0f + (hook.level().getRandom().nextFloat() - hook.level().getRandom().nextFloat()) * 0.4f);
                level.sendParticles(ParticleTypes.DRAGON_BREATH, hook.getX(), hook.getY(), hook.getZ(), (int) (1f + hook.getBbWidth() * 20f), hook.getBbWidth(), 0, hook.getBbWidth(), 0.2f);
                level.sendParticles(ParticleTypes.PORTAL, hook.getX(), hook.getY(), hook.getZ(), (int) (1f + hook.getBbWidth() * 20f), hook.getBbWidth(), 0, hook.getBbWidth(), 0.2f);
                setInt(NIBBLE, hook, Mth.nextInt(hook.level().getRandom(), 20, 40));
                setBiting(hook, true);
            }
            return;
        }

        if (untilLured > 0) {
            setInt(TIME_UNTIL_LURED, hook, untilLured - 1);
            float chance = 0.15f;
            if (untilLured < 20) chance += (20 - untilLured) * 0.05f;
            else if (untilLured < 40) chance += (40 - untilLured) * 0.02f;
            else if (untilLured < 60) chance += (60 - untilLured) * 0.01f;
            if (hook.level().getRandom().nextFloat() < chance) {
                float r = Mth.nextFloat(hook.level().getRandom(), 0f, 360f) * (float) (Math.PI / 180f);
                float dist = Mth.nextFloat(hook.level().getRandom(), 25f, 60f);
                level.sendParticles(ParticleTypes.PORTAL, hook.getX() + Mth.sin(r) * dist * 0.1, hook.getY(), hook.getZ() + Mth.cos(r) * dist * 0.1, 4, 0.1, 0, 0.1, 0.5);
            }
            if (untilLured - 1 <= 0) {
                setFloat(FISH_ANGLE, hook, Mth.nextFloat(hook.level().getRandom(), 0f, 360f));
                setInt(TIME_UNTIL_HOOKED, hook, Mth.nextInt(hook.level().getRandom(), 20, 80));
            }
            return;
        }

        int lureSpeed = getInt(LURE_SPEED, hook);
        setInt(TIME_UNTIL_LURED, hook, Math.max(1, Mth.nextInt(hook.level().getRandom(), 100, 600) - lureSpeed * 20 * 5));
    }


    public static void tryAcquireClientLock(FishingHook hook) {
        if (clientLockedPositions.containsKey(hook.getId())) return;

        if (isInVoid(hook)) {
            clientLockedPositions.put(hook.getId(),
                    new double[]{hook.getX(), hook.getY(), hook.getZ()});
            return;
        }


        BlockPos bp = hook.blockPosition();
        boolean strictlyInLava = hook.level().getFluidState(bp).is(FluidTags.LAVA) || hook.isInLava();
        if (!strictlyInLava) return;

        double surfaceY = computeLavaSurfaceY(hook);
        clientLockedPositions.put(hook.getId(),
                new double[]{hook.getX(), surfaceY, hook.getZ()});
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

    private static void hookedEntityTick(FishingHook hook, Player player) {
        if (shouldStopFishing(hook, player)) {
            Object hookedIn = get(HOOKED_IN, hook);
            if (hookedIn instanceof net.minecraft.world.entity.Entity entity) {
                double dx = player.getX() - entity.getX();
                double dy = player.getY() - entity.getY();
                double dz = player.getZ() - entity.getZ();
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist > 0) {
                    double speed = Math.min(1.2, 0.15 * dist);
                    entity.setDeltaMovement(
                            dx / dist * speed,
                            dy / dist * speed + 0.2,
                            dz / dist * speed
                    );
                    entity.hasImpulse = true;
                }
            }
            lavaHookSurfaceY.remove(hook.getUUID());
            lockedPositions.remove(hook.getUUID());
            hook.discard();
            return;
        }

        Object hookedIn = get(HOOKED_IN, hook);
        if (hookedIn instanceof net.minecraft.world.entity.Entity entity && !entity.isRemoved()) {
            double dx = hook.getX() - entity.getX();
            double dy = hook.getY() - entity.getY();
            double dz = hook.getZ() - entity.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > 0.5) {
                double speed = 0.12;
                entity.setDeltaMovement(
                        entity.getDeltaMovement().add(dx / dist * speed, dy / dist * speed, dz / dist * speed)
                );
                entity.hasImpulse = true;
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

        try {
            java.lang.reflect.Method setHooked = FishingHook.class.getDeclaredMethod(
                    "setHookedEntity", net.minecraft.world.entity.Entity.class);
            setHooked.setAccessible(true);
            setHooked.invoke(hook, mob);
        } catch (Exception e) {
            return false;
        }

        set(CURRENT_STATE, hook, STATE_HOOKED);
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
        return get(HOOKED_IN, hook);
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
                .withLuck((float) getInt(LUCK, hook) + player.getLuck())
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
        if (SHOULD_STOP_FISHING == null) return false;
        try {
            return (boolean) SHOULD_STOP_FISHING.invoke(hook, player);
        } catch (Exception e) {
            return false;
        }
    }

    private static void reapply(FishingHook hook) {
        if (REAPPLY_POSITION == null) return;
        try {
            REAPPLY_POSITION.invoke(hook);
        } catch (Exception ignored) {
        }
    }

    private static void invokeVoid(Method m, Object target, Object... args) {
        if (m == null) return;
        try {
            m.invoke(target, args);
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private static void setBiting(FishingHook hook, boolean biting) {
        if (DATA_BITING == null) return;
        try {
            Object accessor = DATA_BITING.get(null);
            hook.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Boolean>) accessor, biting);
        } catch (Exception ignored) {
        }
    }

    private static Object get(Field f, Object o) {
        if (f == null) return null;
        try {
            return f.get(o);
        } catch (Exception e) {
            return null;
        }
    }

    private static void set(Field f, Object o, Object v) {
        if (f == null) return;
        try {
            f.set(o, v);
        } catch (Exception ignored) {
        }
    }

    private static int getInt(Field f, Object o) {
        Object v = get(f, o);
        return v instanceof Number n ? n.intValue() : 0;
    }

    private static void setInt(Field f, Object o, int v) {
        set(f, o, v);
    }

    private static float getFloat(Field f, Object o) {
        Object v = get(f, o);
        return v instanceof Number n ? n.floatValue() : 0f;
    }

    private static void setFloat(Field f, Object o, float v) {
        set(f, o, v);
    }

    private static Field field(Class<?> cls, String name, String srg) {
        try {
            Field f = cls.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            try {
                return ObfuscationReflectionHelper.findField(cls, srg);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private static Method method(Class<?> cls, String name, String srg, Class<?>... params) {
        try {
            Method m = cls.getDeclaredMethod(name, params);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            try {
                return ObfuscationReflectionHelper.findMethod(cls, srg, params);
            } catch (Exception e2) {
                return null;
            }
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