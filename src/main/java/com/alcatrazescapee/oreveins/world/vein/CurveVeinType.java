/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.oreveins.world.vein;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.BlockPos;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import static com.alcatrazescapee.oreveins.world.vein.CurveVeinType.VeinCurve;

public class CurveVeinType extends SingleVeinType<VeinCurve>
{
    private final float radius;
    private final float angle;

    public CurveVeinType(JsonObject obj, JsonDeserializationContext context) throws JsonParseException
    {
        super(obj, context);
        radius = GsonHelper.getAsFloat(obj, "radius", 5);
        if (radius <= 0)
        {
            throw new JsonParseException("Radius must be > 0");
        }
        angle = GsonHelper.getAsFloat(obj, "angle", 45f);
        if (angle < 0 || angle > 360)
        {
            throw new JsonParseException("Angle must be >= 0 and <= 360");
        }
    }

    @Override
    public boolean inRange(VeinCurve vein, int xOffset, int zOffset)
    {
        return (xOffset < horizontalSize) && (zOffset < horizontalSize);
    }

    @Override
    public float getChanceToGenerate(VeinCurve vein, BlockPos pos)
    {
        for (CurveSegment segment : vein.getSegmentList())
        {
            Vec3 blockPos = new Vec3(pos.getX(), pos.getY(), pos.getZ());
            Vec3 centeredPos = blockPos.subtract(segment.begin);

            // rotate block pos around Y axis
            double yaw = segment.yaw;
            Vec3 posX = new Vec3(Math.cos(yaw) * centeredPos.x + Math.sin(yaw) * centeredPos.z, centeredPos.y, -Math.sin(yaw) * centeredPos.x + Math.cos(yaw) * centeredPos.z);

            // rotate block pos around Z axis
            double pitch = segment.pitch;
            Vec3 posY = new Vec3(Math.cos(pitch) * posX.x - Math.sin(pitch) * posX.y, Math.sin(pitch) * posX.x + Math.cos(pitch) * posX.y, posX.z);

            double rad = Math.sqrt(posY.x * posY.x + posY.z * posY.z);
            double length = segment.length;

            if (((posY.y >= 0 && posY.y <= length) || (posY.y < 0 && posY.y >= length)) && rad < this.radius)
            {
                return 0.005f * density * (1f - 0.9f * (float) rad / this.radius);
            }
        }
        return 0.0f;
    }

    @Override
    public VeinCurve createVein(int chunkX, int chunkZ, RandomSource random)
    {
        int maxOffY = getMaxY() - getMinY() - verticalSize;
        int posY = getMinY() + verticalSize / 2 + ((maxOffY > 0) ? random.nextInt(maxOffY) : 0);
        BlockPos pos = new BlockPos(chunkX * 16 + random.nextInt(16), posY, chunkZ * 16 + random.nextInt(16));
        return new VeinCurve(this, pos, random);
    }

    static class VeinCurve extends Vein<CurveVeinType>
    {
        private final RandomSource rand;
        private final List<CurveSegment> segmentList;
        private boolean isInitialized = false;

        VeinCurve(CurveVeinType type, BlockPos pos, RandomSource random)
        {
            super(type, pos);
            this.rand = RandomSource.create(random.nextLong());
            this.segmentList = new ArrayList<>();
        }

        @Override
        public boolean inRange(int x, int z)
        {
            return getType().inRange(this, getPos().getX() - x, getPos().getZ() - z);
        }

        @Override
        public float getChanceToGenerate(BlockPos pos)
        {
            if (!isInitialized)
            {
                initialize(getType().horizontalSize, getType().verticalSize, getType().angle);
            }
            return getType().getChanceToGenerate(this, pos);
        }

        List<CurveSegment> getSegmentList()
        {
            return segmentList;
        }

        private Vec3 getRandomPointInCuboid(RandomSource rand, Vec3 bottomLeft, Vec3 topRight)
        {
            final double x = bottomLeft.x + (topRight.x - bottomLeft.x) * rand.nextDouble();
            final double y = bottomLeft.y + (topRight.y - bottomLeft.y) * rand.nextDouble();
            final double z = bottomLeft.z + (topRight.z - bottomLeft.z) * rand.nextDouble();

            return new Vec3(x, y, z);
        }

        private void initialize(int hSize, int vSize, float angle)
        {
            double kxy = Math.tan(angle * (1.0f - 2.0f * rand.nextFloat()));
            double kyz = Math.tan(angle * (1.0f - 2.0f * rand.nextFloat()));

            final double h2Size = hSize / 2d;
            final double v2Size = vSize / 2d;

            // four points for cubic Bezier curve
            // p1 and p4 placed on (hSize; hSize; vSize) box with center in vein position
            Vec3 p1, p2, p3, p4;
            double x1, y1, z1, x2, y2, z2;

            if (v2Size >= h2Size * Math.abs(kyz))
            {
                z1 = -h2Size;
                y1 = h2Size * kyz;
            }
            else
            {
                z1 = -v2Size * Math.abs(kyz);
                y1 = v2Size * Math.signum(kyz);
            }

            x1 = (1 >= Math.abs(kxy)) ? h2Size : h2Size * kxy;

            x2 = -x1;
            y2 = -y1;
            z2 = -z1;

            p1 = new Vec3(x1 + getPos().getX(), y1 + getPos().getY(), z1 + getPos().getZ());
            p4 = new Vec3(x2 + getPos().getX(), y2 + getPos().getY(), z2 + getPos().getZ());

            Vec3 bottomLeft = new Vec3(Math.min(p1.x, p4.x), Math.min(p1.y, p4.y), Math.min(p1.z, p4.z));
            Vec3 topRight = new Vec3(Math.max(p1.x, p4.x), Math.max(p1.y, p4.y), Math.max(p1.z, p4.z));

            p2 = getRandomPointInCuboid(rand, bottomLeft, topRight);
            p3 = getRandomPointInCuboid(rand, bottomLeft, topRight);

            // curve segmentation setup
            double step = 5.0 / h2Size;
            double t = 0.0;
            Vec3 pb;
            Vec3 pe = Vec3.ZERO;

            // curve segmentation
            while (t < 1.0)
            {
                pb = (t == 0.0) ? p1 : pe;

                t += step;
                if (t > 1.0)
                {
                    t = 1.0;
                }

                double t11 = 1 - t;
                double t12 = t11 * t11;
                double t13 = t12 * t11;

                double t31 = 3 * t;
                double t32 = 3 * t * t;
                double t3 = t * t * t;

                pe = p1.scale(t13).add(p2.scale(t31 * t12)).add(p3.scale(t32 * t11)).add(p4.scale(t3));

                Vec3 axis = pe.subtract(pb);

                // align segment axis with axis X
                double yaw = Math.atan2(axis.z, axis.x);
                Vec3 axisX = new Vec3(Math.cos(yaw) * axis.x + Math.sin(yaw) * axis.z,
                    axis.y,
                    -Math.sin(yaw) * axis.x + Math.cos(yaw) * axis.z);

                // align segment axis with axis Y
                double pitch = Math.atan2(axisX.x, axisX.y);
                Vec3 axisY = new Vec3(Math.cos(pitch) * axisX.x - Math.sin(pitch) * axisX.y,
                    Math.sin(pitch) * axisX.x + Math.cos(pitch) * axisX.y,
                    axisX.z);

                double length = axisY.y;

                segmentList.add(new CurveSegment(pb, yaw, pitch, length));
            }

            isInitialized = true;
        }
    }

    static class CurveSegment
    {
        final Vec3 begin;
        final double yaw;
        final double pitch;
        final double length;

        CurveSegment(Vec3 begin, double yaw, double pitch, double length)
        {
            this.begin = begin;
            this.yaw = yaw;
            this.pitch = pitch;
            this.length = length;
        }
    }
}
