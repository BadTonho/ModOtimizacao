package com.modotimizacao.modules.animations;

import com.modotimizacao.config.ModConfig;

/**
 * Módulo de otimização de texturas animadas (Fase 2).
 * Controla a pausa de animações quando os sprites não estão visíveis no frame atual.
 * O gate de ativação é exclusivamente {@link ModConfig} (sem estado duplicado morto).
 */
public final class AnimationOptimizer {
    private AnimationOptimizer() {
    }

    public static boolean isEnabled() {
        return ModConfig.getInstance().pauseHiddenAnimations;
    }
}
