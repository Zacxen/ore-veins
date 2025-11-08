/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.oreveins.world.vein;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.crafting.CraftingHelper;

import com.alcatrazescapee.oreveins.command.ClearWorldCommand;
import com.alcatrazescapee.oreveins.util.collections.IWeightedList;
import com.alcatrazescapee.oreveins.util.json.BlockStateDeserializer;
import com.alcatrazescapee.oreveins.util.json.BlockStatePredicateDeserializer;
import com.alcatrazescapee.oreveins.util.json.VeinTypeDeserializer;
import com.alcatrazescapee.oreveins.util.json.WeightedListDeserializer;
import com.alcatrazescapee.oreveins.world.VeinsFeature;
import com.alcatrazescapee.oreveins.world.rule.DistanceRule;
import com.alcatrazescapee.oreveins.world.rule.IBiomeRule;
import com.alcatrazescapee.oreveins.world.rule.IDimensionRule;
import com.alcatrazescapee.oreveins.world.rule.IRule;

public class VeinManager extends SimpleJsonResourceReloadListener
{
    public static final VeinManager INSTANCE;

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder()
        // Collections
        .registerTypeAdapter(new TypeToken<IWeightedList<BlockState>>() {}.getType(), new WeightedListDeserializer<>(BlockState.class))
        .registerTypeAdapter(new TypeToken<IWeightedList<Indicator>>() {}.getType(), new WeightedListDeserializer<>(Indicator.class))
        .registerTypeAdapter(new TypeToken<Predicate<BlockState>>() {}.getType(), BlockStatePredicateDeserializer.INSTANCE)
        .registerTypeAdapter(BlockState.class, BlockStateDeserializer.INSTANCE)
        .registerTypeAdapter(IRule.class, IRule.Deserializer.INSTANCE)
        .registerTypeAdapter(Indicator.class, Indicator.Deserializer.INSTANCE)
        .registerTypeAdapter(IBiomeRule.class, IBiomeRule.Deserializer.INSTANCE)
        .registerTypeAdapter(IDimensionRule.class, IDimensionRule.Deserializer.INSTANCE)
            .registerTypeAdapter(DistanceRule.class, DistanceRule.Deserializer.INSTANCE)
            .registerTypeAdapter(VeinType.class, VeinTypeDeserializer.INSTANCE)
            .disableHtmlEscaping()
            .create();

    static
    {
        // Constructor call must come after GSON declaration
        INSTANCE = new VeinManager();
    }

    private final BiMap<ResourceLocation, VeinType<?>> veins;

    private VeinManager()
    {
        super(GSON, "oreveins");
        this.veins = HashBiMap.create();
    }

    public Collection<VeinType<?>> getVeins()
    {
        return veins.values();
    }

    public Set<ResourceLocation> getKeys()
    {
        return veins.keySet();
    }

    @Nullable
    public VeinType<?> getVein(ResourceLocation key)
    {
        return veins.get(key);
    }

    public ResourceLocation getName(VeinType<?> key)
    {
        return veins.inverse().get(key);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler)
    {
        veins.clear();
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet())
        {
            ResourceLocation name = entry.getKey();
            JsonElement element = entry.getValue();
            if (!element.isJsonObject())
            {
                LOGGER.warn("Skipping loading vein '{}' because its data was not a JSON object", name);
                continue;
            }

            JsonObject json = element.getAsJsonObject();
            try
            {
                if (CraftingHelper.processConditions(json, "conditions"))
                {
                    veins.put(name, GSON.fromJson(json, VeinType.class));
                }
                else
                {
                    LOGGER.info("Skipping loading vein '{}' as it's conditions were not met", name);
                }
            }
            catch (IllegalArgumentException | JsonParseException e)
            {
                LOGGER.warn("Vein '{}' failed to parse. This is most likely caused by incorrectly specified JSON.", entry.getKey());
                LOGGER.warn("Error: ", e);
            }
        }

        LOGGER.info("Registered {} Veins Successfully.", veins.size());

        // After Veins have Reloaded
        ClearWorldCommand.resetVeinStates();
        VeinsFeature.resetChunkRadius();
    }
}
