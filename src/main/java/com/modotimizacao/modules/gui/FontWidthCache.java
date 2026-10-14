package com.modotimizacao.modules.gui;

import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;

/**
 * Cache de alta performance para medição de largura de textos e fontes (Fase 5).
 * Elimina milhares de decomposições de strings e iterações de glifos por frame no HUD,
 * chat, scoreboard e telas de inventário.
 */
public final class FontWidthCache {
    private static final Object2FloatOpenHashMap<String> CACHE = new Object2FloatOpenHashMap<>();
    private static final int MAX_CACHE_SIZE = 4096;
    private static final int MAX_STRING_LENGTH = 256;

    static {
        CACHE.defaultReturnValue(-1.0f);
    }

    private FontWidthCache() {
    }

    /**
     * Obtém a largura em pixels pré-calculada de uma string, se presente no cache.
     *
     * @param text Texto a ser consultado
     * @return largura em float, ou -1.0f se não estiver em cache
     */
    public static float get(String text) {
        if (text == null || text.length() > MAX_STRING_LENGTH) {
            return -1.0f;
        }
        return CACHE.getFloat(text);
    }

    /**
     * Armazena a largura calculada de uma string no cache.
     *
     * @param text  Texto medido
     * @param width Largura calculada pelo renderizador vanilla
     */
    public static void put(String text, float width) {
        if (text == null || text.length() > MAX_STRING_LENGTH) {
            return;
        }
        if (CACHE.size() >= MAX_CACHE_SIZE) {
            CACHE.clear();
        }
        CACHE.put(text, width);
    }

    /**
     * Limpa todo o cache (acionado ao trocar idioma, resolução ou recarregar recursos).
     */
    public static void clear() {
        CACHE.clear();
    }
}
