package com.modotimizacao.mixin.animations;

import com.modotimizacao.modules.animations.AnimatedSpriteHolder;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Collections;
import java.util.Set;

import com.modotimizacao.modules.animations.SectionAnimationContext;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CompiledSectionMesh.class)
public abstract class MixinCompiledSectionMesh implements AnimatedSpriteHolder {
    @Unique
    private Set<SpriteContents> modotimizacao$animatedSprites = Collections.emptySet();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void modotimizacao$onInit(CallbackInfo ci) {
        Set<SpriteContents> captured = SectionAnimationContext.captureAndReset();
        if (!captured.isEmpty()) {
            this.modotimizacao$animatedSprites = captured;
        }
    }

    @Override
    public Set<SpriteContents> modotimizacao$getAnimatedSprites() {
        return this.modotimizacao$animatedSprites;
    }

    @Override
    public void modotimizacao$setAnimatedSprites(Set<SpriteContents> sprites) {
        this.modotimizacao$animatedSprites = (sprites != null) ? sprites : Collections.emptySet();
    }
}
