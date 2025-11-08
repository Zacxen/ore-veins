/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */


package com.alcatrazescapee.oreveins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

import com.alcatrazescapee.oreveins.util.json.BlockStateDeserializer;

public final class Config
{
    public static final CommonConfig COMMON;

    private static final ModConfigSpec COMMON_SPEC;

    static
    {
        final Pair<CommonConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(CommonConfig::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    public static void register(ModContainer container)
    {
        container.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
    }

    public static final class CommonConfig
    {
        public final ModConfigSpec.BooleanValue noOres;
        public final ModConfigSpec.BooleanValue debugCommands;
        public final ModConfigSpec.IntValue extraChunkRange;
        public final ModConfigSpec.BooleanValue avoidVeinCutoffs;

        private final ModConfigSpec.ConfigValue<List<? extends String>> disabledOres;

        CommonConfig(ModConfigSpec.Builder builder)
        {
            builder.push("general");

            noOres = builder
                .comment("Stop all vanilla ore gen calls? Warning: this includes calls such as andesite/diorite, and potentially others that internally behave the same as ores. For more customization, see the disabled ores option.")
                .define("noOres", true);

            disabledOres = builder
                .comment("Vanilla ore gen to disable. Must be specified as a list of block states, i.e. minecraft:gold_ore, minecraft:iron_ore, etc.")
                .defineList("disabledOres", this::defaultDisabledBlockStates, obj -> BlockStateDeserializer.INSTANCE.isBlockState(obj.toString()));

            debugCommands = builder
                    .comment("Enable debug commands such as /veininfo, /clearworld, /findveins")
                    .define("debugCommands", true);

            extraChunkRange = builder
                    .comment("Extra chunk search range when generating veins", "Use if your veins are getting cut off at chunk boundaries")
                    .defineInRange("extraChunkRange", 0, 0, 20);

            avoidVeinCutoffs = builder
                    .comment("Try to avoid placing veins on the edge of their range as defined by min / max y, so they don't get cut off at the border.")
                    .define("avoidVeinCutoffs", true);

            builder.pop();
        }

        public Set<BlockState> disabledBlockStates()
        {
            return disabledOres.get().stream().map(BlockStateDeserializer.INSTANCE::readBlockState).collect(Collectors.toSet());
        }

        private List<? extends String> defaultDisabledBlockStates()
        {
            return new ArrayList<>(Arrays.asList("minecraft:coal_ore", "minecraft:iron_ore", "minecraft:gold_ore", "minecraft:diamond_ore", "minecraft:lapis_ore", "minecraft:redstone_ore", "minecraft:diamond_ore"));
        }
    }
}
