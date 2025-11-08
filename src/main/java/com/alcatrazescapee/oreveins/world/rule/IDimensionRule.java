/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.oreveins.world.rule;

import java.util.function.Predicate;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

import com.alcatrazescapee.oreveins.util.json.PredicateDeserializer;

@FunctionalInterface
public interface IDimensionRule extends Predicate<IDimensionRule.Context>
{
    IDimensionRule DEFAULT = context -> context.dimensionKey().equals(Level.OVERWORLD);

    record Context(Holder<DimensionType> dimensionType, ResourceKey<Level> dimensionKey) {}

    default boolean test(Holder<DimensionType> dimensionType, ResourceKey<Level> levelKey)
    {
        return test(new Context(dimensionType, levelKey));
    }

    class Deserializer extends PredicateDeserializer<Context, IDimensionRule>
    {
        public static final Deserializer INSTANCE = new Deserializer();

        private Deserializer()
        {
            super(IDimensionRule.class, "dimensions");
        }

        @Override
        protected IDimensionRule createSingleRule(String name)
        {
            final ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(name));
            return context -> context.dimensionKey().equals(levelKey);
        }

        @Override
        protected IDimensionRule createPredicate(Predicate<Context> predicate)
        {
            return predicate::test;
        }
    }
}
