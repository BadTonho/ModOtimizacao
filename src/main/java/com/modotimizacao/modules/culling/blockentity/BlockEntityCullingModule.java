package com.modotimizacao.modules.culling.blockentity;

import com.modotimizacao.config.ModConfig;

/**
 * Módulo de culling para blocos especiais / BlockEntities (Fase 3).
 * Evita o processamento e envio à GPU de baús, placas e molduras
 * fora do frustum ou totalmente enclausurados por blocos opacos.
 * O gate de ativação é exclusivamente {@link ModConfig} (sem estado duplicado morto).
 */
public final class BlockEntityCullingModule {
    private BlockEntityCullingModule() {
    }

    public static boolean isEnabled() {
        return ModConfig.getInstance().enableBlockEntityCulling;
    }
}
