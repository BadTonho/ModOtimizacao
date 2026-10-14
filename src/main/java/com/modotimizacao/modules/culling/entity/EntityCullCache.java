package com.modotimizacao.modules.culling.entity;

import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;

/**
 * Cache de alta performance para os resultados de oclusão de entidades.
 * Indexado pelo ID primitivo da entidade (entity.getId()), com ciclo rápido de reciclagem (100ms)
 * para responder com precisão ao movimento dos mobs sem sobrecarregar a CPU com raycasts repetidos.
 */
public final class EntityCullCache {
    private static final Int2BooleanMap CACHE = new Int2BooleanOpenHashMap();
    private static long lastResetTime = System.currentTimeMillis();
    private static final long CACHE_TTL_MS = 100L; // Atualizado a cada 100ms (10 vezes por segundo)
    private static final int MAX_CACHE_SIZE = 4096; // Teto de segurança de memória

    private EntityCullCache() {
    }

    public static void checkExpiration() {
        long now = System.currentTimeMillis();
        if (now - lastResetTime >= CACHE_TTL_MS || CACHE.size() > MAX_CACHE_SIZE) {
            CACHE.clear();
            lastResetTime = now;
        }
    }

    public static boolean has(int entityId) {
        return CACHE.containsKey(entityId);
    }

    public static boolean get(int entityId) {
        return CACHE.get(entityId);
    }

    public static void put(int entityId, boolean occluded) {
        CACHE.put(entityId, occluded);
    }

    public static void clear() {
        CACHE.clear();
    }
}
