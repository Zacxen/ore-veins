/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.oreveins.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryObject;

import static com.alcatrazescapee.oreveins.OreVeins.MOD_ID;

public final class ModFeatures
{
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MOD_ID);

    public static final RegistryObject<VeinsFeature> VEINS = FEATURES.register("veins", VeinsFeature::new);

    public static final ResourceKey<ConfiguredFeature<?, ?>> CONFIGURED_VEINS = ResourceKey.create(Registries.CONFIGURED_FEATURE, new ResourceLocation(MOD_ID, "veins"));
    public static final ResourceKey<PlacedFeature> PLACED_VEINS = ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation(MOD_ID, "veins"));

    private ModFeatures() {}
}
