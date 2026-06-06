package net.kayn.apothic_fishing.registry;

import com.mojang.serialization.Codec;
import net.kayn.apothic_fishing.ApothicFishing;
import net.kayn.apothic_fishing.loot.CrateFishingModifier;
import net.kayn.apothic_fishing.loot.BossFishingLootModifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class AFModifiers {

    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, ApothicFishing.MOD_ID);

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> CRATE_FISHING =
            LOOT_MODIFIERS.register("crate_fishing", CrateFishingModifier.CODEC);

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> FISHING_BOSS =
            LOOT_MODIFIERS.register("boss_fishing", () -> BossFishingLootModifier.CODEC);
}