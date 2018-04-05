package com.github.ustc_zzzz.pec.util;

import com.flowpowered.math.GenericMath;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;

/**
 * @author ustc_zzzz
 */
public final class InterpolationMap implements ToDoubleFunction<Float>
{
    private final TreeMap<Float, Double> offsetMap;

    public InterpolationMap(Map<Float, Double> map)
    {
        this.offsetMap = new TreeMap<>();
        for (Map.Entry<Float, Double> entry : map.entrySet())
        {
            float key = entry.getKey();
            if (key > 1) key -= (Math.ceil(key) - 1);
            if (key < 0) key += GenericMath.floor(key);
            this.offsetMap.put(key, entry.getValue());
        }
        if (!this.offsetMap.isEmpty())
        {
            Map.Entry<Float, Double> lastEntry = this.offsetMap.lastEntry();
            Map.Entry<Float, Double> firstEntry = this.offsetMap.firstEntry();
            if (firstEntry.getKey() > 0) this.offsetMap.put(lastEntry.getKey() - 1, lastEntry.getValue());
            if (lastEntry.getKey() < 1) this.offsetMap.put(firstEntry.getKey() + 1, firstEntry.getValue());
        }
    }

    @Override
    public double applyAsDouble(Float interpolation)
    {
        if (this.offsetMap.isEmpty()) return 0;
        float partialInterpolation = interpolation - GenericMath.floor(interpolation);

        Map.Entry<Float, Double> floorEntry = this.offsetMap.floorEntry(partialInterpolation);
        Map.Entry<Float, Double> ceilingEntry = this.offsetMap.ceilingEntry(partialInterpolation);

        float floor = floorEntry.getKey(), ceiling = ceilingEntry.getKey(), diff = ceiling - floor;
        if (diff < GenericMath.DBL_EPSILON) return (floorEntry.getValue() + ceilingEntry.getValue()) / 2;

        float floorFactor = (ceiling - partialInterpolation) / diff;
        float ceilingFactor = (partialInterpolation - floor) / diff;

        return floorFactor * floorEntry.getValue() + ceilingFactor * ceilingEntry.getValue();
    }
}
