package com.modotimizacao.gui;

import com.modotimizacao.config.ConfigManager;
import com.modotimizacao.config.ModConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Tela nativa de configurações in-game do Mod de Otimização (Fase 6).
 * Permite ligar/desligar módulos individualmente com persistência em disco.
 */
public class ModConfigScreen extends Screen {
    private final Screen parent;

    public ModConfigScreen(Screen parent) {
        super(Component.literal("⚡ Configurações de Otimização"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 24;

        int leftCol = this.width / 2 - buttonWidth - 5;
        int rightCol = this.width / 2 + 5;
        int startY = 45;

        ModConfig config = ModConfig.getInstance();

        // Linha 1: Animações Inteligentes e Culling de Baús
        this.addRenderableWidget(Button.builder(
                CommonComponents.optionStatus(Component.literal("Smart Animations"), config.pauseHiddenAnimations),
                btn -> {
                    config.pauseHiddenAnimations = !config.pauseHiddenAnimations;
                    btn.setMessage(CommonComponents.optionStatus(Component.literal("Smart Animations"), config.pauseHiddenAnimations));
                }
        ).bounds(leftCol, startY, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(
                CommonComponents.optionStatus(Component.literal("Culling de Baús"), config.enableBlockEntityCulling),
                btn -> {
                    config.enableBlockEntityCulling = !config.enableBlockEntityCulling;
                    btn.setMessage(CommonComponents.optionStatus(Component.literal("Culling de Baús"), config.enableBlockEntityCulling));
                }
        ).bounds(rightCol, startY, buttonWidth, buttonHeight).build());

        // Linha 2: Oclusão de Baús e Culling de Entidades (Mobs)
        this.addRenderableWidget(Button.builder(
                CommonComponents.optionStatus(Component.literal("Oclusão de Baús"), config.blockEntityOcclusionCulling),
                btn -> {
                    config.blockEntityOcclusionCulling = !config.blockEntityOcclusionCulling;
                    btn.setMessage(CommonComponents.optionStatus(Component.literal("Oclusão de Baús"), config.blockEntityOcclusionCulling));
                }
        ).bounds(leftCol, startY + spacing, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(
                CommonComponents.optionStatus(Component.literal("Culling de Mobs"), config.enableEntityCulling),
                btn -> {
                    config.enableEntityCulling = !config.enableEntityCulling;
                    btn.setMessage(CommonComponents.optionStatus(Component.literal("Culling de Mobs"), config.enableEntityCulling));
                }
        ).bounds(rightCol, startY + spacing, buttonWidth, buttonHeight).build());

        // Linha 3: Oclusão de Mobs e Otimização HUD/Fontes
        this.addRenderableWidget(Button.builder(
                CommonComponents.optionStatus(Component.literal("Oclusão de Mobs"), config.entityOcclusionCulling),
                btn -> {
                    config.entityOcclusionCulling = !config.entityOcclusionCulling;
                    btn.setMessage(CommonComponents.optionStatus(Component.literal("Oclusão de Mobs"), config.entityOcclusionCulling));
                }
        ).bounds(leftCol, startY + spacing * 2, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(
                CommonComponents.optionStatus(Component.literal("Otimização HUD/Fontes"), config.optimizeFontAndHud),
                btn -> {
                    config.optimizeFontAndHud = !config.optimizeFontAndHud;
                    btn.setMessage(CommonComponents.optionStatus(Component.literal("Otimização HUD/Fontes"), config.optimizeFontAndHud));
                }
        ).bounds(rightCol, startY + spacing * 2, buttonWidth, buttonHeight).build());

        // Linha 4: Otimização de Partículas
        this.addRenderableWidget(Button.builder(
                CommonComponents.optionStatus(Component.literal("Culling de Partículas"), config.optimizeParticles),
                btn -> {
                    config.optimizeParticles = !config.optimizeParticles;
                    btn.setMessage(CommonComponents.optionStatus(Component.literal("Culling de Partículas"), config.optimizeParticles));
                }
        ).bounds(leftCol, startY + spacing * 3, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Distância de Culling: " + distanceLabel(config.blockEntityMaxDistance)),
                btn -> {
                    // Ciclo: Ilimitado (vanilla) -> 96 -> 64 -> 32 -> Ilimitado
                    double current = config.blockEntityMaxDistance;
                    double next;
                    if (current >= 1.0E8) {
                        next = 96.0;
                    } else if (current >= 96.0) {
                        next = 64.0;
                    } else if (current >= 64.0) {
                        next = 32.0;
                    } else if (current >= 32.0) {
                        next = 1.0E9;
                    } else {
                        next = 96.0; // valores fora do ciclo voltam ao inicio
                    }
                    config.blockEntityMaxDistance = next;
                    config.entityMaxDistance = next;
                    btn.setMessage(Component.literal("Distância de Culling: " + distanceLabel(next)));
                }
        ).bounds(rightCol, startY + spacing * 3, buttonWidth, buttonHeight).build());

        // Botão Inferior: Concluído / Salvar
        int doneWidth = 200;
        this.addRenderableWidget(Button.builder(
                CommonComponents.GUI_DONE,
                btn -> this.onClose()
        ).bounds(this.width / 2 - doneWidth / 2, this.height - 32, doneWidth, buttonHeight).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        this.extractMenuBackground(graphics);
        graphics.centeredText(this.font, this.title, this.width / 2, 18, 0xFFFFFF);
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClose() {
        ConfigManager.save();
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parent);
        }
    }

    /**
     * Formata a distância de culling para exibição na GUI.
     */
    private static String distanceLabel(double distance) {
        if (distance >= 1.0E8) {
            return "Ilimitado (Vanilla)";
        }
        return (int) distance + " blocos";
    }
}
