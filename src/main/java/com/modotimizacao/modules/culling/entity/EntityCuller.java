package com.modotimizacao.modules.culling.entity;

import com.modotimizacao.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Motor central de cálculo de Occlusion Culling para Entidades e Mobs.
 * Executa testes em camadas:
 * 1. Isenções de fidelidade vanilla (Jogador, Glowing, Montarias, Bosses, Laços, Espectador)
 * 2. Limite de distância máxima configurada
 * 3. Consulta ao cache rápido autolimpante (EntityCullCache)
 * 4. Teste de linha de visão de 3 pontos (Olhos, Centro e Pés) via raycast visual
 */
public final class EntityCuller {

    private EntityCuller() {
    }

    /**
     * Determina se uma entidade deve ser descartada da extração e renderização.
     *
     * @param entity Entidade em avaliação
     * @param camX   Posição X da câmera
     * @param camY   Posição Y da câmera
     * @param camZ   Posição Z da câmera
     * @return true se a entidade puder ser ocultada com segurança (totalmente ocluída), false para renderizar normalmente
     */
    public static boolean shouldCull(Entity entity, double camX, double camY, double camZ) {
        if (!EntityCullingModule.isEnabled()) {
            return false;
        }

        if (entity == null) {
            return false;
        }

        Vec3 cameraPos = new Vec3(camX, camY, camZ);

        // 1. Isenções de Fidelidade Vanilla (Regra 4)
        if (ExemptEntities.isExempt(entity, cameraPos)) {
            return false;
        }

        // 2. Limite de Distância Máxima
        double maxDist = ModConfig.getInstance().entityMaxDistance;
        double distSq = entity.position().distanceToSqr(cameraPos);
        if (distSq > maxDist * maxDist) {
            return true;
        }

        // 3. Teste de Oclusão ativado?
        if (!ModConfig.getInstance().entityOcclusionCulling) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return false;
        }

        // 4. Consulta ao Cache Rápido
        int entityId = entity.getId();
        EntityCullCache.checkExpiration();
        if (EntityCullCache.has(entityId)) {
            return EntityCullCache.get(entityId);
        }

        // 5. Teste de Linha de Visão de 3 Pontos (Cabeça, Centro e Pés)
        // Ponto 1: Olhos / Cabeça
        Vec3 eyes = entity.getEyePosition();
        if (isPointVisible(level, cameraPos, eyes)) {
            EntityCullCache.put(entityId, false);
            return false;
        }

        // Ponto 2: Centro do Tronco
        AABB box = entity.getBoundingBox();
        Vec3 center = box.getCenter();
        if (isPointVisible(level, cameraPos, center)) {
            EntityCullCache.put(entityId, false);
            return false;
        }

        // Ponto 3: Pés (com leve elevação de 0.1 para não colidir com o chão em que pisa)
        Vec3 feet = entity.position().add(0.0, 0.1, 0.0);
        if (isPointVisible(level, cameraPos, feet)) {
            EntityCullCache.put(entityId, false);
            return false;
        }

        // Se todos os 3 pontos estão ocluídos por paredes sólidas opacas, descarta a entidade
        EntityCullCache.put(entityId, true);
        return true;
    }

    /**
     * Executa um raycast visual entre a câmera e um ponto da entidade.
     *
     * @param level Nível do cliente
     * @param from  Posição da câmera
     * @param to    Ponto de destino na entidade
     * @return true se o ponto tiver linha de visão desobstruída até a câmera
     */
    private static boolean isPointVisible(ClientLevel level, Vec3 from, Vec3 to) {
        ClipContext ctx = new ClipContext(
                from,
                to,
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                CollisionContext.empty()
        );

        BlockHitResult hit = level.clip(ctx);
        if (hit.getType() == HitResult.Type.MISS) {
            return true; // Nada no caminho
        }

        BlockPos hitPos = hit.getBlockPos();
        BlockState state = level.getBlockState(hitPos);
        if (!state.canOcclude() && !state.isSolidRender()) {
            return true; // O bloco atingido é transparente (vidro, ar, etc.)
        }

        // Compara a distância do impacto com a distância até o alvo
        double hitDistSq = from.distanceToSqr(hit.getLocation());
        double targetDistSq = from.distanceToSqr(to);

        // Se o impacto ocorreu no próprio alvo ou atrás dele (com tolerância de 0.25 blocos), é visível
        return hitDistSq >= targetDistSq - 0.25;
    }
}
