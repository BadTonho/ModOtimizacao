package com.modotimizacao.modules.animations;

import com.modotimizacao.config.ModConfig;

/**
 * Módulo de otimização de texturas animadas (Fase 2).
 * Controla a pausa de animações quando os sprites não estão visíveis no frame atual.
 */
public final class AnimationOptimizer {
    private static boolean enabled = true;

    private AnimationOptimizer() {
    }

    public static boolean isEnabled() {
        return enabled && ModConfig.getInstance().pauseHiddenAnimations;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }
}
