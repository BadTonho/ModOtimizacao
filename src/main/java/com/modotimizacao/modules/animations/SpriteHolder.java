package com.modotimizacao.modules.animations;

import net.minecraft.client.renderer.texture.SpriteContents;

/**
 * Interface duck-typing para armazenar a referência ao SpriteContents
 * diretamente na instância de SpriteContents$AnimationState.
 */
public interface SpriteHolder {
    SpriteContents modotimizacao$getSprite();
    void modotimizacao$setSprite(SpriteContents sprite);
}
