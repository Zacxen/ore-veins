/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.oreveins;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.fml.config.ModConfig;

import com.alcatrazescapee.oreveins.world.ModFeatures;
import com.alcatrazescapee.oreveins.world.VanillaFeatureManager;
import com.alcatrazescapee.oreveins.world.vein.VeinManager;

import static com.alcatrazescapee.oreveins.OreVeins.MOD_ID;

@Mod(MOD_ID)
public final class OreVeins
{
    public static final String MOD_ID = "oreveins";

    private static final Logger LOGGER = LogManager.getLogger();

    public OreVeins(final IEventBus modEventBus, final ModContainer container)
    {
        LOGGER.debug("Constructing");

        Config.register(container);

        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::registerWorldGen);
        modEventBus.addListener(this::onAddReloadListeners);
        modEventBus.addListener(this::onLoadConfig);

        ModFeatures.FEATURES.register(modEventBus);
        ModFeatures.CONFIGURED_FEATURES.register(modEventBus);
        ModFeatures.PLACED_FEATURES.register(modEventBus);
        VanillaFeatureManager.register(modEventBus);

        NeoForge.EVENT_BUS.register(ForgeEventHandler.INSTANCE);
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        LOGGER.debug("Setup");

        event.enqueueWork(VanillaFeatureManager::onConfigReloading);
    }

    private void registerWorldGen(final RegisterEvent event)
    {
        event.register(NeoForgeRegistries.Keys.BIOME_MODIFIERS, helper -> {
            HolderLookup.RegistryLookup<Biome> biomes = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).lookupOrThrow(Registries.BIOME);
            HolderSet<PlacedFeature> features = HolderSet.direct(ModFeatures.PLACED_VEINS);
            HolderSet<Biome> allBiomes = HolderSet.direct(biomes.listElements().toList());

            helper.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, "add_veins"),
                new BiomeModifiers.AddFeaturesBiomeModifier(allBiomes, features, GenerationStep.Decoration.UNDERGROUND_ORES));
            helper.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, "vanilla_ore_replacements"), VanillaFeatureManager.createModifier());
        });
    }

    private void onAddReloadListeners(final AddReloadListenerEvent event)
    {
        event.addListener(VeinManager.INSTANCE);
    }

    private void onLoadConfig(final ModConfigEvent.Reloading event)
    {
        LOGGER.debug("Reloading config - reevaluating vanilla ore vein settings");
        ModConfig.Type type = event.getConfig().getType();
        if (type == ModConfig.Type.SERVER || type == ModConfig.Type.COMMON)
        {
            VanillaFeatureManager.onConfigReloading();
        }
    }
}
