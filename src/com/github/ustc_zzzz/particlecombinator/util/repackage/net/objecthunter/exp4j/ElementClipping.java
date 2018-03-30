package com.github.ustc_zzzz.particlecombinator.util.repackage.net.objecthunter.exp4j;

import com.flowpowered.math.GenericMath;
import com.github.ustc_zzzz.particlecombinator.util.Element;

import java.util.Collections;
import java.util.Random;

/**
 * @author ustc_zzzz
 */
public class ElementClipping implements Element
{
    private final Element child;
    private final float lowerBound;
    private final float upperBound;

    public ElementClipping(Element child, float lowerBound, float upperBound)
    {
        this.child = child;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    @Override
    public Iterable<Particle> getParticles(float interpolation, Random random)
    {
        int lowerDiff = GenericMath.floor(interpolation - this.lowerBound);
        int upperDiff = GenericMath.floor(this.upperBound - interpolation);
        return lowerDiff + upperDiff >= 0 ? this.child.getParticles(interpolation, random) : Collections.emptyList();
    }
}
