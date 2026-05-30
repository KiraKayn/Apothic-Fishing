package net.kayn.apothic_fishing.crate;

import dev.shadowsoffire.placebo.reload.WeightedDynamicRegistry;
import net.kayn.apothic_fishing.ApothicFishing;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.Optional;
import java.util.Set;

public class CrateRegistry extends WeightedDynamicRegistry<CrateDefinition> {

    public static final CrateRegistry INSTANCE = new CrateRegistry();

    private CrateRegistry() {
        super(ApothicFishing.LOGGER, "crates", true, false);
    }

    @Override
    protected void registerBuiltinCodecs() {
        this.registerDefaultCodec(
                new ResourceLocation(ApothicFishing.MOD_ID, "crate"),
                CrateDefinition.CODEC);
    }

    public static Optional<CrateDefinition> getRandomCrateForLocation(
            RandomSource rand,
            float luck,
            ResourceLocation dimension,
            ResourceLocation biome) {

        CrateDefinition result = INSTANCE.getRandomItem(rand, luck,
                crate -> isValidForLocation(crate, dimension, biome));
        return Optional.ofNullable(result);
    }

    private static boolean isValidForLocation(CrateDefinition crate,
                                              ResourceLocation dimension,
                                              ResourceLocation biome) {
        Set<ResourceLocation> dims   = crate.getDimensions();
        Set<ResourceLocation> biomes = crate.getBiomes();

        boolean dimEmpty   = dims.isEmpty();
        boolean biomeEmpty = biomes.isEmpty();

        if (!dimEmpty && !biomeEmpty) return dims.contains(dimension) && biomes.contains(biome);
        if (!dimEmpty)               return dims.contains(dimension);
        if (!biomeEmpty)             return biomes.contains(biome);
        return true;
    }
}