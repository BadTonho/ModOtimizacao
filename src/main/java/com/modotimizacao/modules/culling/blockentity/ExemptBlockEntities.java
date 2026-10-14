package com.modotimizacao.modules.culling.blockentity;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

/**
 * Gerencia a lista de BlockEntities isentas de culling para preservar a fidelidade vanilla (Regra 4).
 * Beacons, portais do End e blocos sendo ativamente minerados/quebrados nunca são ocluídos.
 */
public final class ExemptBlockEntities {
    private static final Set<String> EXEMPT_PATHS = Set.of(
            "beacon",
            "end_gateway",
            "end_portal",
            "structure_block"
    );

    private ExemptBlockEntities() {
    }

    public static boolean isExempt(BlockEntityRenderState state) {
        if (state == null) {
            return false;
        }

        // Se estiver sendo minerado ou quebrado, a animação de quebra precisa ser vista
        if (state.breakProgress != null) {
            return true;
        }

        BlockEntityType<?> type = state.blockEntityType;
        if (type == null) {
            return false;
        }

        Identifier key = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
        if (key == null) {
            return false;
        }

        return EXEMPT_PATHS.contains(key.getPath());
    }
}
