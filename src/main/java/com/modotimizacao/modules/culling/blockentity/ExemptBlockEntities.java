package com.modotimizacao.modules.culling.blockentity;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

/**
 * Gerencia a lista de BlockEntities isentas de culling para preservar a fidelidade vanilla (Regra 4).
 * Beacons, portais do End e blocos sendo ativamente minerados/quebrados nunca são ocluídos.
 *
 * <p>Os tipos são resolvidos uma única vez (lazy, após o freeze dos registros)
 * como objetos {@link BlockEntityType}: lookup O(1) por identidade de registro,
 * sem string contains por frame.</p>
 */
public final class ExemptBlockEntities {
    private static volatile Set<BlockEntityType<?>> exemptTypes;

    private ExemptBlockEntities() {
    }

    private static Set<BlockEntityType<?>> exemptTypes() {
        Set<BlockEntityType<?>> local = exemptTypes;
        if (local == null) {
            local = Set.of(
                    typeFor("beacon"),
                    typeFor("end_gateway"),
                    typeFor("end_portal"),
                    typeFor("structure_block")
            );
            exemptTypes = local;
        }
        return local;
    }

    private static BlockEntityType<?> typeFor(String path) {
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(path));
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
        return type != null && exemptTypes().contains(type);
    }
}
