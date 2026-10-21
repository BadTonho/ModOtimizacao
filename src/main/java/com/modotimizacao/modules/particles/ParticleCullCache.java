package com.modotimizacao.modules.particles;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;

/**
 * Cache real de oclusão de blocos para o culling de partículas (Fase 8).
 *
 * <p>Motivação: {@code ParticleCuller.isOccluded} consulta o estado do bloco
 * para cada partícula em cada frame — em chuva/neve/explosões são milhares de
 * consultas por segundo reavaliando o <em>mesmo</em> bloco. Como todas as
 * partículas dentro de um mesmo bloco compartilham o mesmo resultado, o
 * hit rate é altíssimo e a maioria das consultas {@code getBlockState}
 * desaparece do frame.</p>
 *
 * <p>Design (zero alocação por consulta, sem custo O(n) no frame ativo):
 * <ul>
 *   <li>Chave: {@code BlockPos.asLong(bx, by, bz)}.</li>
 *   <li>Valor empacotado: {@code (frameId << 1) | bitOclusao} — o frame é parte
 *       do dado, portanto nunca há resultado "stale" no cache ativo.</li>
 *   <li>Double-buffer: dois mapas trocados por frame pelo {@code FrameTracker}.
 *       O mapa promovido a "ativo" é limpo na troca (custo referente aos blocos
 *       de <em>duas</em> rodadas atrás, amortizado); o frame em curso nunca paga
 *       limpeza.</li>
 *   <li>Memória limitada: cada mapa nunca acumula mais que ~2 frames de blocos
 *       consultados (dezenas de KB em cena extrema de chuva).</li>
 * </ul></p>
 *
 * <p>Threading: exclusivo da thread de render/main (consultado por
 * {@code ParticleCuller} durante {@code extractRenderState}, resetado pelo
 * {@code FrameTracker} durante {@code prepareChunkRenders}). Não é thread-safe
 * — e não precisa ser, por construção.</p>
 *
 * <p>Janela de validade: resultados valem por no máximo 1 frame (transição de
 * troca de buffer). Diferenças visuais residuais são impossíveis de perceber
 * em partículas (efeitos transitórios) e não tocam em estado de mundo/server.</p>
 */
public final class ParticleCullCache {

    /** Mapa do frame em curso: só contém entradas carimbadas com frameId atual. */
    private static Long2LongOpenHashMap front = new Long2LongOpenHashMap(256);
    /** Mapa da rodada anterior; reciclado como "front" duas rodadas depois. */
    private static Long2LongOpenHashMap back = new Long2LongOpenHashMap(256);
    /** Época do frame atual; começa em 1 porque 0 é o valor "vazio" das tabelas. */
    private static long frameId = 1;

    static {
        front.defaultReturnValue(0L);
        back.defaultReturnValue(0L);
    }

    private ParticleCullCache() {
    }

    /**
     * Chamado pelo {@code FrameTracker} ao iniciar cada frame: troca os buffers
     * e limpa o mapa promovido a ativo. Nunca toca no frame que ainda está sendo
     * usado pelas fases de extração que vêm na sequência.
     */
    public static void onFrameStart() {
        frameId++;
        Long2LongOpenHashMap tmp = front;
        front = back;
        back = tmp;
        front.clear(); // limpa o mapa que RECEBERÁ o frame novo (dados de 2 rodadas atrás)
    }

    /**
     * Consulta rápida do frame em curso.
     *
     * @return a entrada empacotada (frame válido por construção) ou {@code 0L} se ausente
     */
    static long lookup(long blockPosAsLong) {
        return front.get(blockPosAsLong);
    }

    /**
     * Grava o resultado da oclusão do bloco para o restante do frame atual.
     */
    static void store(long blockPosAsLong, boolean occluded) {
        front.put(blockPosAsLong, (frameId << 1) | (occluded ? 1L : 0L));
    }
}
