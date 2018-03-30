package com.github.ustc_zzzz.particlecombinator.util;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3f;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleType;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Collections;
import java.util.Random;
import java.util.function.Consumer;

/**
 * @author ustc_zzzz
 */
public interface Element
{
    Iterable<Particle> getParticles(float interpolation, Random random);

    final class Particle
    {
        public ParticleType effectType;
        public Vector3f orientation;
        public Vector3d coordinate;
        public Vector3f offset;
        public int delayTicks;
        public float speed;

        private static final ParticleType DEFAULT_PARTICLE_TYPE = ParticleTypes.FLAME;

        public Particle()
        {
            this.effectType = DEFAULT_PARTICLE_TYPE;
            this.orientation = Vector3f.UNIT_Y;
            this.coordinate = Vector3d.ZERO;
            this.offset = Vector3f.ZERO;
            this.delayTicks = 0;
            this.speed = 0;
        }

        public Particle(Particle that)
        {
            this.orientation = that.orientation;
            this.delayTicks = that.delayTicks;
            this.coordinate = that.coordinate;
            this.effectType = that.effectType;
            this.offset = that.offset;
            this.speed = that.speed;
        }

        public Element asElement()
        {
            return (interpolation, random) -> Collections.singletonList(this);
        }

        public void spawn(Location<World> location, Object plugin, Player... players)
        {
            Vector3d o = this.offset.toDouble();
            Vector3d v = GenericMath.normalizeSafe(this.orientation).mul(this.speed).toDouble();
            ParticleEffect p = ParticleEffect.builder().type(this.effectType).velocity(v).offset(o).build();
            Location<World> particleLocation = location.add(this.coordinate);
            if (this.delayTicks >= 0)
            {
                Consumer<Task> executor = task ->
                {
                    for (Player player : players)
                    {
                        if (player.getWorld().equals(particleLocation.getExtent()))
                        {
                            player.spawnParticles(p, particleLocation.getPosition());
                        }
                    }
                };
                Task.builder().delayTicks(this.delayTicks).execute(executor).submit(plugin);
            }
        }

        @Override
        public String toString()
        {
            // noinspection StringBufferReplaceableByString
            return new StringBuilder()
                    .append("Particle{effectType=").append(this.effectType)
                    .append(", orientation=").append(this.orientation)
                    .append(", coordinate=").append(this.coordinate)
                    .append(", delayTicks=").append(this.delayTicks)
                    .append(", speed=").append(this.speed)
                    .append('}').toString();
        }
    }
}
