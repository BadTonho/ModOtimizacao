package com.modotimizacao.modules.culling.entity;

import com.modotimizacao.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Motor de culling de entidades ultra-rápido (Zero Raycasts, O(1)).
 * Elimina o custo de CPU e GPU de entidades irrelevantes sem sobrecarregar a main thread.
 */
public final class EntityCuller {

    private EntityCuller() {
    }

    public static boolean shouldCull(Entity entity, double camX, double camY, double camZ) {
        if (!EntityCullingModule.isEnabled() || entity == null) {
            return false;
        }

        Vec3 cameraPos = new Vec3(camX, camY, camZ);

        // 1. Isenções de Fidelidade Vanilla (Regra 4)
        if (ExemptEntities.isExempt(entity, cameraPos)) {
            return false;
        }

        // 2. Proximidade Imediata (< 12 blocos): Nunca ocluir para combate fluido e responsivo
        double distSq = entity.position().distanceToSqr(cameraPos);
        if (distSq <= 144.0) { // 12 * 12
            return false;
        }

        // 3. Limite de Distância Máxima (Configurável)
        double maxDist = ModConfig.getInstance().entityMaxDistance;
        if (distSq > maxDist * maxDist) {
            return true; // Excede distância máxima configurada
        }

        // 4. Culling de Cavernas Subterrâneas (O(1) sem raycast)
        // Se a entidade está significativamente abaixo do jogador (> 12 blocos) e o teto da caverna é sólido, descarta.
        if (ModConfig.getInstance().entityOcclusionCulling) {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            if (level != null && (camY - entity.getY()) > 12.0) {
                BlockPos entityHeadPos = entity.blockPosition().above(2);
                BlockState ceilingState = level.getBlockState(entityHeadPos);
                if (ceilingState.isSolidRender()) {
                    return true; // Mob em caverna sob teto sólido
                }
            }
        }

        return false;
    }
}
