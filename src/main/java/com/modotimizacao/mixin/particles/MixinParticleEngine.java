package com.modotimizacao.mixin.particles;

import com.modotimizacao.modules.particles.ParticleLimiter;
import com.modotimizacao.modules.particles.ParticleOptimizationModule;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin que intercepta a adição de partículas no ParticleEngine.
 * Atua como limitador adaptativo para evitar picos de lag por excesso de partículas (ex: explosões massivas).
 */
@Mixin(ParticleEngine.class)
public abstract class MixinParticleEngine {

    @Inject(method = "add(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
    private void modotimizacao$throttleParticle(Particle particle, CallbackInfo ci) {
        if (ParticleOptimizationModule.isEnabled() && ParticleLimiter.shouldThrottle(particle)) {
            ci.cancel();
        }
    }
}
