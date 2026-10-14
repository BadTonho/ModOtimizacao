package com.modotimizacao.benchmark;

import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Desativa solicitacao de backup durante os benchmarks automatizados,
 * permitindo que mundos pesados de versoes anteriores abram instantaneamente no QuickPlay.
 */
@Mixin(LevelSummary.class)
public abstract class MixinLevelSummary {

    @Inject(method = "shouldBackup", at = @At("HEAD"), cancellable = true)
    private void modotimizacao$neverBackup(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
