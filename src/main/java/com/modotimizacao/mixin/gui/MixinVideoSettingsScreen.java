package com.modotimizacao.mixin.gui;

import com.modotimizacao.gui.ModConfigScreen;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin que adiciona o botão de acesso rápido às otimizações diretamente
 * na tela nativa de Configurações de Vídeo (Video Settings) do Minecraft.
 */
@Mixin(VideoSettingsScreen.class)
public abstract class MixinVideoSettingsScreen extends OptionsSubScreen {

    protected MixinVideoSettingsScreen(Screen lastScreen, Options options, Component title) {
        super(lastScreen, options, title);
    }

    @Inject(method = "addOptions", at = @At("TAIL"))
    private void modotimizacao$addOptimizationButton(CallbackInfo ci) {
        if (this.list != null) {
            this.list.addHeader(Component.literal("⚡ Mod de Otimização"));
            this.list.addBig(Button.builder(
                    Component.literal("Configurar Otimizações (FPS & Gráficos)..."),
                    btn -> {
                        if (this.minecraft != null) {
                            this.minecraft.setScreenAndShow(new ModConfigScreen(this));
                        }
                    }
            ).build());
        }
    }
}
