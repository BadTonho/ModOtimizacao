package com.modotimizacao.modules.gui;

import com.modotimizacao.config.ModConfig;

/**
 * Módulo de otimização de interface, fontes e HUD (Fase 5).
 * Controla a ativação do cache de texto e reciclagem de estruturas gráficas.
 */
public final class GuiOptimizationModule {
    private static boolean enabled = true;

    private GuiOptimizationModule() {
    }

    public static boolean isEnabled() {
        return enabled && ModConfig.getInstance().optimizeFontAndHud;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            FontWidthCache.clearAll();
        }
    }

    public static void clearCache() {
        FontWidthCache.clearAll();
    }
}
