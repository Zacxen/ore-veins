/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.oreveins.util.json;

import java.lang.reflect.Type;

import com.google.gson.*;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.neoforged.neoforge.registries.ForgeRegistries;

public enum BlockStateDeserializer implements JsonDeserializer<BlockState>
{
    INSTANCE;

    @Override
    public BlockState deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
        if (json.isJsonPrimitive())
        {
            return readBlockState(json.getAsString());
        }
        else if (json.isJsonObject())
        {
            JsonObject jsonObj = json.getAsJsonObject();
            String name = jsonObj.get("block").getAsString();
            return readBlockState(name);
        }
        throw new JsonParseException("BlockState must be String or Object");
    }

    public BlockState readBlockState(String block)
    {
        return parseBlockState(block).blockState();
    }

    public BlockStateParser.BlockResult parseBlockState(String block)
    {
        StringReader reader = new StringReader(block);
        try
        {
            return BlockStateParser.parseForBlock(ForgeRegistries.BLOCKS.getHolderLookup(), reader, true);
        }
        catch (CommandSyntaxException e)
        {
            throw new JsonParseException("Unable to parse block state: " + block, e);
        }
    }

    public boolean isBlockState(String block)
    {
        try
        {
            BlockStateParser.parseForBlock(ForgeRegistries.BLOCKS.getHolderLookup(), new StringReader(block), true);
            return true;
        }
        catch (CommandSyntaxException e)
        {
            return false;
        }
    }
}
