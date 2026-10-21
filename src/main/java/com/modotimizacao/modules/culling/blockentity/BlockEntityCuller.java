package com.modotimizacao.modules.culling.blockentity;

import com.modotimizacao.config.ModConfig;
import com.modotimizacao.mixin.culling.blockentity.MixinFrustumAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Motor central de cálculo de Culling para BlockEntities.
 * Executa testes em camadas (O(1), zero raycasts):
 * 1. Isenções vanilla (Beacons, Portais, Blocos quebrando)
 * 2. Distância máxima configurada (default Ilimitado = vanilla)
 * 3. Frustum Culling com coordenadas primitivas
 * 4. Oclusão por enclausuramento sólido com margem protetora (anel ±1)
 */
public final class BlockEntityCuller {

    /** Direções reutilizáveis (Direction.values() clona o array a cada chamada). */
    private static final Direction[] DIRECTIONS = Direction.values();

    /**
     * Posição mutável reutilizada para as consultas de vizinhos das faces e do
     * anel ±1 (Fase 8 — evita {@code pos.relative}/pos.offset}, que alocavam de
     * 6 a 32 objetos por BlockEntity por frame). Confinação de thread:
     * {@code shouldCull} roda exclusivamente na thread de render/main
     * (submissão de BlockEntities), sem reentrância durante os testes.
     */
    private static final BlockPos.MutableBlockPos NEIGHBOR = new BlockPos.MutableBlockPos();

    private BlockEntityCuller() {
    }

    /**
     * Determina se uma BlockEntity deve ser descartada antes de gerar dados na GPU.
     *
     * @param state       Estado de renderização da BlockEntity
     * @param cameraState Estado da câmera da cena
     * @return true se o bloco puder ser descartado com segurança (invisível), false para renderizar normalmente
     */
    public static boolean shouldCull(BlockEntityRenderState state, CameraRenderState cameraState) {
        if (!BlockEntityCullingModule.isEnabled()) {
            return false;
        }

        if (state == null || state.blockPos == null || cameraState == null) {
            return false;
        }

        // 1. Isenções de Fidelidade Vanilla (Regra 4)
        if (ExemptBlockEntities.isExempt(state)) {
            return false;
        }

        Vec3 cameraPos = cameraState.pos;
        if (cameraPos == null) {
            return false;
        }

        BlockPos pos = state.blockPos;

        // 2. Teste de Distância
        double dx = (pos.getX() + 0.5) - cameraPos.x;
        double dy = (pos.getY() + 0.5) - cameraPos.y;
        double dz = (pos.getZ() + 0.5) - cameraPos.z;
        double distSq = dx * dx + dy * dy + dz * dz;

        // Proximidade imediata (< 2 blocos): nunca ocluir para evitar cortes de câmera
        if (distSq <= 4.0) {
            return false;
        }

        double maxDist = ModConfig.getInstance().blockEntityMaxDistance;
        if (distSq > maxDist * maxDist) {
            return true; // Excede distância máxima configurada
        }

        // 3. Teste de Frustum (Tronco de Visão da Câmera) com margem protetora para baús duplos/camas
        if (cameraState.cullFrustum != null) {
            double minX = pos.getX() - 1.0;
            double minY = pos.getY() - 1.0;
            double minZ = pos.getZ() - 1.0;
            double maxX = pos.getX() + 2.0;
            double maxY = pos.getY() + 2.0;
            double maxZ = pos.getZ() + 2.0;

            int frustumResult = ((MixinFrustumAccessor) cameraState.cullFrustum).modotimizacao$invokeCubeInFrustum(
                    minX, minY, minZ, maxX, maxY, maxZ
            );

            // -2 = INSIDE, -1 = INTERSECT. Qualquer outro valor é fora do FOV da câmera.
            if (frustumResult != -2 && frustumResult != -1) {
                return true;
            }
        }

        // 4. Teste de Oclusão (Paredes e Enclausuramento)
        if (!ModConfig.getInstance().blockEntityOcclusionCulling) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return false;
        }

        LocalPlayer player = mc.player;
        if (player != null && player.isSpectator()) {
            return false; // Modo espectador vê através das paredes
        }

        // Teste de Enclausuramento Rápido (O(1), zero raycasts):
        // 1) as 6 faces adjacentes devem ser sólidas e opacas, e
        // 2) a superfície externa da caixa expandida ±1 (anel de 26 células) também,
        //    protegendo modelos que se projetam no bloco vizinho (camas, baús duplos, mods).
        for (Direction dir : DIRECTIONS) {
            BlockState neighbor = level.getBlockState(NEIGHBOR.set(
                    pos.getX() + dir.getStepX(),
                    pos.getY() + dir.getStepY(),
                    pos.getZ() + dir.getStepZ()));
            if (!neighbor.isSolidRender()) {
                return false;
            }
        }
        for (int rx = -1; rx <= 1; rx++) {
            for (int ry = -1; ry <= 1; ry++) {
                for (int rz = -1; rz <= 1; rz++) {
                    if (rx == 0 && ry == 0 && rz == 0) {
                        continue;
                    }
                    BlockState ringNeighbor = level.getBlockState(NEIGHBOR.set(
                            pos.getX() + rx, pos.getY() + ry, pos.getZ() + rz));
                    if (!ringNeighbor.isSolidRender()) {
                        return false;
                    }
                }
            }
        }
        return true; // Enclausurado por paredes sólidas com margem completa
    }
}
