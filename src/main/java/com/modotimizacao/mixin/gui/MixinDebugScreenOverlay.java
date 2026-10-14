package com.modotimizacao.mixin.gui;

import com.modotimizacao.config.ModConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Mixin que adiciona informações de diagnóstico em tempo real no menu F3 (Fase 6).
 * Exibe o status ativo/inativo de cada subsistema de otimização no Minecraft 26.3.
 */
@Mixin(DebugScreenOverlay.class)
public abstract class MixinDebugScreenOverlay {

    @Inject(
            method = "extractLines(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Ljava/util/List;ZI)V",
            at = @At("HEAD")
    )
    private void modotimizacao$addDebugLines(
            GuiGraphicsExtractor extractor,
            List<String> lines,
            boolean left,
            int y,
            CallbackInfo ci
    ) {
        if (left && lines != null) {
            ModConfig config = ModConfig.getInstance();
            lines.add("");
            lines.add(String.format("§a[ModOtimizacao 26.3]§r Anims: %s | BE-Cull: %s | Ent-Cull: %s | HUD: %s | Part: %s",
                    config.pauseHiddenAnimations ? "§aON§r" : "§cOFF§r",
                    config.enableBlockEntityCulling ? (config.blockEntityOcclusionCulling ? "§aON+Occl§r" : "§eON§r") : "§cOFF§r",
                    config.enableEntityCulling ? (config.entityOcclusionCulling ? "§aON+Occl§r" : "§eON§r") : "§cOFF§r",
                    config.optimizeFontAndHud ? "§aON§r" : "§cOFF§r",
                    config.optimizeParticles ? "§aON§r" : "§cOFF§r"
            ));
        }
    }
}
