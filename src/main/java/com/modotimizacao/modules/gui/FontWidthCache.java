package com.modotimizacao.modules.gui;

import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.client.StringSplitter;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Cache de alta performance para medição de largura de textos e fontes (Fase 5).
 * Elimina milhares de decomposições de strings e iterações de glifos por frame no HUD,
 * chat, scoreboard e telas de inventário.
 *
 * <p>Correção de fidelidade (Regra 4): a chave primária de invalidação é a própria
 * instância do {@link StringSplitter}. Como cada recarga de recursos/idioma cria
 * novas instâncias de Font {@code(FontManager#createFont)}, os caches obsoletos
 * ficam naturalmente órfãos e são reciclados pelo limite de instâncias — nunca
 * servindo uma largura medida com uma fonte para outra.</p>
 */
public final class FontWidthCache {
    private static final Map<StringSplitter, Object2FloatOpenHashMap<String>> INSTANCES = new IdentityHashMap<>();
    private static final int MAX_INSTANCES = 8;
    private static final int MAX_ENTRIES_PER_INSTANCE = 512;
    private static final int MAX_STRING_LENGTH = 256;

    private FontWidthCache() {
    }

    /**
     * Obtém a largura em pixels pré-calculada de uma string, se presente no cache
     * da instância de splitter (fonte) informada.
     *
     * @param splitter Instância do StringSplitter (define a fonte/métricas válidas)
     * @param text     Texto a ser consultado
     * @return largura em float, ou -1.0f se não estiver em cache
     */
    public static float get(StringSplitter splitter, String text) {
        if (splitter == null || text == null || text.length() > MAX_STRING_LENGTH) {
            return -1.0f;
        }
        Object2FloatOpenHashMap<String> cache = INSTANCES.get(splitter);
        if (cache == null) {
            return -1.0f;
        }
        return cache.getFloat(text);
    }

    /**
     * Armazena a largura calculada de uma string no cache da instância de splitter informada.
     *
     * @param splitter Instância do StringSplitter (define a fonte/métricas válidas)
     * @param text     Texto medido
     * @param width    Largura calculada pelo renderizador vanilla
     */
    public static void put(StringSplitter splitter, String text, float width) {
        if (splitter == null || text == null || text.length() > MAX_STRING_LENGTH) {
            return;
        }
        Object2FloatOpenHashMap<String> cache = INSTANCES.get(splitter);
        if (cache == null) {
            if (INSTANCES.size() >= MAX_INSTANCES) {
                // Teto de segurança de memória: descarta caches de splitters antigos.
                INSTANCES.clear();
            }
            cache = new Object2FloatOpenHashMap<>();
            cache.defaultReturnValue(-1.0f);
            INSTANCES.put(splitter, cache);
        }
        if (cache.size() >= MAX_ENTRIES_PER_INSTANCE) {
            cache.clear();
        }
        cache.put(text, width);
    }

    /**
     * Limpa todos os caches (acionado ao recarregar recursos/recursos de fonte).
     */
    public static void clearAll() {
        INSTANCES.clear();
    }
}
