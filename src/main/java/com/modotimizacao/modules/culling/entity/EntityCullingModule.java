package com.modotimizacao.modules.culling.entity;

import com.modotimizacao.config.ModConfig;

/**
 * Módulo de culling para entidades vivas e mobs (Fase 4).
 * Suprime chamadas de renderização de modelos de mobs que estejam ocultos
 * (fora da distância configurada ou enclausurados por coluna sólida).
 * O gate de ativação é exclusivamente {@link ModConfig} (sem estado duplicado morto).
 */
public final class EntityCullingModule {
    private EntityCullingModule() {
    }

    public static boolean isEnabled() {
        return ModConfig.getInstance().enableEntityCulling;
    }
}
