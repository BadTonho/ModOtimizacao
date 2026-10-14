package com.modotimizacao.mixin.animations;

import com.modotimizacao.modules.animations.SpriteHolder;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpriteContents.class)
public abstract class MixinSpriteContents {
    @Inject(method = "createAnimationState", at = @At("RETURN"))
    private void modotimizacao$bindSpriteToAnimationState(
            GpuBufferSlice slice, int i,
            CallbackInfoReturnable<SpriteContents.AnimationState> cir
    ) {
        SpriteContents.AnimationState state = cir.getReturnValue();
        if (state instanceof SpriteHolder holder) {
            holder.modotimizacao$setSprite((SpriteContents) (Object) this);
        }
    }
}
