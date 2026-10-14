package com.modotimizacao.modules.animations;

import net.minecraft.client.renderer.texture.SpriteContents;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rastreador de visibilidade de texturas animadas.
 * Armazena os sprites que foram consultados ou que estão em seções de chunk visíveis no frame atual.
 */
public final class AnimationTracker {
    private static long currentFrame = 0;
    private static final Set<SpriteContents> ACTIVE_SPRITES = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private AnimationTracker() {
    }

    /**
     * Chamado no início do frame de renderização pelo LevelRenderer.
     */
    public static void onFrameStart() {
        currentFrame++;
        ACTIVE_SPRITES.clear();
    }

    /**
     * Marca um sprite animado como ativo/visível para o frame atual.
     */
    public static void markActive(SpriteContents sprite) {
        if (sprite != null && sprite.isAnimated()) {
            ACTIVE_SPRITES.add(sprite);
        }
    }

    /**
     * Verifica se o sprite foi marcado como ativo no frame atual.
     */
    public static boolean isActive(SpriteContents sprite) {
        if (sprite == null) {
            return false;
        }
        return ACTIVE_SPRITES.contains(sprite);
    }

    public static long getCurrentFrame() {
        return currentFrame;
    }
}
