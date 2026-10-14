package com.modotimizacao.modules.culling.entity;

import com.modotimizacao.config.ModConfig;

/**
 * Módulo de culling para entidades vivas e mobs (Fase 4).
 * Suprime chamadas de renderização de modelos de mobs que estejam atrás de paredes opacas.
 */
public final class EntityCullingModule {
    private static boolean enabled = true;

    private EntityCullingModule() {
    }

    public static boolean isEnabled() {
        return enabled && ModConfig.getInstance().enableEntityCulling;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }
}
