package com.modotimizacao.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Gerenciador de persistência de configurações em disco no formato JSON (Fase 6).
 * Salva e carrega o arquivo de configuração em .minecraft/config/modotimizacao.json.
 */
public final class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("modotimizacao-config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("modotimizacao.json");

    private ConfigManager() {
    }

    /**
     * Carrega as configurações do disco. Se o arquivo não existir, grava os padrões.
     */
    public static void load() {
        if (!Files.exists(CONFIG_FILE)) {
            LOGGER.info("[ModOtimizacao] Arquivo modotimizacao.json não encontrado. Criando configuração padrão...");
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
            if (loaded != null) {
                apply(loaded);
                LOGGER.info("[ModOtimizacao] Configurações carregadas com sucesso de: {}", CONFIG_FILE.getFileName());
            } else {
                LOGGER.warn("[ModOtimizacao] Arquivo modotimizacao.json vazio. Restaurando padrões...");
                save();
            }
        } catch (Exception e) {
            LOGGER.error("[ModOtimizacao] Erro ao ler modotimizacao.json. Mantendo configurações padrão.", e);
        }
    }

    /**
     * Salva as configurações atuais no disco em formato JSON legível.
     */
    public static void save() {
        try {
            Path parent = CONFIG_FILE.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
                GSON.toJson(ModConfig.getInstance(), writer);
            }
            LOGGER.info("[ModOtimizacao] Configurações salvas em: {}", CONFIG_FILE.getFileName());
        } catch (Exception e) {
            LOGGER.error("[ModOtimizacao] Falha ao salvar modotimizacao.json.", e);
        }
    }

    /**
     * Copia os valores carregados para a instância singleton global.
     */
    private static void apply(ModConfig source) {
        ModConfig target = ModConfig.getInstance();
        target.pauseHiddenAnimations = source.pauseHiddenAnimations;
        target.enableBlockEntityCulling = source.enableBlockEntityCulling;
        target.blockEntityMaxDistance = source.blockEntityMaxDistance > 0 ? source.blockEntityMaxDistance : 64.0;
        target.blockEntityOcclusionCulling = source.blockEntityOcclusionCulling;
        target.enableEntityCulling = source.enableEntityCulling;
        target.entityMaxDistance = source.entityMaxDistance > 0 ? source.entityMaxDistance : 64.0;
        target.entityOcclusionCulling = source.entityOcclusionCulling;
        target.optimizeFontAndHud = source.optimizeFontAndHud;
        target.optimizeParticles = source.optimizeParticles;
        target.particleMaxDistance = source.particleMaxDistance > 0 ? source.particleMaxDistance : 48.0;
        target.maxParticlesPerBlock = source.maxParticlesPerBlock > 0 ? source.maxParticlesPerBlock : 35;
    }
}
