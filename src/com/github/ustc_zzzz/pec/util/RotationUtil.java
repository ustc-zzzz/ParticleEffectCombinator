package com.github.ustc_zzzz.pec.util;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.TrigMath;
import com.flowpowered.math.matrix.Matrix3d;
import com.flowpowered.math.vector.Vector3d;

/**
 * @author ustc_zzzz
 */
public final class RotationUtil
{
    public static Matrix3d getRotationMatrix(double yaw, double polar)
    {
        yaw = yaw < 0 ? yaw % TrigMath.TWO_PI + TrigMath.TWO_PI : yaw % TrigMath.TWO_PI;
        polar = polar < 0 ? polar % TrigMath.TWO_PI + TrigMath.TWO_PI : polar % TrigMath.TWO_PI;

        float yCos = TrigMath.cos(yaw), ySin = TrigMath.sin(yaw);
        float pCos = TrigMath.cos(polar), pSin = TrigMath.sin(polar);

        Matrix3d yMatrix = Matrix3d.from(yCos, 0, ySin, 0, 1, 0, -ySin, 0, yCos);
        Matrix3d pMatrix = Matrix3d.from(1, 0, 0, 0, pCos, pSin, 0, -pSin, pCos);

        return yMatrix.mul(pMatrix);
    }

    public static Matrix3d getRotationMatrix(Vector3d rotationAxis, double rotation)
    {
        rotationAxis = GenericMath.normalizeSafe(rotationAxis);
        rotation = rotation < 0 ? rotation % TrigMath.TWO_PI + TrigMath.TWO_PI : rotation % TrigMath.TWO_PI;

        double cos = TrigMath.cos(rotation), sin = TrigMath.sin(rotation);
        double rx = rotationAxis.getX(), ry = rotationAxis.getY(), rz = rotationAxis.getZ();

        Matrix3d crossProduct = Matrix3d.from(0, -rz, ry, rz, 0, -rx, -ry, rx, 0);

        return Matrix3d.IDENTITY.add(crossProduct.mul(sin)).add(crossProduct.mul(crossProduct).mul(1 - cos));
    }

    private RotationUtil()
    {
        // nothing
    }
}
