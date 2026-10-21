package com.modotimizacao.mixin.animations;

import com.modotimizacao.modules.animations.AnimatedSpriteHolder;
import com.modotimizacao.modules.animations.AnimationOptimizer;
import com.modotimizacao.modules.animations.AnimationTracker;
import com.modotimizacao.modules.core.FrameTracker;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {
    @Shadow
    @Final
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

    @Inject(method = "prepareChunkRenders", at = @At("HEAD"))
    private void modotimizacao$trackVisibleSectionAnimations(Matrix4fc matrix, boolean bool, CallbackInfoReturnable<?> cir) {
        // Fase 8: sincronização única de frame — alimenta o FrameTracker que
        // avança os caches por frame (animações + oclusão de partículas),
        // incondicionalmente (custo amortizado O(1); os módulos se gateiam
        // nos seus próprios hot paths).
        FrameTracker.onFrameStart();

        if (AnimationOptimizer.isEnabled()) {
            int count = this.visibleSections.size();
            for (int i = 0; i < count; i++) {
                SectionRenderDispatcher.RenderSection section = this.visibleSections.get(i);
                if (section != null) {
                    SectionMesh mesh = section.getSectionMesh();
                    if (mesh instanceof AnimatedSpriteHolder holder) {
                        Set<SpriteContents> sprites = holder.modotimizacao$getAnimatedSprites();
                        if (sprites != null && !sprites.isEmpty()) {
                            for (SpriteContents sprite : sprites) {
                                AnimationTracker.markActive(sprite);
                            }
                        }
                    }
                }
            }
        }
    }
}
