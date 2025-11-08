/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.oreveins.util.json;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public enum BlockStatePredicateDeserializer implements JsonDeserializer<Predicate<BlockState>>
{
    INSTANCE;

    private static final HolderLookup.RegistryLookup<Block> BLOCKS = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).lookupOrThrow(Registries.BLOCK);

    @Override
    public Predicate<BlockState> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
        if (json.isJsonArray())
        {
            List<Predicate<BlockState>> subPredicates = new ArrayList<>();
            for (JsonElement subElement : json.getAsJsonArray())
            {
                subPredicates.add(context.deserialize(subElement, new TypeToken<Predicate<BlockState>>() {}.getType()));
            }
            return stateIn -> subPredicates.stream().anyMatch(predicate -> predicate.test(stateIn));
        }
        else if (json.isJsonObject())
        {
            JsonObject obj = json.getAsJsonObject();
            return parsePredicate(obj.get("block").getAsString());
        }
        else
        {
            return parsePredicate(json.getAsString());
        }
    }

    public Predicate<BlockState> parsePredicate(String value)
    {
        if (value.startsWith("#"))
        {
            String tagName = value.substring(1);
            TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, ResourceLocation.parse(tagName));
            if (BLOCKS.get(tagKey).isPresent())
            {
                return stateIn -> stateIn.is(tagKey);
            }
            throw new JsonParseException("Unknown tag: " + tagName);
        }

        ResourceLocation id = ResourceLocation.parse(value);
        return BLOCKS.get(ResourceKey.create(Registries.BLOCK, id))
            .map(Holder::value)
            .map(block -> (Predicate<BlockState>) stateIn -> stateIn.getBlock() == block)
            .orElseThrow(() -> new JsonParseException("Unknown block: " + value));
    }
}
