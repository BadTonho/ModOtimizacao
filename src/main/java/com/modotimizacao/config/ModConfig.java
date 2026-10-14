package com.modotimizacao.config;

/**
 * Gerenciador de configurações desacoplado e modular do mod.
 * Permite ativar ou desativar individualmente cada módulo de otimização.
 */
public final class ModConfig {
    public boolean pauseHiddenAnimations = true;
    public boolean enableBlockEntityCulling = true;
    public boolean enableEntityCulling = true;
    public boolean optimizeFontAndHud = true;
    public boolean optimizeParticles = true;

    private static final ModConfig INSTANCE = new ModConfig();

    private ModConfig() {
    }

    public static ModConfig getInstance() {
        return INSTANCE;
    }
}
