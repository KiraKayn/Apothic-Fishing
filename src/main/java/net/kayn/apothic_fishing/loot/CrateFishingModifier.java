package net.kayn.apothic_fishing.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.kayn.apothic_fishing.ApothicFishing;
import net.kayn.apothic_fishing.crate.CrateItem;
import net.kayn.apothic_fishing.crate.CrateRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class CrateFishingModifier extends LootModifier {

    public static final Supplier<Codec<CrateFishingModifier>> CODEC =
            () -> RecordCodecBuilder.create(inst ->
                    codecStart(inst).apply(inst, CrateFishingModifier::new));

    public CrateFishingModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(
            ObjectArrayList<ItemStack> generatedLoot, LootContext context) {

        ServerLevel serverLevel = context.getLevel();

        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        if (origin == null) {
            ApothicFishing.LOGGER.warn("[ApothicFishing] No ORIGIN in fishing context; skipping.");
            return generatedLoot;
        }

        BlockPos hookPos = BlockPos.containing(origin);
        ResourceLocation dimensionId = serverLevel.dimension().location();

        Holder<Biome> biomeHolder = serverLevel.getBiome(hookPos);
        ResourceLocation biomeId = biomeHolder.unwrapKey()
                .map(ResourceKey::location)
                .orElse(null);

        if (biomeId == null) {
            ApothicFishing.LOGGER.warn("[ApothicFishing] Could not resolve biome at {}.", hookPos);
            return generatedLoot;
        }

        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        float luck = (entity instanceof Player player) ? player.getLuck() : 0f;

        if (CrateRegistry.INSTANCE.getValues().isEmpty()) {
            return generatedLoot;
        }

        if (serverLevel.getRandom().nextFloat() > 0.15f) return generatedLoot;

        CrateRegistry.getRandomCrateForLocation(serverLevel.getRandom(), luck, dimensionId, biomeId)
                .ifPresent(crate -> {
                    ResourceLocation crateId = CrateRegistry.INSTANCE.getKey(crate);
                    if (crateId != null) {
                        ApothicFishing.LOGGER.debug("[ApothicFishing] Replacing fishing loot with crate {}.", crateId);
                        generatedLoot.clear();
                        generatedLoot.add(CrateItem.createCrateStack(crateId));
                    }
                });

        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}