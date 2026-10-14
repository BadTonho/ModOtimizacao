package com.modotimizacao.mixin.culling.blockentity;

import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor para invocar o teste de Frustum com coordenadas primitivas double (zero alocação).
 */
@Mixin(Frustum.class)
public interface MixinFrustumAccessor {
    @Invoker("cubeInFrustum")
    int modotimizacao$invokeCubeInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
}
