package com.github.ustc_zzzz.particlecombinator.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author ustc_zzzz
 */
public class ElementTimeOffset implements Element
{
    private final Element child;
    private final InterpolationMap offsetMap;

    public ElementTimeOffset(Element child, Map<Float, Double> offsetMap)
    {
        this.child = child;
        this.offsetMap = new InterpolationMap(offsetMap);
    }

    @Override
    public List<Particle> getParticles(float interpolation, Random random)
    {
        ArrayList<Particle> result = new ArrayList<>();
        double interpolationValue = this.offsetMap.applyAsDouble(interpolation);
        for (Particle p : this.child.getParticles(interpolation, random))
        {
            Particle newParticle = new Particle(p);
            newParticle.delayTicks = Math.toIntExact(p.delayTicks + Math.round(interpolationValue));
            result.add(newParticle);
        }
        return result;
    }
}
