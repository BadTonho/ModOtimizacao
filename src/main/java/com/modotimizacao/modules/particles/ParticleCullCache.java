package com.modotimizacao.modules.particles;

import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;

/**
 * Cache rápido para oclusão de partículas agrupadas por grade de blocos (Fase 5).
 * Como centenas de partículas compartilham o mesmo bloco (ex: fogueiras, tochas, explosões),
 * o resultado do teste de visibilidade é reutilizado para todas as partículas daquela coordenada.
 * Também impõe um orçamento máximo de raycasts por frame para proteger o frametime.
 */
public final class ParticleCullCache {
    private static final Long2BooleanMap CACHE = new Long2BooleanOpenHashMap();
    private static long lastResetTime = System.currentTimeMillis();
    private static final long CACHE_TTL_MS = 100L; // Reciclado a cada 100ms
    private static final int MAX_CACHE_SIZE = 2048;

    private static final int MAX_RAYCASTS_PER_FRAME = 32;
    private static int raycastsThisFrame = 0;

    private ParticleCullCache() {
    }

    public static void checkExpiration() {
        long now = System.currentTimeMillis();
        if (now - lastResetTime >= CACHE_TTL_MS || CACHE.size() > MAX_CACHE_SIZE) {
            CACHE.clear();
            raycastsThisFrame = 0;
            lastResetTime = now;
        }
    }

    public static boolean has(long blockKey) {
        return CACHE.containsKey(blockKey);
    }

    public static boolean get(long blockKey) {
        return CACHE.get(blockKey);
    }

    public static void put(long blockKey, boolean occluded) {
        CACHE.put(blockKey, occluded);
    }

    public static boolean canRaycast() {
        return raycastsThisFrame < MAX_RAYCASTS_PER_FRAME;
    }

    public static void incrementRaycasts() {
        raycastsThisFrame++;
    }

    public static void clear() {
        CACHE.clear();
        raycastsThisFrame = 0;
    }
}
