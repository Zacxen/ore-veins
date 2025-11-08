/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.oreveins.world;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.alcatrazescapee.oreveins.OreVeins.MOD_ID;

public final class ModFeatures
{
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MOD_ID);
    public static final DeferredRegister<ConfiguredFeature<?, ?>> CONFIGURED_FEATURES = DeferredRegister.create(Registries.CONFIGURED_FEATURE, MOD_ID);
    public static final DeferredRegister<PlacedFeature> PLACED_FEATURES = DeferredRegister.create(Registries.PLACED_FEATURE, MOD_ID);

    public static final DeferredHolder<Feature<?>, VeinsFeature> VEINS = FEATURES.register("veins", VeinsFeature::new);
    public static final DeferredHolder<ConfiguredFeature<?, ?>, ConfiguredFeature<NoneFeatureConfiguration, ?>> CONFIGURED_VEINS = CONFIGURED_FEATURES.register("veins", () -> new ConfiguredFeature<>(VEINS.get(), NoneFeatureConfiguration.INSTANCE));
    public static final DeferredHolder<PlacedFeature, PlacedFeature> PLACED_VEINS = PLACED_FEATURES.register("veins", () -> new PlacedFeature(CONFIGURED_VEINS, List.of()));

    private ModFeatures() {}
}
