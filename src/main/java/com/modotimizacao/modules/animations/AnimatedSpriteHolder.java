package com.modotimizacao.modules.animations;

import net.minecraft.client.renderer.texture.SpriteContents;
import java.util.Set;

/**
 * Interface duck-typing injetada no CompiledSectionMesh para associar
 * a lista de sprites animados contidos naquele pedaço de terreno.
 */
public interface AnimatedSpriteHolder {
    Set<SpriteContents> modotimizacao$getAnimatedSprites();
    void modotimizacao$setAnimatedSprites(Set<SpriteContents> sprites);
}
