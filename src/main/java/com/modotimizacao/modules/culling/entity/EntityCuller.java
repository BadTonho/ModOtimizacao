package com.modotimizacao.modules.culling.entity;

import com.modotimizacao.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Motor de culling de entidades ultra-rápido (Zero Raycasts, O(1)).
 * Elimina o custo de CPU e GPU de entidades irrelevantes sem sobrecarregar a main thread.
 *
 * <p>Correções de fidelidade vanilla (Regra 4):
 * <ul>
 *   <li>O teste de "entidade em caverna" agora escaneia a coluna inteira de blocos
 *       acima da entidade até o nível da câmera (antes testava apenas 1 bloco,
 *       escondendo mobs visíveis por aberturas laterais ou túneis).</li>
 *   <li>Se a coluna for mais alta que o limite de escaneamento, o teste simplesmente
 *       não conclui (comportamento vanilla), com fail-safe.</li>
 * </ul></p>
 */
public final class EntityCuller {

    /** Autoridade máxima da coluna escaneada (fail-safe para estruturas muito altas). */
    private static final int MAX_COLUMN_SCAN = 16;

    private EntityCuller() {
    }

    public static boolean shouldCull(Entity entity, double camX, double camY, double camZ) {
        if (!EntityCullingModule.isEnabled() || entity == null) {
            return false;
        }

        // 1. Isenções de Fidelidade Vanilla (Regra 4): jogador, espectador, glowing, veículo, laço, bosses
        if (ExemptEntities.isExempt(entity)) {
            return false;
        }

        // 2. Distância única, zero alocação (evita new Vec3 por entidade por frame)
        double dx = entity.getX() - camX;
        double dy = entity.getY() - camY;
        double dz = entity.getZ() - camZ;
        double distSq = dx * dx + dy * dy + dz * dz;

        // Proximidade imediata (< 2.5 blocos): isenção de juntura — evita cortes visuais ao virar esquinas/portas
        if (distSq <= 6.25) {
            return false;
        }

        // 3. Proximidade de combate (< 12 blocos): nunca ocultar
        if (distSq <= 144.0) {
            return false;
        }

        // 4. Limite de Distância Máxima (Configurável; default Ilimitado = vanilla)
        double maxDist = ModConfig.getInstance().entityMaxDistance;
        if (distSq > maxDist * maxDist) {
            return true;
        }

        // 5. Culling de cavernas subterrâneas por coluna sólida (O(altura), zero raycast):
        // só conclui cull se TODOS os blocos acima da entidade até o nível da câmera
        // forem sólidos e opacos — mob invisível de verdade.
        if (ModConfig.getInstance().entityOcclusionCulling) {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            if (level != null && (camY - entity.getY()) > 12.0) {
                return isHiddenBySolidColumn(level, entity, camY);
            }
        }

        return false;
    }

    /**
     * Verifica se toda a coluna de blocos acima da entidade (até o nível da câmera)
     * é sólida e opaca. Retorna false se qualquer abertura for encontrada ou se a
     * coluna exceder o limite de escaneamento (fail-safe vanilla).
     */
    private static boolean isHiddenBySolidColumn(ClientLevel level, Entity entity, double camY) {
        BlockPos root = entity.blockPosition();
        int startY = root.getY() + 2;
        int endY = (int) Math.floor(camY);

        if (endY - startY + 1 > MAX_COLUMN_SCAN) {
            return false; // Coluna mais alta que o scan: indecisão = vanilla
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(root.getX(), startY, root.getZ());
        for (int y = startY; y <= endY; y++) {
            cursor.setY(y);
            BlockState ceiling = level.getBlockState(cursor);
            if (!ceiling.isSolidRender()) {
                return false; // Abertura: entidade pode estar visível, renderiza
            }
        }
        return true; // Enclausurada por coluna sólida até a câmera
    }
}
