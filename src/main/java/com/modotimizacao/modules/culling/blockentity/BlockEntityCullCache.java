package com.modotimizacao.modules.culling.blockentity;

import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;

/**
 * Cache de alta performance para os resultados de raycast/oclusão de BlockEntities.
 * Evita recalcular verificações pesadas de colisão/linha de visão a cada frame.
 */
public final class BlockEntityCullCache {
    private static final Long2BooleanMap CACHE = new Long2BooleanOpenHashMap();
    private static long lastResetTime = System.currentTimeMillis();
    private static final long CACHE_TTL_MS = 500L; // Atualiza a cada 500ms
    private static final int MAX_CACHE_SIZE = 2048; // Evita consumo excessivo de memória

    private BlockEntityCullCache() {
    }

    public static void checkExpiration() {
        long now = System.currentTimeMillis();
        if (now - lastResetTime >= CACHE_TTL_MS || CACHE.size() > MAX_CACHE_SIZE) {
            CACHE.clear();
            lastResetTime = now;
        }
    }

    public static boolean has(long posKey) {
        return CACHE.containsKey(posKey);
    }

    public static boolean get(long posKey) {
        return CACHE.get(posKey);
    }

    public static void put(long posKey, boolean occluded) {
        CACHE.put(posKey, occluded);
    }

    public static void clear() {
        CACHE.clear();
    }
}
