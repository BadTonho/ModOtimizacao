package com.modotimizacao.mixin.gui;

import com.modotimizacao.modules.gui.FontWidthCache;
import com.modotimizacao.modules.gui.GuiOptimizationModule;
import net.minecraft.client.StringSplitter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin que intercepta o cálculo de largura de textos no StringSplitter.
 * Evita decomposição caractere por caractere e busca de métricas para textos repetidos no HUD.
 *
 * <p>O cache é consultado e preenchido por instância do splitter (fonte),
 * garantindo que larguras nunca sejam cruzadas entre fontes diferentes.</p>
 */
@Mixin(StringSplitter.class)
public abstract class MixinStringSplitter {

    @Inject(method = "stringWidth(Ljava/lang/String;)F", at = @At("HEAD"), cancellable = true)
    private void modotimizacao$fastStringWidth(String string, CallbackInfoReturnable<Float> cir) {
        if (GuiOptimizationModule.isEnabled() && string != null) {
            float width = FontWidthCache.get((StringSplitter) (Object) this, string);
            if (width >= 0.0f) {
                cir.setReturnValue(width);
            }
        }
    }

    @Inject(method = "stringWidth(Ljava/lang/String;)F", at = @At("RETURN"))
    private void modotimizacao$saveStringWidth(String string, CallbackInfoReturnable<Float> cir) {
        if (GuiOptimizationModule.isEnabled() && string != null) {
            FontWidthCache.put((StringSplitter) (Object) this, string, cir.getReturnValueF());
        }
    }
}
