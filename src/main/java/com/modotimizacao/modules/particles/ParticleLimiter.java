package com.modotimizacao.modules.particles;

import com.modotimizacao.config.ModConfig;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Limitador adaptativo de densidade de partículas (Fase 5).
 * Previne quedas bruscas de FPS e travamentos de GPU decorrentes de centenas de partículas
 * acumuladas no mesmo bloco durante explosões massivas de TNT, fogos de artifício ou poções.
 */
public final class ParticleLimiter {
    private static final Long2IntMap PARTICLES_PER_BLOCK = new Long2IntOpenHashMap();
    private static long lastResetTime = System.currentTimeMillis();
    private static int totalThisTick = 0;
    private static final int MAX_GLOBAL_PER_TICK = 1000;
    private static final long TICK_DURATION_MS = 50L;

    private ParticleLimiter() {
    }

    private static void checkTickReset() {
        long now = System.currentTimeMillis();
        if (now - lastResetTime >= TICK_DURATION_MS) {
            PARTICLES_PER_BLOCK.clear();
            totalThisTick = 0;
            lastResetTime = now;
        }
    }

    /**
     * Determina se a criação de uma partícula deve ser cancelada por sobrecarga de densidade.
     *
     * @param particle Partícula prestes a ser adicionada
     * @return true se deve descartar a partícula, false se deve permitir
     */
    public static boolean shouldThrottle(Particle particle) {
        if (!ParticleOptimizationModule.isEnabled() || particle == null) {
            return false;
        }

        checkTickReset();

        // Limite global de segurança contra crash por spam massivo
        if (++totalThisTick > MAX_GLOBAL_PER_TICK) {
            return true;
        }

        AABB box = particle.getBoundingBox();
        if (box == null) {
            return false;
        }

        Vec3 center = box.getCenter();
        int bx = Mth.floor(center.x);
        int by = Mth.floor(center.y);
        int bz = Mth.floor(center.z);
        long blockKey = BlockPos.asLong(bx, by, bz);

        int currentCount = PARTICLES_PER_BLOCK.get(blockKey);
        int maxPerBlock = ModConfig.getInstance().maxParticlesPerBlock;

        if (currentCount >= maxPerBlock) {
            return true; // Excesso de partículas concentradas no mesmo bloco
        }

        PARTICLES_PER_BLOCK.put(blockKey, currentCount + 1);
        return false;
    }
}
