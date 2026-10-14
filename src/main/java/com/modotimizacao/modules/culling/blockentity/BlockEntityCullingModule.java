package com.modotimizacao.modules.culling.blockentity;

import com.modotimizacao.config.ModConfig;

/**
 * Módulo de culling para blocos especiais / BlockEntities (Fase 3).
 * Evita o processamento e envio à GPU de baús, placas e molduras ocluídos.
 */
public final class BlockEntityCullingModule {
    private static boolean enabled = true;

    private BlockEntityCullingModule() {
    }

    public static boolean isEnabled() {
        return enabled && ModConfig.getInstance().enableBlockEntityCulling;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }
}
