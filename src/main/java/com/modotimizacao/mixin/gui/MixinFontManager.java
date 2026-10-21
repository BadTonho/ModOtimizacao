package com.modotimizacao.mixin.gui;

import com.modotimizacao.modules.gui.FontWidthCache;
import net.minecraft.client.gui.font.FontManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

/**
 * Mixin que invalida o cache de larguras de fontes ao recarregar recursos
 * (troca de resource packs, idioma etc.).
 *
 * <p>A validade principal vem das novas instâncias de StringSplitter criadas por
 * {@code FontManager#createFont} após cada recarga; este hook é a segunda camada
 * de segurança, limpando proativamente qualquer residual entre recargas.</p>
 */
@Mixin(FontManager.class)
public abstract class MixinFontManager {

    @Inject(method = "reload", at = @At("RETURN"))
    private void modotimizacao$invalidateFontCache(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        FontWidthCache.clearAll();
    }
}
