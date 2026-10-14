package com.modotimizacao.mixin.culling.blockentity;

import com.modotimizacao.modules.culling.blockentity.BlockEntityCuller;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin que intercepta o envio de BlockEntities à pipeline de renderização da GPU.
 * Descarta blocos fora do Frustum ou ocluídos por paredes sólidas.
 */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class MixinBlockEntityRenderDispatcher {

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private <S extends BlockEntityRenderState> void modotimizacao$cullBlockEntity(
            S renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState,
            CallbackInfo ci
    ) {
        if (BlockEntityCuller.shouldCull(renderState, cameraState)) {
            ci.cancel();
        }
    }
}
