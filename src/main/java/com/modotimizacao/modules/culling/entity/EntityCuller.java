package com.modotimizacao.modules.culling.entity;

import com.modotimizacao.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

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

    /**
     * Cursor de posição reutilizado para o scan de coluna (Fase 8 — evita
     * {@code new MutableBlockPos} por entidade por frame). Confinação de thread:
     * {@code shouldCull} roda exclusivamente na thread de render/main
     * (EntityRenderDispatcher.shouldRender), sem reentrância durante o scan.
     */
    private static final BlockPos.MutableBlockPos CURSOR = new BlockPos.MutableBlockPos();

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

        // 4. Limite de Distância Máxima (Configurável; default Ilimitado = vanilla).
        //    Fase 8: distância medida até o PONTO MAIS PRÓXIMO da hitbox (clamp da
        //    câmera no envelope da AABB, zero alocação) — mobs grandes (Ghast,
        //    Ravager) não sofrem pop-in/out na borda do limite. Teste estritamente
        //    MAIS conservador que a distância ponto-a-ponto da câmera ao centro:
        //    só oculta quando a hitbox INTEIRA passa do limite (Regra 4).
        //    O sentinel "Ilimitado" (1.0E9) continua nunca ocultando.
        double maxDist = ModConfig.getInstance().entityMaxDistance;
        AABB box = entity.getBoundingBox();
        if (box == null) {
            return false; // fail-safe: sem hitbox não há culling
        }
        double nearX = Mth.clamp(camX, box.minX, box.maxX);
        double nearY = Mth.clamp(camY, box.minY, box.maxY);
        double nearZ = Mth.clamp(camZ, box.minZ, box.maxZ);
        double ndx = camX - nearX;
        double ndy = camY - nearY;
        double ndz = camZ - nearZ;
        if (ndx * ndx + ndy * ndy + ndz * ndz > maxDist * maxDist) {
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

        // Fase 8: a coluna vertical nunca cruza chunks (x/z fixos) — resolve o
        // chunk UMA vez (16x menos resoluções de chunk por mob subterrâneo) e
        // itera as seções direto no chunk, comportamento idêntico ao level.getBlockState.
        LevelChunk chunk = level.getChunk(root.getX() >> 4, root.getZ() >> 4);
        BlockPos.MutableBlockPos cursor = CURSOR.set(root.getX(), startY, root.getZ());
        for (int y = startY; y <= endY; y++) {
            cursor.setY(y);
            BlockState ceiling = chunk.getBlockState(cursor);
            if (!ceiling.isSolidRender()) {
                return false; // Abertura: entidade pode estar visível, renderiza
            }
        }
        return true; // Enclausurada por coluna sólida até a câmera
    }
}
