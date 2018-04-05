package com.github.ustc_zzzz.pec.util;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.TrigMath;
import com.flowpowered.math.matrix.Matrix3d;
import com.flowpowered.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author ustc_zzzz
 */
public class ElementRotation implements Element
{
    private final Element child;
    private final Matrix3d rotationMatrix;

    public ElementRotation(Element child, Vector3d rotationAxis, double rotationDegree)
    {
        this.child = child;
        this.rotationMatrix = RotationUtil.getRotationMatrix(rotationAxis, rotationDegree * TrigMath.DEG_TO_RAD);
    }

    @Override
    public List<Particle> getParticles(float interpolation, Random random)
    {
        ArrayList<Particle> result = new ArrayList<>();
        for (Particle p : this.child.getParticles(interpolation, random))
        {
            Particle newParticle = new Particle(p);
            newParticle.coordinate = this.rotationMatrix.transform(p.coordinate);
            newParticle.orientation = this.rotationMatrix.transform(p.orientation.toDouble()).toFloat();
            result.add(newParticle);
        }
        return result;
    }
}
