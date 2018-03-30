package com.github.ustc_zzzz.particlecombinator.util;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.TrigMath;
import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author ustc_zzzz
 */
public class ElementCircle implements Element
{
    private final float radius;
    private final Element child;
    private final int sampleNumber;

    public ElementCircle(Element child, float radius, float sampleNumber)
    {
        this.child = child;
        this.radius = radius;
        this.sampleNumber = GenericMath.floor(radius * TrigMath.TWO_PI * (sampleNumber + GenericMath.FLT_EPSILON));
    }

    @Override
    public List<Particle> getParticles(float interpolation, Random random)
    {
        ArrayList<Particle> result = new ArrayList<>();
        for (int i = 0; i < this.sampleNumber; ++i)
        {
            double factor = i / (double) this.sampleNumber;
            double rotation = factor * TrigMath.TWO_PI, cos = TrigMath.cos(rotation), sin = TrigMath.sin(rotation);
            for (Particle p : this.child.getParticles((float) factor, random))
            {
                Particle newParticle = new Particle(p);
                double x = p.coordinate.getX(), y = p.coordinate.getY(), z = p.coordinate.getZ() + this.radius;
                newParticle.coordinate = Vector3d.from(x * cos + z * sin, y, z * cos - x * sin);
                float vx = p.orientation.getX(), vy = p.orientation.getY(), vz = p.orientation.getZ();
                newParticle.orientation = Vector3d.from(vx * cos + vz * sin, vy, vz * cos - vx * sin).toFloat();
                result.add(newParticle);
            }
        }
        return result;
    }
}
