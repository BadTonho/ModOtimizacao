package com.modotimizacao.config;

/**
 * Gerenciador de configurações desacoplado e modular do mod.
 * Permite ativar ou desativar individualmente cada módulo de otimização.
 */
public final class ModConfig {
    public boolean pauseHiddenAnimations = true;
    public boolean enableBlockEntityCulling = true;
    /**
     * Distância de culling de BlockEntities.
     * Default: 1.0E9 (sentinela "Ilimitado" — fidelidade vanilla absoluta, Regra 4).
     * O usuário pode reduzir explicitamente na GUI; valores pequenos escondem
     * BlockEntities visíveis e são um trade-off consciente.
     */
    public double blockEntityMaxDistance = 1.0E9;
    public boolean blockEntityOcclusionCulling = true;
    public boolean enableEntityCulling = true;
    /**
     * Distância de culling de entidades. Default: 1.0E9 ("Ilimitado" — Regra 4).
     */
    public double entityMaxDistance = 1.0E9;
    public boolean entityOcclusionCulling = true;
    public boolean optimizeFontAndHud = true;
    public boolean optimizeParticles = true;
    /**
     * Distância de culling de partículas. Default: 1.0E9 ("Ilimitado" — Regra 4).
     */
    public double particleMaxDistance = 1.0E9;
    public int maxParticlesPerBlock = 35;

    private static final ModConfig INSTANCE = new ModConfig();

    private ModConfig() {
    }

    public static ModConfig getInstance() {
        return INSTANCE;
    }
}
