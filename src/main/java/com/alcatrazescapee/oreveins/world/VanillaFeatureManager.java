/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.oreveins.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import net.neoforged.neoforge.common.world.BiomeModificationContext;
import net.neoforged.neoforge.common.world.BiomeModifications;
import net.neoforged.neoforge.common.world.BiomeSelectors;

import com.alcatrazescapee.oreveins.Config;
import com.alcatrazescapee.oreveins.OreVeins;

public final class VanillaFeatureManager
{
    private static final Map<ResourceKey<Biome>, List<Holder<PlacedFeature>>> DISABLED_FEATURES = new HashMap<>();

    private static Set<BlockState> disabledBlockStates = new HashSet<>();
    private static boolean disableAll = false;
    private static boolean registered = false;

    private VanillaFeatureManager() {}

    public static void init()
    {
        if (registered)
        {
            return;
        }
        BiomeModifications.create(new ResourceLocation(OreVeins.MOD_ID, "vanilla_ore_replacements"))
            .add(BiomeModifications.ModificationPhase.REPLACEMENTS, BiomeSelectors.all(), VanillaFeatureManager::apply);
        registered = true;
    }

    public static void onConfigReloading()
    {
        disabledBlockStates = new HashSet<>(Config.COMMON.disabledBlockStates());
        disableAll = Config.COMMON.noOres.get();
    }

    private static void apply(BiomeModificationContext context)
    {
        ResourceKey<Biome> biomeKey = context.getBiomeKey().orElse(null);
        if (biomeKey == null)
        {
            return;
        }

        List<Holder<PlacedFeature>> disabledFeatures = DISABLED_FEATURES.computeIfAbsent(biomeKey, key -> new ArrayList<>());
        BiomeModificationContext.GenerationSettings generation = context.getGenerationSettings();
        List<Holder<PlacedFeature>> features = generation.getFeatures(GenerationStep.Decoration.UNDERGROUND_ORES);

        if (!disabledFeatures.isEmpty())
        {
            Iterator<Holder<PlacedFeature>> iterator = disabledFeatures.iterator();
            while (iterator.hasNext())
            {
                Holder<PlacedFeature> holder = iterator.next();
                if (!shouldDisable(holder))
                {
                    features.add(holder);
                    iterator.remove();
                }
            }
        }

        Iterator<Holder<PlacedFeature>> iterator = features.iterator();
        while (iterator.hasNext())
        {
            Holder<PlacedFeature> holder = iterator.next();
            if (shouldDisable(holder))
            {
                iterator.remove();
                disabledFeatures.add(holder);
            }
        }
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
                if (disabledBlockStates.contains(target.state()))
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
}
