package com.modotimizacao.modules.culling.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Gerencia a lista de exceções de fidelidade vanilla (Regra 4) para entidades.
 * Garante que jogadores, mobs brilhantes (glowing), montarias compartilhadas,
 * bosses e mobs em laços nunca sejam ocultados indevidamente.
 */
public final class ExemptEntities {
    private static final Set<String> EXEMPT_BOSS_PATHS = Set.of(
            "ender_dragon",
            "wither",
            "elder_guardian"
    );

    private ExemptEntities() {
    }

    /**
     * Verifica se uma entidade está isenta de culling.
     *
     * @param entity    Entidade a ser testada
     * @param cameraPos Posição atual da câmera no mundo
     * @return true se a entidade for isenta (deve renderizar), false caso possa ser testada por oclusão
     */
    public static boolean isExempt(Entity entity, Vec3 cameraPos) {
        if (entity == null) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        // 1. O próprio jogador local e a entidade de visualização da câmera
        if (player != null && (entity == player || entity == mc.getCameraEntity())) {
            return true;
        }

        // 2. Modo espectador: deve enxergar todas as entidades através de paredes como no vanilla
        if (player != null && player.isSpectator()) {
            return true;
        }

        // 3. Efeito Glowing (brilho de contorno / flecha espectral): DEVE ser visto através de paredes
        if (entity.isCurrentlyGlowing()) {
            return true;
        }

        // 4. Montarias e veículos compartilhados com o jogador
        if (player != null) {
            if (entity.isPassengerOfSameVehicle(player) || entity.getRootVehicle() == player.getRootVehicle()) {
                return true;
            }
            if (entity.hasIndirectPassenger(player)) {
                return true;
            }
        }

        // 5. Mobs com laço (leash): preserva a renderização do laço visual
        if (entity instanceof Leashable leashable && leashable.isLeashed()) {
            return true;
        }

        // 6. Chefões de grandes proporções (Dragon, Wither, Elder Guardian)
        EntityType<?> type = entity.getType();
        if (type != null) {
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (id != null && EXEMPT_BOSS_PATHS.contains(id.getPath())) {
                return true;
            }
        }

        // 7. Proximidade imediata (< 2.5 blocos): evita cortes visuais ao virar esquinas ou cruzar portas
        if (cameraPos != null) {
            double distSq = entity.position().distanceToSqr(cameraPos);
            if (distSq <= 6.25) {
                return true;
            }
        }

        return false;
    }
}
