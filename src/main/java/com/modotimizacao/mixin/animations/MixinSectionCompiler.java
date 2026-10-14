package com.modotimizacao.mixin.animations;

import com.modotimizacao.modules.animations.AnimationOptimizer;
import com.modotimizacao.modules.animations.SectionAnimationContext;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionCompiler.class)
public abstract class MixinSectionCompiler {
    @Inject(method = "compile", at = @At("HEAD"))
    private void modotimizacao$onCompileStart(
            SectionPos sectionPos, RenderSectionRegion region,
            VertexSorting vertexSorting, SectionBufferBuilderPack pack,
            CallbackInfoReturnable<?> cir
    ) {
        if (AnimationOptimizer.isEnabled()) {
            SectionAnimationContext.beginSection();
        }
    }
}
