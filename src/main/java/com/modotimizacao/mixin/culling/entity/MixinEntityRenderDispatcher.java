package com.modotimizacao.mixin.culling.entity;

import com.modotimizacao.modules.culling.entity.EntityCuller;
import com.modotimizacao.modules.culling.entity.EntityCullingModule;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin que intercepta o teste de renderização de entidades no EntityRenderDispatcher.
 * Injeta no retorno (@At("RETURN")): se o vanilla já aprovou que a entidade está no campo de visão,
 * valida a oclusão contra paredes sólidas antes de permitir a extração e submissão na GPU.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class MixinEntityRenderDispatcher {

    @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true)
    private <E extends Entity> void modotimizacao$cullOccludedEntity(
            E entity,
            Frustum frustum,
            double camX,
            double camY,
            double camZ,
            float partialTicks,
            CallbackInfoReturnable<Boolean> cir
    ) {
        // Se o vanilla já decidiu que NÃO deve renderizar (fora do FOV ou longe), não faz nada
        if (!cir.getReturnValueZ()) {
            return;
        }

        // Se o vanilla decidiu renderizar, testa se está ocluído por paredes
        if (EntityCullingModule.isEnabled() && EntityCuller.shouldCull(entity, camX, camY, camZ)) {
            cir.setReturnValue(false);
        }
    }
}
