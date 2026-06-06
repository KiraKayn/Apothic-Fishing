package net.kayn.apothic_fishing.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.kayn.apothic_fishing.event.BossFishingHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

public class BossFishingLootModifier extends LootModifier {

    public static final Codec<BossFishingLootModifier> CODEC = RecordCodecBuilder.create(inst ->
            codecStart(inst).and(
                    Codec.floatRange(0f, 1f)
                            .fieldOf("boss_chance")
                            .forGetter(m -> m.bossChance)
            ).apply(inst, BossFishingLootModifier::new)
    );

    private final float bossChance;
    private static float globalBossChance = 0.05f;


    public BossFishingLootModifier(LootItemCondition[] conditions, float bossChance) {
        super(conditions);  
        this.bossChance = bossChance;
        globalBossChance = bossChance;
    }

    public static float getBossChance() {
        return globalBossChance;
    }

    @Override
    @NotNull
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!(context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof FishingHook hook)) {
            return generatedLoot;
        }

        ServerLevel serverLevel = context.getLevel();
        Player player = hook.getPlayerOwner();
        if (player == null) return generatedLoot;

        if (serverLevel.getRandom().nextFloat() >= bossChance) return generatedLoot;

        Mob boss = BossFishingHandler.tryFishBoss(hook, player, serverLevel, player.getLuck());
        if (boss == null) return generatedLoot;

        hook.getPersistentData().putInt("apothic_fishing.hooked_boss_id", boss.getId());

        generatedLoot.clear();
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}