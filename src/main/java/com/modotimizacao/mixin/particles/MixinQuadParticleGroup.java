package com.modotimizacao.mixin.particles;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.modotimizacao.modules.particles.ParticleCuller;
import com.modotimizacao.modules.particles.ParticleOptimizationModule;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mixin que intercepta o teste de visibilidade de partículas quádruplas no QuadParticleGroup.
 * Adiciona o teste de oclusão por blocos sólidos para descartar partículas atrás de paredes.
 *
 * <p>Usa {@link WrapOperation} (MixinExtras, embutido no Fabric Loader) em vez de
 * {@code @Redirect}: múltiplos mods podem envolver a mesma chamada sem colisão
 * de mixin, preservando a interoperabilidade.</p>
 */
@Mixin(QuadParticleGroup.class)
public abstract class MixinQuadParticleGroup {

    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/culling/Frustum;pointInFrustum(DDD)Z"
            )
    )
    private boolean modotimizacao$cullOccludedParticle(Frustum frustum, double x, double y, double z, Operation<Boolean> original) {
        // Se estiver fora do frustum da câmera, descarta imediatamente (vanilla)
        if (!original.call(frustum, x, y, z)) {
            return false;
        }

        // Se estiver no frustum, valida se está ocluída por paredes
        if (ParticleOptimizationModule.isEnabled() && ParticleCuller.isOccluded(x, y, z)) {
            return false;
        }

        return true;
    }
}
