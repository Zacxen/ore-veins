/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.oreveins.world.rule;

import java.util.function.Predicate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.BiomeDictionary;

import com.alcatrazescapee.oreveins.util.json.PredicateDeserializer;

@FunctionalInterface
public interface IBiomeRule extends Predicate<Holder<Biome>>
{
    IBiomeRule DEFAULT = biome -> true;

    class Deserializer extends PredicateDeserializer<Holder<Biome>, IBiomeRule>
    {
        public static final Deserializer INSTANCE = new Deserializer();

        private Deserializer()
        {
            super(IBiomeRule.class, "biomes");
        }

        @Override
        protected IBiomeRule createSingleRule(JsonObject json, String typeName)
        {
            if ("tag".equals(typeName))
            {
                final TagKey<Biome> tag = TagKey.create(Registries.BIOME, new ResourceLocation(GsonHelper.getAsString(json, "biomes")));
                return holder -> holder.is(tag);
            }
            else if ("dictionary".equals(typeName))
            {
                final BiomeDictionary.Type type = BiomeDictionary.Type.getType(GsonHelper.getAsString(json, "biomes"));
                return holder -> holder.unwrapKey().map(key -> BiomeDictionary.hasType(key, type)).orElse(false);
            }
            else if ("biome".equals(typeName))
            {
                return createSingleRule(json.get("biomes").getAsString());
            }
            else
            {
                throw new JsonParseException("Type must be logical (and, or, not), or (biome, tag, dictionary)");
            }
        }

        @Override
        protected IBiomeRule createSingleRule(String name) throws JsonParseException
        {
            final ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, new ResourceLocation(name));
            return holder -> holder.is(key);
        }

        @Override
        protected IBiomeRule createPredicate(Predicate<Holder<Biome>> predicate)
        {
            return predicate::test;
        }
    }
}
