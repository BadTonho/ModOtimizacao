package com.modotimizacao.modules.particles;

import com.modotimizacao.config.ModConfig;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CritParticle;
import net.minecraft.client.particle.NoteParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TotemParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * Limitador adaptativo de densidade de partículas (Fase 5).
 * Previne quedas bruscas de FPS e travamentos de GPU decorrentes de centenas de partículas
 * acumuladas no mesmo bloco durante explosões massivas de TNT, fogos de artifício ou poções.
 *
 * <p>Correções de fidelidade vanilla (Regra 4):
 * <ul>
 *   <li>Partículas de feedback crítico nunca são descartadas (crit/dano/enchanted hit,
 *       nota de música, totem da imortalidade) — o vanilla as exibe e elas são
 *       essenciais para leitura de combate.</li>
 *   <li>A janela de densidade é alinhada ao tick do jogo quando dentro de um mundo
 *       (antes usava relógio pared, derrapando em lag/pausa); fora de mundo usa relógio.</li>
 * </ul></p>
 */
public final class ParticleLimiter {
    private static final Long2IntMap PARTICLES_PER_BLOCK = new Long2IntOpenHashMap();
    private static long lastGameTick = -1L;
    private static long lastResetTime = System.currentTimeMillis();
    private static int totalThisTick = 0;
    private static final int MAX_GLOBAL_PER_TICK = 1000;
    private static final long FALLBACK_WINDOW_MS = 50L;

    private ParticleLimiter() {
    }

    /**
     * Janela de densidade: alinhada ao tick do jogo em mundo; relógio pared nos menus.
     */
    private static void checkTickReset() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            long tick = level.getGameTime();
            if (tick != lastGameTick) {
                lastGameTick = tick;
                resetWindow();
            }
        } else {
            long now = System.currentTimeMillis();
            if (now - lastResetTime >= FALLBACK_WINDOW_MS) {
                lastResetTime = now;
                resetWindow();
            }
        }
    }

    private static void resetWindow() {
        PARTICLES_PER_BLOCK.clear();
        totalThisTick = 0;
    }

    /**
     * Partículas de feedback de combate/ritual: o vanilla as exibe sempre, então
     * nunca são limitadas (fidelidade vanilla). No 26.3, crit, dano-inimigo e
     * enchanted-hit são a mesma classe {@link CritParticle}.
     */
    private static boolean isFeedbackParticle(Particle particle) {
        return particle instanceof CritParticle
                || particle instanceof NoteParticle
                || particle instanceof TotemParticle;
    }

    /**
     * Determina se a criação de uma partícula deve ser cancelada por sobrecarga de densidade.
     *
     * @param particle Partícula prestes a ser adicionada
     * @return true se deve descartar a partícula, false se deve permitir
     */
    public static boolean shouldThrottle(Particle particle) {
        if (!ParticleOptimizationModule.isEnabled() || particle == null) {
            return false;
        }

        // Isenção de fidelidade: feedback visual de gameplay nunca é cortado
        if (isFeedbackParticle(particle)) {
            return false;
        }

        checkTickReset();

        // Limite global de segurança contra crash por spam massivo
        if (++totalThisTick > MAX_GLOBAL_PER_TICK) {
            return true;
        }

        AABB box = particle.getBoundingBox();
        if (box == null) {
            return false;
        }

        // Centro da AABB calculado manualmente: box.getCenter() aloca um Vec3 por
        // partícula avaliada (Fase 8 — zero alocação no hot path de spawn)
        double cx = (box.minX + box.maxX) * 0.5;
        double cy = (box.minY + box.maxY) * 0.5;
        double cz = (box.minZ + box.maxZ) * 0.5;
        int bx = Mth.floor(cx);
        int by = Mth.floor(cy);
        int bz = Mth.floor(cz);
        long blockKey = BlockPos.asLong(bx, by, bz);

        int currentCount = PARTICLES_PER_BLOCK.get(blockKey);
        int maxPerBlock = ModConfig.getInstance().maxParticlesPerBlock;

        if (currentCount >= maxPerBlock) {
            return true; // Excesso de partículas concentradas no mesmo bloco
        }

        PARTICLES_PER_BLOCK.put(blockKey, currentCount + 1);
        return false;
    }
}
