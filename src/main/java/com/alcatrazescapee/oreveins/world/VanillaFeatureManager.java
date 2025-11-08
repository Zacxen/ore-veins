/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.oreveins.world;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

import com.alcatrazescapee.oreveins.Config;
import com.alcatrazescapee.oreveins.OreVeins;

public final class VanillaFeatureManager
{
    private static Set<BlockState> disabledBlockStates = new HashSet<>();
    private static boolean disableAll = false;

    private VanillaFeatureManager() {}

    public static void register(IEventBus ignored) {}

    public static void onConfigReloading()
    {
        disabledBlockStates = new HashSet<>(Config.COMMON.disabledBlockStates());
        disableAll = Config.COMMON.noOres.get();
    }

    public static BiomeModifier createModifier()
    {
        return VanillaOreRemovalModifier.INSTANCE;
    }

    private static boolean shouldDisable(Holder<PlacedFeature> holder)
    {
        if (!disableAll && disabledBlockStates.isEmpty())
        {
            return false;
        }
        PlacedFeature placedFeature = holder.value();
        Holder<ConfiguredFeature<?, ?>> configured = placedFeature.feature();
        FeatureConfiguration configuration = configured.value().config();
        if (configuration instanceof OreConfiguration oreConfiguration)
        {
            if (disableAll)
            {
                return isVanillaOre(configured);
            }
            for (OreConfiguration.TargetBlockState target : oreConfiguration.targetStates)
            {
                if (disabledBlockStates.contains(target.state))
                {
                    return isVanillaOre(configured);
                }
            }
        }
        return false;
    }

    private static boolean isVanillaOre(Holder<ConfiguredFeature<?, ?>> configured)
    {
        ResourceLocation location = configured.unwrapKey().map(ResourceKey::location).orElse(null);
        if (location != null)
        {
            return location.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE);
        }
        Feature<?> feature = configured.value().feature();
        return feature == Feature.ORE || feature == Feature.SCATTERED_ORE;
    }

    private enum VanillaOreRemovalModifier implements BiomeModifier
    {
        INSTANCE;

        private static final MapCodec<VanillaOreRemovalModifier> CODEC = MapCodec.unit(() -> INSTANCE);

        @Override
        public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder)
        {
            if (phase != Phase.REMOVE)
            {
                return;
            }
            List<Holder<PlacedFeature>> features = builder.getGenerationSettings().getFeatures(GenerationStep.Decoration.UNDERGROUND_ORES);
            features.removeIf(VanillaFeatureManager::shouldDisable);
        }

        @Override
        public MapCodec<? extends BiomeModifier> codec()
        {
            return CODEC;
        }
    }
}
