/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */


package com.alcatrazescapee.oreveins.command;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.alcatrazescapee.oreveins.world.vein.VeinManager;
import com.alcatrazescapee.oreveins.world.vein.VeinType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import static com.alcatrazescapee.oreveins.OreVeins.MOD_ID;

public final class ClearWorldCommand
{
    private static final Set<BlockState> VEIN_STATES = new HashSet<>();

    public static void resetVeinStates()
    {
        VeinManager.INSTANCE.getVeins().stream().map(VeinType::getOreStates).forEach(VEIN_STATES::addAll);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(
            Commands.literal("clearworld").requires(source -> source.hasPermission(2))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 250))
                    .executes(cmd -> clearWorld(cmd.getSource(), IntegerArgumentType.getInteger(cmd, "radius")))));
    }

    private static int clearWorld(CommandSourceStack source, int radius)
    {
        final ServerLevel level = source.getLevel();
        final BlockPos center = BlockPos.containing(source.getPosition());
        final BlockState air = Blocks.AIR.defaultBlockState();

        final int minY = level.getMinBuildHeight();
        final int maxY = level.getMaxBuildHeight() - 1;
        final BlockPos minPos = new BlockPos(center.getX() - radius, minY, center.getZ() - radius);
        final BlockPos maxPos = new BlockPos(center.getX() + radius, maxY, center.getZ() + radius);

        for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos))
        {
            if (!VEIN_STATES.contains(level.getBlockState(pos)))
            {
                level.setBlock(pos, air, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }

        source.sendSuccess(() -> Component.translatable(MOD_ID + ".command.clear_world_done"), true);
        return 1;
    }
}
