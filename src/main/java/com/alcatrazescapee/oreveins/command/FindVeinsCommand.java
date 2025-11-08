/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.oreveins.command;

import java.util.List;

import com.google.gson.JsonParseException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import com.alcatrazescapee.oreveins.world.VeinsFeature;
import com.alcatrazescapee.oreveins.world.vein.Vein;
import com.alcatrazescapee.oreveins.world.vein.VeinManager;
import com.alcatrazescapee.oreveins.world.vein.VeinType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import static com.alcatrazescapee.oreveins.OreVeins.MOD_ID;

public final class FindVeinsCommand
{
    private static final String TP_MESSAGE = "{\"text\":\"" + ChatFormatting.BLUE + "[Click to Teleport]" + ChatFormatting.RESET + "\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/tp %d %d %d\"}}";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(
            Commands.literal("findveins").requires(source -> source.hasPermission(2))
                .then(Commands.argument("type", new VeinTypeArgument())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(VeinManager.INSTANCE.getKeys(), builder))
                    .then(Commands.argument("radius", IntegerArgumentType.integer(0, 250))
                        .executes(cmd -> findVeins(cmd.getSource(), VeinTypeArgument.getVein(cmd, "type"), IntegerArgumentType.getInteger(cmd, "radius")))
                    )
                )
        );
    }

    private static int findVeins(CommandSourceStack source, ResourceLocation veinName, int radius) throws CommandSyntaxException
    {
        final BlockPos pos = BlockPos.containing(source.getPosition());
        final int chunkX = pos.getX() >> 4, chunkZ = pos.getZ() >> 4;
        final List<Vein<?>> veins = VeinsFeature.getNearbyVeins(chunkX, chunkZ, source.getLevel().getSeed(), radius);
        final VeinType<?> type = VeinManager.INSTANCE.getVein(veinName);
        if (type == null)
        {
            source.sendFailure(Component.translatable(MOD_ID + ".command.unknown_vein", veinName.toString()));
        }

        // Search for veins matching type
        //noinspection EqualsBetweenInconvertibleTypes
        veins.removeIf(x -> !x.getType().equals(type));
        source.sendSuccess(() -> Component.translatable(MOD_ID + ".command.veins_found"), true);
        for (Vein<?> vein : veins)
        {
            MutableComponent resultText = Component.translatable(MOD_ID + ".command.vein_info", vein.toString());
            if (source.getEntity() instanceof ServerPlayer player)
            {
                BlockPos veinPos = vein.getPos();
                try
                {
                    Component tpText = Component.Serializer.fromJson(String.format(TP_MESSAGE, veinPos.getX(), veinPos.getY(), veinPos.getZ()));
                    if (tpText != null)
                    {
                        MutableComponent message = resultText.copy().append(tpText);
                        player.sendSystemMessage(ComponentUtils.updateForEntity(source, message, player, 0));
                    }
                }
                catch (JsonParseException e) { /* Ignore, it shouldn't happen */ }
            }
        }
        return 1;
    }
}
