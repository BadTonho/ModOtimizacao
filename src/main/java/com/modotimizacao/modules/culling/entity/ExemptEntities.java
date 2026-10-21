package com.modotimizacao.modules.culling.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;

import java.util.Set;

/**
 * Gerencia a lista de exceções de fidelidade vanilla (Regra 4) para entidades.
 * Garante que jogadores, mobs brilhantes (glowing), montarias compartilhadas,
 * bosses e mobs em laços nunca sejam ocultados indevidamente.
 *
 * <p>Os bosses são resolvidos uma única vez (lazy, após o freeze dos registros)
 * como objetos {@link EntityType}: lookup O(1) por identidade de registro,
 * sem string contains por frame.</p>
 */
public final class ExemptEntities {
    private static volatile Set<EntityType<?>> exemptBossTypes;

    private ExemptEntities() {
    }

    private static Set<EntityType<?>> exemptBossTypes() {
        Set<EntityType<?>> local = exemptBossTypes;
        if (local == null) {
            local = Set.of(
                    entityTypeFor("ender_dragon"),
                    entityTypeFor("wither"),
                    entityTypeFor("elder_guardian")
            );
            exemptBossTypes = local;
        }
        return local;
    }

    private static EntityType<?> entityTypeFor(String path) {
        return BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(path));
    }

    /**
     * Verifica se uma entidade está isenta de culling (deve renderizar sempre).
     *
     * @param entity Entidade a ser testada
     * @return true se a entidade for isenta, false caso possa ser testada por distância/oclusão
     */
    public static boolean isExempt(Entity entity) {
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
        return exemptBossTypes().contains(entity.getType());
    }
}
