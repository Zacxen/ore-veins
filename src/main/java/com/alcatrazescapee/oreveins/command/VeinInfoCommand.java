/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.oreveins.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.alcatrazescapee.oreveins.world.vein.VeinManager;
import com.alcatrazescapee.oreveins.world.vein.VeinType;
import com.mojang.brigadier.CommandDispatcher;

import static com.alcatrazescapee.oreveins.OreVeins.MOD_ID;

public final class VeinInfoCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(
            Commands.literal("veininfo").requires(source -> source.hasPermission(2))
                .then(Commands.argument("type", new VeinTypeArgument())
                    .executes(cmd -> veinInfo(cmd.getSource(), VeinTypeArgument.getVein(cmd, "type")))
                ));
    }

    private static int veinInfo(CommandSourceStack source, ResourceLocation veinName)
    {
        // Search for veins that match a type
        final VeinType<?> type = VeinManager.INSTANCE.getVein(veinName);
        if (type == null)
        {
            source.sendFailure(Component.translatable(MOD_ID + ".command.unknown_vein", veinName.toString()));
        }
        else
        {
            source.sendSuccess(() -> Component.translatable(MOD_ID + ".command.vein_info", type.toString()), true);
        }
        return 1;
    }
}
