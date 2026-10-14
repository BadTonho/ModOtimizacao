package com.modotimizacao.modules.animations;

import net.minecraft.client.renderer.texture.SpriteContents;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Contexto thread-local para capturar sprites animados durante
 * a compilação paralela de seções de chunks (SectionCompiler).
 */
public final class SectionAnimationContext {
    private static final ThreadLocal<Set<SpriteContents>> CURRENT_SPRITES = ThreadLocal.withInitial(HashSet::new);

    private SectionAnimationContext() {
    }

    public static void beginSection() {
        CURRENT_SPRITES.get().clear();
    }

    public static void recordSprite(SpriteContents sprite) {
        if (sprite != null && sprite.isAnimated()) {
            CURRENT_SPRITES.get().add(sprite);
        }
    }

    public static Set<SpriteContents> captureAndReset() {
        Set<SpriteContents> set = CURRENT_SPRITES.get();
        if (set.isEmpty()) {
            return Collections.emptySet();
        }
        Set<SpriteContents> copy = new HashSet<>(set);
        set.clear();
        return copy;
    }
}
