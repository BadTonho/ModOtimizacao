package com.modotimizacao.mixin.animations;

import com.modotimizacao.modules.animations.AnimationOptimizer;
import com.modotimizacao.modules.animations.AnimationTracker;
import com.modotimizacao.modules.animations.SpriteHolder;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.renderer.texture.SpriteContents$AnimationState")
public abstract class MixinSpriteContentsAnimationState implements SpriteHolder {
    @Unique
    private SpriteContents modotimizacao$sprite;

    @Override
    public SpriteContents modotimizacao$getSprite() {
        return this.modotimizacao$sprite;
    }

    @Override
    public void modotimizacao$setSprite(SpriteContents sprite) {
        this.modotimizacao$sprite = sprite;
    }

    @Inject(method = "needsToDraw", at = @At("HEAD"), cancellable = true)
    private void modotimizacao$checkIfVisible(CallbackInfoReturnable<Boolean> cir) {
        if (AnimationOptimizer.isEnabled() && this.modotimizacao$sprite != null) {
            if (!AnimationTracker.isActive(this.modotimizacao$sprite)) {
                cir.setReturnValue(false);
            }
        }
    }
}
