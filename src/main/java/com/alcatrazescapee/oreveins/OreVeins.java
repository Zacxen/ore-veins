/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.oreveins;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.BiomeModifications;
import net.neoforged.neoforge.common.world.BiomeSelectors;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import com.alcatrazescapee.oreveins.world.ModFeatures;
import com.alcatrazescapee.oreveins.world.VanillaFeatureManager;
import com.alcatrazescapee.oreveins.world.vein.VeinManager;

import static com.alcatrazescapee.oreveins.OreVeins.MOD_ID;

@Mod(MOD_ID)
public final class OreVeins
{
    public static final String MOD_ID = "oreveins";

    private static final Logger LOGGER = LogManager.getLogger();

    public OreVeins()
    {
        LOGGER.debug("Constructing");

        Config.register();

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::registerWorldGen);
        modEventBus.addListener(this::onAddReloadListeners);
        modEventBus.addListener(this::onLoadConfig);

        ModFeatures.FEATURES.register(modEventBus);

        NeoForge.EVENT_BUS.register(ForgeEventHandler.INSTANCE);
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        LOGGER.debug("Setup");

        event.enqueueWork(() -> {
            BiomeModifications.addFeature(BiomeSelectors.all(), GenerationStep.Decoration.UNDERGROUND_ORES, ModFeatures.PLACED_VEINS);

            VanillaFeatureManager.onConfigReloading();
        });
    }

    private void registerWorldGen(final RegisterEvent event)
    {
        event.register(Registries.CONFIGURED_FEATURE, helper -> helper.register(ModFeatures.CONFIGURED_VEINS,
            new ConfiguredFeature<>(ModFeatures.VEINS.get(), NoneFeatureConfiguration.INSTANCE)));
        event.register(Registries.PLACED_FEATURE, helper -> {
            HolderLookup.RegistryLookup<ConfiguredFeature<?, ?>> lookup = helper.lookup(Registries.CONFIGURED_FEATURE).orElseThrow();
            Holder<ConfiguredFeature<?, ?>> configured = lookup.getOrThrow(ModFeatures.CONFIGURED_VEINS);
            helper.register(ModFeatures.PLACED_VEINS, new PlacedFeature(configured, List.<PlacementModifier>of()));
        });
    }

    private void onAddReloadListeners(final AddReloadListenerEvent event)
    {
        event.addListener(VeinManager.INSTANCE);
    }

    private void onLoadConfig(final ModConfigEvent.Reloading event)
    {
        LOGGER.debug("Reloading config - reevaluating vanilla ore vein settings");
        if (event.getConfig().getType().isServer())
        {
            VanillaFeatureManager.onConfigReloading();
        }
    }
}
