package com.modotimizacao.mixin.animations;

import com.modotimizacao.modules.animations.AnimationTracker;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextureAtlasSprite.class)
public abstract class MixinTextureAtlasSprite {
    @Shadow
    @Final
    private SpriteContents contents;

    @Inject(method = "getU0", at = @At("HEAD"))
    private void modotimizacao$markActiveOnU0(CallbackInfoReturnable<Float> cir) {
        if (this.contents != null && this.contents.isAnimated()) {
            AnimationTracker.markActive(this.contents);
        }
    }

    @Inject(method = "getV0", at = @At("HEAD"))
    private void modotimizacao$markActiveOnV0(CallbackInfoReturnable<Float> cir) {
        if (this.contents != null && this.contents.isAnimated()) {
            AnimationTracker.markActive(this.contents);
        }
    }
}
