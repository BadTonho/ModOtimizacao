# 🗺️ Roadmap de Desenvolvimento

Plano de execução passo a passo para o desenvolvimento do mod de otimização client-side no Fabric para Minecraft 26.3.

---

## 📌 Visão Geral das Fases

```
[Fase 1: Alicerce] ➔ [Fase 2: Animações] ➔ [Fase 3: BlockEntity Culling] ➔ [Fase 4: Entity Culling] ➔ [Fase 5: HUD & Partículas] ➔ [Fase 6: Polimento & Config]
```

---

## 🏗️ Fase 1: Alicerce do Projeto (Setup & Arquitetura Base) — ✅ CONCLUÍDO
**Objetivo:** Ter o ambiente Fabric Loom pronto, compilável e estruturado para receber os módulos.

- [x] **Configuração do Gradle e Fabric Loom:**
  - `build.gradle` com suporte ao Java 25 / JVM moderna e Fabric Loom 1.18.4.
  - `gradle.properties` com as variáveis de versão (Minecraft 26.3, Loom, Loader 0.19.5).
  - `settings.gradle`.
- [x] **Metadados do Mod (`fabric.mod.json`):**
  - Mod ID definido como `modotimizacao`.
  - Definido estritamente como `"environment": "client"` (Regra 5).
- [x] **Configuração de Mixins (`modotimizacao.mixins.json`):**
  - Mapeamento configurado com `"compatibilityLevel": "JAVA_25"`.
- [x] **Esqueleto de Pastas e Pacotes (Regra 3):**
  - `com.modotimizacao.core`: Classe de inicialização `ModClientInitializer`.
  - `com.modotimizacao.config`: `ModConfig` desacoplado.
  - `com.modotimizacao.modules`: Subpastas `animations`, `culling/blockentity`, `culling/entity`.
  - `com.modotimizacao.util`: `FrustumUtil`.
- [x] **Compilação e Verificação:** Executado `./gradlew build` com sucesso gerando o JAR final.

---

## ⚡ Fase 2: Primeiro Módulo — Pausa de Animações Invisíveis — ✅ CONCLUÍDO
**Objetivo:** Reduzir consumo de banda da GPU pausando o upload de texturas animadas fora de visão.

- [x] Identificado o pipeline de animações no Minecraft 26.3 (`SpriteContents$AnimationState.needsToDraw()`, `drawToAtlas()` e RenderPearl).
- [x] Implementado o rastreamento duplo de visibilidade (`AnimationTracker` + `SectionAnimationContext`).
- [x] Mixins cirúrgicos criados para chunks (`CompiledSectionMesh`, `SectionCompiler`, `ModelBlockRenderer`, `FluidRenderer`, `LevelRenderer`) e entidades/GUI (`TextureAtlasSprite`).
- [x] Garantida 100% de paridade temporal vanilla (avanço de ticks contínuo, sem congelamento ao reentrar na visão).
- [x] Validada conformidade com a Regra 4 (Fidelidade Vanilla) e Regra 5 (Zero impacto no mundo e estritamente client-side).
- [x] Compilação `./gradlew build` executada com sucesso e empacotamento verificado.

---

## 📦 Fase 3: Culling de BlockEntities (Baús, Molduras e Placas) — ✅ CONCLUÍDO
**Objetivo:** Eliminar quedas de FPS em vilas e salas de baús descartando blocos fora de visão ou ocluídos por paredes.

- [x] Criado sub-módulo modular e desacoplado em `modules/culling/blockentity/` (`BlockEntityCuller`, `BlockEntityCullCache`, `BlockEntityCullingModule`, `ExemptBlockEntities`).
- [x] Implementado teste de Frustum Culling com coordenadas primitivas double e zero alocações via `MixinFrustumAccessor`.
- [x] Implementado teste duplo de oclusão: enclausuramento por 6 faces sólidas e raycast visual com cache de alta performance (`Long2BooleanOpenHashMap`) auto-expirável a cada 500ms.
- [x] Garantida paridade vanilla absoluta (Regra 4): isenção para Beacons, Portais do End, Gateways, blocos sendo quebrados (`breakProgress != null`), proximidade imediata e modo espectador.
- [x] Interceptado `BlockEntityRenderDispatcher.submit(...)` via `MixinBlockEntityRenderDispatcher` abortando o pipeline da GPU quando ocluído.
- [x] Opções integradas em `ModConfig` (`enableBlockEntityCulling`, `blockEntityMaxDistance`, `blockEntityOcclusionCulling`).
- [x] Compilação `./gradlew.bat build` validada no Java 25 com empacotamento no JAR verificado.

---

## 👥 Fase 4: Occlusion Culling de Entidades (Mobs e Jogadores) — ✅ CONCLUÍDO
**Objetivo:** Evitar o processamento na CPU de esqueletos/animações e envio à GPU de modelos de mobs escondidos atrás de blocos sólidos.

- [x] Criado sub-módulo modular e desacoplado em `modules/culling/entity/` (`EntityCuller`, `EntityCullCache`, `EntityCullingModule`, `ExemptEntities`).
- [x] Interceptação cirúrgica pós-frustum em `EntityRenderDispatcher.shouldRender(...)` via `MixinEntityRenderDispatcher` (`@At("RETURN")`), descartando a entidade antes de `LevelExtractor.extractEntity(...)`.
- [x] Implementado teste de linha de visão de 3 pontos (olhos/cabeça, centro do tronco e pés) via raycast visual (`ClipContext.Block.VISUAL`) com tolerância geométrica.
- [x] Cache rápido autolimpante (`EntityCullCache`) indexado por `entity.getId()` com ciclo de 100ms (10Hz) para resposta instantânea ao movimento.
- [x] Blindagem e paridade vanilla absoluta (Regra 4): isenções completas para efeito Glowing / Spectral Arrow, jogador local, câmera, veículos/montarias compartilhadas, bosses (Dragon, Wither, Elder Guardian), laços e modo espectador.
- [x] Integração completa no `ModConfig` (`enableEntityCulling`, `entityMaxDistance`, `entityOcclusionCulling`).
- [x] Compilação `./gradlew.bat build` validada no Java 25 com empacotamento no JAR verificado.

---

## 🎨 Fase 5: Otimização de Interface e Partículas — ✅ CONCLUÍDO
**Objetivo:** Melhorar a resposta de menus/inventários, aliviar alocações de memória de texto e evitar travamentos com partículas.

- [x] **Cache de Largura de Fontes/HUD:** Implementado `FontWidthCache` e `MixinStringSplitter` para acelerar medições de texto repetidas no HUD, menus e scoreboard, eliminando alocações de `MutableFloat` e decomposições de strings por frame.
- [x] **Culling de Partículas Ocluídas:** Implementado `ParticleCuller`, `ParticleCullCache` e `MixinQuadParticleGroup` para descartar partículas dentro de blocos sólidos ou atrás de paredes, com orçamento seguro de raycasts por frame (máximo 32 raycasts/frame) e proteção de proximidade (<= 3 blocos sempre visível).
- [x] **Limitador Adaptativo de Partículas:** Implementado `ParticleLimiter` e `MixinParticleEngine` para descartar excessos em explosões massivas (mais de 35 partículas/bloco por tick), prevenindo travamentos e lag spikes de GPU.
- [x] Opções integradas em `ModConfig` (`optimizeFontAndHud`, `optimizeParticles`, `particleMaxDistance`, `maxParticlesPerBlock`).
- [x] Compilação `./gradlew.bat build` validada no Java 25 com empacotamento no JAR verificado.

---

## ⚙️ Fase 6: Sistema de Configurações e Diagnóstico — ✅ CONCLUÍDO
**Objetivo:** Permitir ao jogador ligar/desligar cada recurso, personalizar distâncias e mensurar os ganhos diretamente no jogo.

- [x] **Persistência de Configurações JSON (`ConfigManager`):** Salva e carrega automaticamente as preferências em `.minecraft/config/modotimizacao.json` com formatação Gson legível (pretty-printing) e recuperação automática contra arquivos corrompidos ou ausentes.
- [x] **Tela Nativa de Opções In-Game (`ModConfigScreen`):** Tela interativa com botões de alternância (ON/OFF) e seletor cíclico de distância de culling (32, 64, 96 blocos), com salvamento automático ao fechar.
- [x] **Integração Orgânica às Configurações de Vídeo (`MixinVideoSettingsScreen`):** Botão largo e cabeçalho `⚡ Mod de Otimização` adicionado no rodapé da tela padrão de Gráficos do Minecraft 26.3 (`Esc -> Opções -> Gráficos...`), sem necessitar de mods auxiliares.
- [x] **Diagnóstico em Tempo Real no F3 (`MixinDebugScreenOverlay`):** Injeção de linha colorida de status no menu F3 indicando o estado ativo/inativo de cada subsistema de otimização (`Anims`, `BE-Cull`, `Ent-Cull`, `HUD`, `Part`).
- [x] Compilação `./gradlew.bat build` validada com sucesso no Java 25 LTS e Fabric Loom 1.18.4 com empacotamento completo no JAR verificado.

---

## 📋 Critérios de Aceite para Cada Etapa
1. Segue as 5 regras de [REGRAS_DO_PROJETO.md](file:///c:/Users/Admin/Desktop/ProjetosCode/minecraft/ModOtimização/REGRAS_DO_PROJETO.md).
2. Não quebra compilação nem gera warnings de Mixin.
3. Não altera nenhum dado de save, pacotes de rede ou lógica de servidor.
