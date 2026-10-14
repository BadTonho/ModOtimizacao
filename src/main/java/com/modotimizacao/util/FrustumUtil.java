package com.modotimizacao.util;

/**
 * Utilitários matemáticos e de interseção de visão/frustum de alta performance.
 */
public final class FrustumUtil {
    private FrustumUtil() {
    }

    /**
     * Verifica de forma simplificada se uma caixa delimitadora (AABB) está dentro de um raio de visão.
     */
    public static boolean isInRange(double deltaX, double deltaY, double deltaZ, double maxDistanceSquared) {
        return (deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) <= maxDistanceSquared;
    }
}
