package com.github.ustc_zzzz.pec.util;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author ustc_zzzz
 */
public class ElementLine implements Element
{
    private final Vector3d to;
    private final Vector3d from;
    private final Element child;
    private final float samplingPhase;
    private final double samplingNumber;

    public ElementLine(Vector3d to, Vector3d from, Element child, float sampleNumber, float samplingPhase)
    {
        this.to = to;
        this.from = from;
        this.child = child;
        this.samplingPhase = samplingPhase - GenericMath.floor(samplingPhase);
        this.samplingNumber = GenericMath.sqrt(to.distanceSquared(from)) * (sampleNumber + GenericMath.FLT_EPSILON);
    }

    @Override
    public List<Particle> getParticles(float interpolation, Random random)
    {
        ArrayList<Particle> result = new ArrayList<>();
        for (int i = 0; i < this.samplingNumber; ++i)
        {
            double factor = (i + this.samplingPhase) / this.samplingNumber;
            Vector3d diffVector = GenericMath.lerp(this.from, this.to, factor);
            for (Particle p : this.child.getParticles((float) factor, random))
            {
                Particle newParticle = new Particle(p);
                newParticle.coordinate = p.coordinate.add(diffVector);
                result.add(newParticle);
            }
        }
        return result;
    }
}
