/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.oreveins.world.vein;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.BlockPos;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;

public class ConeVeinType extends SingleVeinType<Vein<?>>
{
    private final float shape;
    private final boolean inverted;

    public ConeVeinType(JsonObject obj, JsonDeserializationContext context) throws JsonParseException
    {
        super(obj, context);
        shape = GsonHelper.getAsFloat(obj, "shape", 0.5f);
        inverted = GsonHelper.getAsBoolean(obj, "inverted", false);
    }

    @Override
    public Vein<?> createVein(int chunkX, int chunkZ, RandomSource random)
    {
        return createDefaultVein(chunkX, chunkZ, random);
    }

    @Override
    public float getChanceToGenerate(Vein<?> vein, BlockPos pos)
    {
        final double dx = Math.pow(vein.getPos().getX() - pos.getX(), 2);
        final double dz = Math.pow(vein.getPos().getZ() - pos.getZ(), 2);

        float dy = 0.5f + (pos.getY() - vein.getPos().getY()) / (verticalSize * 2f); // 0 at bottom, 1.0 at top
        if (inverted)
        {
            dy = 1f - dy;
        }
        if (dy > 1f || dy < 0f)
        {
            return 0;
        }

        final float maxR = (1f - shape * dy) * horizontalSize;
        return 0.005f * density * (1.0f - (float) (dx + dz) / (maxR * maxR)); // Otherwise calculate from radius
    }
}
