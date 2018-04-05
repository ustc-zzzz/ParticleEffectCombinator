package com.github.ustc_zzzz.pec.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.ToDoubleFunction;

/**
 * @author ustc_zzzz
 */
public class ElementOffsetExpression implements Element
{
    private final Element child;
    private final ToDoubleFunction<Float> offsetX;
    private final ToDoubleFunction<Float> offsetY;
    private final ToDoubleFunction<Float> offsetZ;
    private final ToDoubleFunction<Float> offsetT;

    public ElementOffsetExpression(Element child, ToDoubleFunction<Float> offsetX, ToDoubleFunction<Float> offsetY, ToDoubleFunction<Float> offsetZ, ToDoubleFunction<Float> offsetT)
    {
        this.child = child;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.offsetT = offsetT;
    }

    @Override
    public List<Particle> getParticles(float interpolation, Random random)
    {
        ArrayList<Particle> result = new ArrayList<>();
        double interpolationX = this.offsetX.applyAsDouble(interpolation);
        double interpolationY = this.offsetY.applyAsDouble(interpolation);
        double interpolationZ = this.offsetZ.applyAsDouble(interpolation);
        double interpolationT = this.offsetT.applyAsDouble(interpolation);
        for (Particle p : this.child.getParticles(interpolation, random))
        {
            Particle newParticle = new Particle(p);
            newParticle.delayTicks = Math.toIntExact(p.delayTicks + Math.round(interpolationT));
            newParticle.coordinate = p.coordinate.add(interpolationX, interpolationY, interpolationZ);
            result.add(newParticle);
        }
        return result;
    }
}
