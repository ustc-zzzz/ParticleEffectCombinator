package com.github.ustc_zzzz.particlecombinator.util;

import com.github.ustc_zzzz.particlecombinator.util.Element;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @author ustc_zzzz
 */
public class ElementConcatenation implements Element
{
    private final List<Element> elements;

    public ElementConcatenation(List<Element> elements)
    {
        this.elements = ImmutableList.copyOf(elements);
    }

    @Override
    public Iterable<Particle> getParticles(float interpolation, Random random)
    {
        return Iterables.concat(this.getParticleLists(interpolation, random));
    }

    private List<Iterable<Particle>> getParticleLists(float interpolation, Random random)
    {
        return this.elements.stream().map(e -> e.getParticles(interpolation, random)).collect(Collectors.toList());
    }
}
