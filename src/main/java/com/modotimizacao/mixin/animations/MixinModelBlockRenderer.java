package com.modotimizacao.mixin.animations;

import com.modotimizacao.modules.animations.AnimationOptimizer;
import com.modotimizacao.modules.animations.SectionAnimationContext;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBlockRenderer.class)
public abstract class MixinModelBlockRenderer {
    @Inject(method = "putQuadWithTint", at = @At("HEAD"))
    private void modotimizacao$recordQuadSprite(
            BlockQuadOutput output, float x, float y, float z,
            BlockAndTintGetter level, BlockState state, BlockPos pos,
            BakedQuad quad, CallbackInfo ci
    ) {
        if (AnimationOptimizer.isEnabled() && quad != null) {
            TextureAtlasSprite sprite = quad.materialInfo().sprite();
            if (sprite != null && sprite.isAnimated()) {
                SectionAnimationContext.recordSprite(sprite.contents());
            }
        }
    }
}
