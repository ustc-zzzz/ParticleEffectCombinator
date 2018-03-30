package com.github.ustc_zzzz.particlecombinator.util;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.TrigMath;
import com.flowpowered.math.matrix.Matrix3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author ustc_zzzz
 */
public class ElementSphere implements Element
{
    private final float radius;
    private final Element child;
    private final int sampleNumber;

    private static final double GOLDEN_RATIO_WITH_TWO_PI = TrigMath.PI * (Math.sqrt(5) - 1);

    public ElementSphere(Element child, float radius, float sampleNumber)
    {
        this.child = child;
        this.radius = radius;
        sampleNumber += GenericMath.FLT_EPSILON;
        this.sampleNumber = GenericMath.floor(radius * radius * TrigMath.TWO_PI * 2 * sampleNumber * sampleNumber);
    }

    @Override
    public List<Particle> getParticles(float interpolation, Random random)
    {
        ArrayList<Particle> result = new ArrayList<>();
        for (int i = 0; i < this.sampleNumber; ++i)
        {
            double y = 1 - (2 * i + 1) / (double) this.sampleNumber;
            double polar = -TrigMath.acos(y), yaw = GOLDEN_RATIO_WITH_TWO_PI * i;
            Matrix3d rotationMatrix = RotationUtil.getRotationMatrix(yaw, polar);
            for (Particle p : this.child.getParticles((float) ((y + 1) / 2), random))
            {
                Particle newParticle = new Particle(p);
                newParticle.coordinate = rotationMatrix.transform(p.coordinate.add(0, this.radius, 0));
                newParticle.orientation = rotationMatrix.transform(p.orientation.toDouble()).toFloat();
                result.add(newParticle);
            }
        }
        return result;
    }
}
