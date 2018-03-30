package com.github.ustc_zzzz.particlecombinator.util;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author ustc_zzzz
 */
public class ElementOffset implements Element
{
    private final Element child;
    private final Vector3d orientation;
    private final InterpolationMap offsetMap;

    public ElementOffset(Element child, Vector3d orientation, Map<Float, Double> offsetMap)
    {
        this.child = child;
        this.offsetMap = new InterpolationMap(offsetMap);
        this.orientation = GenericMath.normalizeSafe(orientation);
    }

    @Override
    public List<Particle> getParticles(float interpolation, Random random)
    {
        ArrayList<Particle> result = new ArrayList<>();
        double interpolationValue = this.offsetMap.applyAsDouble(interpolation);
        for (Particle p : this.child.getParticles(interpolation, random))
        {
            Particle newParticle = new Particle(p);
            newParticle.coordinate = p.coordinate.add(this.orientation.mul(interpolationValue));
            result.add(newParticle);
        }
        return result;
    }
}
