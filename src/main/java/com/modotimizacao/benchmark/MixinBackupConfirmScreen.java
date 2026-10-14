package com.modotimizacao.benchmark;

import net.minecraft.client.gui.screens.BackupConfirmScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Confirma automaticamente a tela de backup caso ela ainda seja exibida,
 * avancando para a renderizacao do mundo.
 */
@Mixin(BackupConfirmScreen.class)
public abstract class MixinBackupConfirmScreen {

    @Shadow
    protected BackupConfirmScreen.Listener onProceed;

    @Inject(method = "init", at = @At("TAIL"))
    private void modotimizacao$autoProceed(CallbackInfo ci) {
        if (this.onProceed != null) {
            this.onProceed.proceed(false, true);
        }
    }
}
