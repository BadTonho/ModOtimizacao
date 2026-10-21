package com.modotimizacao.modules.gui;

import com.modotimizacao.config.ModConfig;

/**
 * Módulo de otimização de interface, fontes e HUD (Fase 5).
 * Controla a ativação do cache de larguras de texto por fonte.
 * O gate de ativação é exclusivamente {@link ModConfig} (sem estado duplicado morto).
 */
public final class GuiOptimizationModule {
    private GuiOptimizationModule() {
    }

    public static boolean isEnabled() {
        return ModConfig.getInstance().optimizeFontAndHud;
    }
}
