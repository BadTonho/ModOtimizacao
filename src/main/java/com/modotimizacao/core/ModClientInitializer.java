package com.modotimizacao.core;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ponto de entrada exclusivo do cliente para o mod de otimização.
 * Estritamente Client-Side (Regra 5).
 */
public final class ModClientInitializer implements ClientModInitializer {
    public static final String MOD_ID = "modotimizacao";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[ModOtimizacao] Inicializando mod de otimização client-side para Minecraft 26.3...");
        com.modotimizacao.config.ConfigManager.load();
    }
}
