package com.modotimizacao.mixin.animations;

import com.modotimizacao.modules.animations.AnimationOptimizer;
import com.modotimizacao.modules.animations.SectionAnimationContext;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FluidRenderer.class)
public abstract class MixinFluidRenderer {
    @Shadow
    @Final
    private FluidStateModelSet fluidModels;

    @Inject(method = "tesselate", at = @At("HEAD"))
    private void modotimizacao$recordFluidSprites(
            BlockAndTintGetter level, BlockPos pos,
            FluidRenderer.Output output, BlockState blockState,
            FluidState fluidState, CallbackInfo ci
    ) {
        if (AnimationOptimizer.isEnabled() && this.fluidModels != null) {
            FluidModel model = this.fluidModels.get(fluidState);
            if (model != null) {
                if (model.stillMaterial() != null) {
                    TextureAtlasSprite sprite = model.stillMaterial().sprite();
                    if (sprite != null && sprite.isAnimated()) {
                        SectionAnimationContext.recordSprite(sprite.contents());
                    }
                }
                if (model.flowingMaterial() != null) {
                    TextureAtlasSprite sprite = model.flowingMaterial().sprite();
                    if (sprite != null && sprite.isAnimated()) {
                        SectionAnimationContext.recordSprite(sprite.contents());
                    }
                }
            }
        }
    }
}
