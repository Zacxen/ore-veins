/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.oreveins.util.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;
import javax.annotation.Nonnull;

import net.minecraft.util.RandomSource;

public class WeightedList<E> implements IWeightedList<E>
{
    private final NavigableMap<Double, E> map;
    private double totalWeight;

    public WeightedList()
    {
        this.totalWeight = 0;
        this.map = new TreeMap<>();
    }

    public void add(double weight, E element)
    {
        if (weight > 0)
        {
            totalWeight += weight;
            map.put(totalWeight, element);
        }
    }

    public E get(RandomSource random)
    {
        double value = random.nextDouble() * totalWeight;
        return map.higherEntry(value).getValue();
    }

    @Override
    public Collection<E> values()
    {
        return map.values();
    }

    @Override
    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    @Override
    public String toString()
    {
        return map.toString();
    }

    @Nonnull
    @Override
    public Iterator<E> iterator()
    {
        return map.values().iterator();
    }
}
