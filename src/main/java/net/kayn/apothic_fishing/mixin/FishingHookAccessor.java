package net.kayn.apothic_fishing.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FishingHook.class)
public interface FishingHookAccessor {

    @Accessor("nibble")
    int getNibble();
    @Accessor("nibble")
    void setNibble(int value);

    @Accessor("timeUntilLured")
    int getTimeUntilLured();
    @Accessor("timeUntilLured")
    void setTimeUntilLured(int value);

    @Accessor("timeUntilHooked")
    int getTimeUntilHooked();
    @Accessor("timeUntilHooked")
    void setTimeUntilHooked(int value);

    @Accessor("fishAngle")
    float getFishAngle();
    @Accessor("fishAngle")
    void setFishAngle(float value);

    @Accessor("outOfWaterTime")
    int getOutOfWaterTime();
    @Accessor("outOfWaterTime")
    void setOutOfWaterTime(int value);

    @Accessor("lureSpeed")
    int getLureSpeed();

    @Accessor("luck")
    int getLuck();

    @Accessor("life")
    int getLife();
    @Accessor("life")
    void setLife(int value);

    @Accessor("hookedIn")
    Entity getHookedIn();
    @Accessor("hookedIn")
    void setHookedIn(Entity entity);

    @Invoker("setHookedEntity")
    void invokeSetHookedEntity(Entity entity);

    @Accessor("DATA_BITING")
    static EntityDataAccessor<Boolean> getDATA_BITING() {
        throw new UnsupportedOperationException("Mixin replaces this");
    }
}