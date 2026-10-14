package com.modotimizacao.benchmark;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * Mixin universal para telemetria de FPS durante benchmarks.
 * Grava amostras segundo a segundo de FPS real sem afetar o pipeline grafico.
 */
@Mixin(Minecraft.class)
public abstract class MixinFpsRecorder {

    @Unique
    private static PrintWriter modotimizacao$writer;
    @Unique
    private static long modotimizacao$lastSampleMs = 0;
    @Unique
    private static int modotimizacao$frameCount = 0;
    @Unique
    private static int modotimizacao$elapsedSec = 0;

    @Inject(method = "renderFrame", at = @At("HEAD"))
    private void modotimizacao$sampleFps(boolean renderLevel, CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.level == null || mc.player == null) {
            return;
        }

        long now = System.currentTimeMillis();
        modotimizacao$frameCount++;

        if (modotimizacao$writer == null) {
            try {
                modotimizacao$writer = new PrintWriter(new FileWriter("benchmark_fps.csv", false), true);
                modotimizacao$writer.println("Timestamp,ElapsedSec,Fps,InstantFps");
                modotimizacao$lastSampleMs = now;

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    if (modotimizacao$writer != null) {
                        modotimizacao$writer.flush();
                        modotimizacao$writer.close();
                    }
                }));
            } catch (Exception ignored) {
            }
        }

        if (modotimizacao$writer != null && (now - modotimizacao$lastSampleMs) >= 1000) {
            long delta = now - modotimizacao$lastSampleMs;
            double instantFps = (modotimizacao$frameCount * 1000.0) / delta;
            int vanillaFps = mc.getFps();
            modotimizacao$elapsedSec++;

            modotimizacao$writer.printf(java.util.Locale.US, "%d,%d,%d,%.1f%n",
                    now,
                    modotimizacao$elapsedSec,
                    vanillaFps,
                    instantFps
            );
            modotimizacao$writer.flush();

            modotimizacao$frameCount = 0;
            modotimizacao$lastSampleMs = now;
        }
    }
}
