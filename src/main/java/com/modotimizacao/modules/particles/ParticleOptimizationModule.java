package com.modotimizacao.modules.particles;

import com.modotimizacao.config.ModConfig;

/**
 * Módulo de otimização de partículas (Fase 5).
 * Controla o culling por oclusão e o limitador adaptativo de densidade.
 * O gate de ativação é exclusivamente {@link ModConfig} (sem estado duplicado morto).
 */
public final class ParticleOptimizationModule {
    private ParticleOptimizationModule() {
    }

    public static boolean isEnabled() {
        return ModConfig.getInstance().optimizeParticles;
    }
}
