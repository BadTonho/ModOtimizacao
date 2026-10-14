package com.modotimizacao.modules.particles;

import com.modotimizacao.config.ModConfig;

/**
 * Módulo de otimização de partículas (Fase 5).
 * Controla o culling por oclusão e o limitador adaptativo de densidade.
 */
public final class ParticleOptimizationModule {
    private static boolean enabled = true;

    private ParticleOptimizationModule() {
    }

    public static boolean isEnabled() {
        return enabled && ModConfig.getInstance().optimizeParticles;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            ParticleCullCache.clear();
            ParticleLimiter.clear();
        }
    }

    public static void clearCache() {
        ParticleCullCache.clear();
        ParticleLimiter.clear();
    }
}
