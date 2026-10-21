package com.modotimizacao.modules.particles;

import com.modotimizacao.config.ModConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Motor de culling de partículas por grade de blocos (Fase 5).
 * Descarta partículas que estejam dentro de blocos sólidos opacos ou atrás de paredes.
 */
public final class ParticleCuller {

    private ParticleCuller() {
    }

    /**
     * Verifica se uma partícula na coordenada (x, y, z) está ocluída da visão da câmera.
     *
     * @param x Coordenada X da partícula
     * @param y Coordenada Y da partícula
     * @param z Coordenada Z da partícula
     * @return true se estiver ocluída (deve pular o envio à GPU), false se for visível
     */
    public static boolean isOccluded(double x, double y, double z) {
        if (!ParticleOptimizationModule.isEnabled()) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return false;
        }

        Camera camera = mc.gameRenderer.mainCamera();
        if (camera == null) {
            return false;
        }

        Vec3 camPos = camera.position();
        if (camPos == null) {
            return false;
        }

        // 1. Teste de Distância
        double dx = x - camPos.x;
        double dy = y - camPos.y;
        double dz = z - camPos.z;
        double distSq = dx * dx + dy * dy + dz * dz;

        // Proximidade imediata (<= 3 blocos): sempre visível
        if (distSq <= 9.0) {
            return false;
        }

        // Limite máximo de renderização de partículas
        double maxDist = ModConfig.getInstance().particleMaxDistance;
        if (distSq > maxDist * maxDist) {
            return true;
        }

        // 2. Teste se está dentro de um bloco sólido opaco — servido por cache real
        //    por frame (Fase 8): partículas do mesmo bloco no mesmo frame compartilham
        //    UMA única consulta getBlockState. Zero alocação no caminho de hit.
        int bx = Mth.floor(x);
        int by = Mth.floor(y);
        int bz = Mth.floor(z);
        long key = BlockPos.asLong(bx, by, bz);
        long packed = ParticleCullCache.lookup(key);
        if (packed != 0L) {
            return (packed & 1L) != 0L; // entradas do front são sempre do frame atual
        }

        BlockState state = level.getBlockState(new BlockPos(bx, by, bz));
        boolean occluded = state.isSolidRender(); // Bloco sólido: remove partículas em rocha
        ParticleCullCache.store(key, occluded);
        return occluded;
    }
}
