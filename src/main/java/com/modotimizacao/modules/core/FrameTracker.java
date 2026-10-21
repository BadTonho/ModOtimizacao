package com.modotimizacao.modules.core;

import com.modotimizacao.modules.animations.AnimationTracker;
import com.modotimizacao.modules.particles.ParticleCullCache;

/**
 * Ponto único de sincronização por frame (Fase 8).
 *
 * <p>Todo cache "por frame" do mod converge para cá: o mixin de
 * {@code LevelRenderer.prepareChunkRenders} (início do pipeline de render)
 * chama {@link #onFrameStart()} exatamente uma vez no início de cada frame,
 * e este módulo repassa o sinal a cada subsistema com estado por frame.</p>
 *
 * <ul>
 *   <li>{@link AnimationTracker} — sprites animados marcados como visíveis (Fase 4).</li>
 *   <li>{@link ParticleCullCache} — oclusão de blocos consultada pelo culling
 *       de partículas (Fase 8), válida somente dentro do frame corrente.</li>
 * </ul>
 *
 * <p>Threading: chamado na thread de render/main (prepareChunkRenders), uma
 * única vez por frame. Todos os subsistemas repassados operam exclusivamente
 * nessa mesma thread — nenhum cache por frame é compartilhado entre threads.</p>
 */
public final class FrameTracker {

    private FrameTracker() {
    }

    /**
     * Avança a época do frame e reseta os subsistemas com estado por frame.
     * Custo amortizado O(1) por frame (+ troca/limpeza dos buffers dos caches,
     * sempre sobre a rodada anterior — nunca sobre o frame em curso).
     */
    public static void onFrameStart() {
        AnimationTracker.onFrameStart();
        ParticleCullCache.onFrameStart();
    }
}
