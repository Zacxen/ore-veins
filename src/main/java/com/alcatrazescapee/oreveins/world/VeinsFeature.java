/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.oreveins.world;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.Level;

import com.alcatrazescapee.oreveins.Config;
import com.alcatrazescapee.oreveins.world.vein.Indicator;
import com.alcatrazescapee.oreveins.world.vein.Vein;
import com.alcatrazescapee.oreveins.world.vein.VeinManager;
import com.alcatrazescapee.oreveins.world.vein.VeinType;

import com.mojang.serialization.Codec;

import static net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG;
import static net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG;

public class VeinsFeature extends Feature<NoneFeatureConfiguration>
{
    private static int CHUNK_RADIUS = 0;

    public static void resetChunkRadius()
    {
        CHUNK_RADIUS = 1 + VeinManager.INSTANCE.getVeins().stream().mapToInt(VeinType::getChunkRadius).max().orElse(0) + Config.COMMON.extraChunkRange.get();
    }

    public static List<Vein<?>> getNearbyVeins(int chunkX, int chunkZ, long worldSeed, int radius)
    {
        List<Vein<?>> veins = new ArrayList<>();
        for (int x = chunkX - radius; x <= chunkX + radius; x++)
        {
            for (int z = chunkZ - radius; z <= chunkZ + radius; z++)
            {
                getVeinsAtChunk(veins, x, z, worldSeed);
            }
        }
        return veins;
    }

    private static void getVeinsAtChunk(List<Vein<?>> veins, int chunkX, int chunkZ, long worldSeed)
    {
        RandomSource random = RandomSource.create(worldSeed + chunkX * 341873128712L + chunkZ * 132897987541L);
        for (VeinType<?> type : VeinManager.INSTANCE.getVeins())
        {
            for (int i = 0; i < type.getCount(); i++)
            {
                if (random.nextInt(type.getRarity()) == 0)
                {
                    type.createVeins(veins, chunkX, chunkZ, random);
                }
            }
        }
    }

    public VeinsFeature()
    {
        super(Codec.unit(NoneFeatureConfiguration.INSTANCE));
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context)
    {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        ServerLevel serverLevel = level.getLevel();
        Holder<DimensionType> dimensionType = serverLevel.dimensionTypeRegistration();
        ResourceKey<Level> dimensionKey = serverLevel.dimension();

        List<Vein<?>> veins = getNearbyVeins(origin.getX() >> 4, origin.getZ() >> 4, level.getSeed(), CHUNK_RADIUS)
            .stream()
            .filter(vein -> vein.getType().matchesDimension(dimensionType, dimensionKey))
            .collect(Collectors.toList());

        MutableBlockPos mutablePos = new MutableBlockPos();
        MutableBlockPos surfacePos = new MutableBlockPos();

        for (int x = origin.getX(); x < origin.getX() + 16; x++)
        {
            for (int z = origin.getZ(); z < origin.getZ() + 16; z++)
            {
                mutablePos.set(x, level.getMinBuildHeight(), z);
                Holder<Biome> biomeHolder = level.getBiome(mutablePos);

                for (Vein<?> vein : veins)
                {
                    if (vein.getType().matchesBiome(biomeHolder) && vein.inRange(x, z))
                    {
                        Indicator veinIndicator = vein.getType().getIndicator(random);
                        boolean canGenerateIndicator = false;

                        for (int y = vein.getType().getMinY(); y <= vein.getType().getMaxY(); y++)
                        {
                            mutablePos.set(x, y, z);
                            if (random.nextFloat() < vein.getChanceToGenerate(mutablePos))
                            {
                                if (vein.getType().canGenerateAt(level, mutablePos))
                                {
                                    BlockState oreState = vein.getStateToGenerate(mutablePos, random);
                                    setBlock(level, mutablePos, oreState);
                                    if (veinIndicator != null && !canGenerateIndicator)
                                    {
                                        Heightmap.Types heightmap = veinIndicator.shouldIgnoreLiquids() ? OCEAN_FLOOR_WG : WORLD_SURFACE_WG;
                                        int depth = level.getHeight(heightmap, x, z) - y;
                                        if (depth < 0)
                                        {
                                            depth = -depth;
                                        }
                                        canGenerateIndicator = depth < veinIndicator.getMaxDepth();
                                    }
                                }
                            }
                        }

                        if (veinIndicator != null && canGenerateIndicator)
                        {
                            if (random.nextInt(veinIndicator.getRarity()) == 0)
                            {
                                Heightmap.Types heightmap = veinIndicator.shouldIgnoreLiquids() ? OCEAN_FLOOR_WG : WORLD_SURFACE_WG;
                                surfacePos.set(x, 0, z);
                                BlockPos topPos = level.getHeightmapPos(heightmap, surfacePos);
                                surfacePos.set(topPos);

                                BlockState indicatorState = veinIndicator.getStateToGenerate(random);

                                if (veinIndicator.shouldReplaceSurface())
                                {
                                    surfacePos.move(0, -1, 0);
                                }
                                BlockPos belowPos = surfacePos.below();
                                if (indicatorState.canSurvive(level, surfacePos) && (veinIndicator.shouldIgnoreLiquids() || level.getFluidState(surfacePos).isEmpty()) && veinIndicator.validUnderState(level.getBlockState(belowPos)))
                                {
                                    setBlock(level, surfacePos, indicatorState);
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
}
