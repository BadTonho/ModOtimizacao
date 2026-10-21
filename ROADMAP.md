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
  - `com.modotimizacao.modules`: Subpastas `animations`, `culling/blockentity`, `culling/entity` (+ `gui` e `particles` nas fases seguintes).
  - `com.modotimizacao.util`: `FrustumUtil` (removido posteriormente na Fase 7 por código morto).
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

- [x] Criado sub-módulo modular e desacoplado em `modules/culling/blockentity/` (`BlockEntityCuller`, `BlockEntityCullingModule`, `ExemptBlockEntities`).
- [x] Implementado teste de Frustum Culling com coordenadas primitivas double e zero alocações via `MixinFrustumAccessor`.
- [x] Implementado teste de oclusão 100% O(1), zero raycasts: enclausuramento pelas 6 faces sólidas **+ anel externo ±1 (26 células)** para proteger camas, baús duplos e modelos multi-bloco.
- [x] Garantida paridade vanilla absoluta (Regra 4): isenção para Beacons, Portais do End, Gateways, blocos sendo quebrados (`breakProgress != null`), proximidade imediata e modo espectador.
- [x] Interceptado `BlockEntityRenderDispatcher.submit(...)` via `MixinBlockEntityRenderDispatcher` abortando o pipeline da GPU quando ocluído.
- [x] Opções integradas em `ModConfig` (`enableBlockEntityCulling`, `blockEntityMaxDistance` default **Ilimitado**, `blockEntityOcclusionCulling`).
- [x] Compilação `./gradlew.bat build` validada no Java 25 com empacotamento no JAR verificado.

---

## 👥 Fase 4: Occlusion Culling de Entidades (Mobs e Jogadores) — ✅ CONCLUÍDO
**Objetivo:** Evitar o processamento na CPU de esqueletos/animações e envio à GPU de modelos de mobs escondidos atrás de blocos sólidos.

- [x] Criado sub-módulo modular e desacoplado em `modules/culling/entity/` (`EntityCuller`, `EntityCullingModule`, `ExemptEntities`).
- [x] Interceptação cirúrgica pós-frustum em `EntityRenderDispatcher.shouldRender(...)` via `MixinEntityRenderDispatcher` (`@At("RETURN")`), descartando a entidade antes da extração para a GPU.
- [x] Teste de oclusão de cavernas 100% O(coluna), zero raycasts: escaneia a coluna completa de blocos acima da entidade até o nível da câmera (máx. 16 blocos, fail-safe vanilla quando indeciso).
- [x] Blindagem e paridade vanilla absoluta (Regra 4): isenções completas para efeito Glowing / Spectral Arrow, jogador local, câmera, veículos/montarias compartilhadas, bosses (Ender Dragon, Wither, Elder Guardian), laços e modo espectador.
- [x] Integração completa no `ModConfig` (`enableEntityCulling`, `entityMaxDistance` default **Ilimitado**, `entityOcclusionCulling`).
- [x] Compilação `./gradlew.bat build` validada no Java 25 com empacotamento no JAR verificado.

---

## 🎨 Fase 5: Otimização de Interface e Partículas — ✅ CONCLUÍDO
**Objetivo:** Melhorar a resposta de menus/inventários, aliviar alocações de memória de texto e evitar travamentos com partículas.

- [x] **Cache de Largura de Fontes/HUD:** Implementado `FontWidthCache` e `MixinStringSplitter` para acelerar medições de texto repetidas no HUD, menus e scoreboard. Cache por **instância de fonte** (IdentityHashMap) com invalidação no reload de recursos (`MixinFontManager`), eliminando alocações de `MutableFloat` e decomposições de strings por frame.
- [x] **Culling de Partículas Ocluídas:** Implementado `ParticleCuller` e `MixinQuadParticleGroup` (`@WrapOperation`/MixinExtras) para descartar partículas dentro de blocos sólidos ou fora da distância configurada, 100% O(1) e zero raycasts, com proteção de proximidade (<= 3 blocos sempre visível).
- [x] **Limitador Adaptativo de Partículas:** Implementado `ParticleLimiter` e `MixinParticleEngine` para descartar excessos em explosões massivas (mais de 35 partículas/bloco por tick), com janela alinhada ao tick do jogo e **isenções de fidelidade** para CritParticle (crit/dano/enchanted-hit), NoteParticle e TotemParticle.
- [x] Opções integradas em `ModConfig` (`optimizeFontAndHud`, `optimizeParticles`, `particleMaxDistance` default **Ilimitado**, `maxParticlesPerBlock`).
- [x] Compilação `./gradlew.bat build` validada no Java 25 com empacotamento no JAR verificado.

---

## ⚙️ Fase 6: Sistema de Configurações e Diagnóstico — ✅ CONCLUÍDO
**Objetivo:** Permitir ao jogador ligar/desligar cada recurso, personalizar distâncias e mensurar os ganhos diretamente no jogo.

- [x] **Persistência de Configurações JSON (`ConfigManager`):** Salva e carrega automaticamente as preferências em `.minecraft/config/modotimizacao.json` com formatação Gson legível (pretty-printing) e recuperação automática contra arquivos corrompidos ou ausentes.
- [x] **Tela Nativa de Opções In-Game (`ModConfigScreen`):** Tela interativa com botões de alternância (ON/OFF) e seletor cíclico de distância de culling (**Ilimitado (Vanilla)** → 96 → 64 → 32), com salvamento automático ao fechar.
- [x] **Integração Orgânica às Configurações de Vídeo (`MixinVideoSettingsScreen`):** Botão largo e cabeçalho `⚡ Mod de Otimização` adicionado no rodapé da tela padrão de Gráficos do Minecraft 26.3 (`Esc -> Opções -> Gráficos...`), sem necessitar de mods auxiliares.
- [x] **Diagnóstico em Tempo Real no F3 (`MixinDebugScreenOverlay`):** Injeção de linha colorida de status no menu F3 indicando o estado ativo/inativo de cada subsistema de otimização (`Anims`, `BE-Cull`, `Ent-Cull`, `HUD`, `Part`).
- [x] Compilação `./gradlew.bat build` validada com sucesso no Java 25 LTS e Fabric Loom 1.18.4 com empacotamento completo no JAR verificado.

---

## 🔧 Fase 7: Auditoria Completa e Correções de Integridade — ✅ CONCLUÍDO
**Objetivo:** Eliminar bugs, falsos positivos e incompatibilidades mapeados por uma auditoria completa (Código, Mixins × JAR real 26.3, scripts de benchmark e documentação).

- [x] **Registro de Mixins:** Os 8 mixins da Fase 2 (animções) e o `gui.MixinStringSplitter` estavam compilados porém **não registrados** em `modotimizacao.mixins.json` (recursos da Fase 2/5 executavam como código morto na prática). Todos registrados; cada alvo validado via `javap` contra o JAR oficial do Minecraft 26.3.
- [x] **FontWidthCache:** chave por instância de fonte (`StringSplitter`), invalidação via `MixinFontManager` (`FontManager.reload`) e teto de memória (8 instâncias × 512 textos). Antes, larguras cruzavam entre fontes e o cache nunca invalidava.
- [x] **Fidelidade Vanilla (Regra 4) nos defaults:** distâncias de culling de BlockEntities/entidades/partículas agora iniciam **Ilimitadas** (sentinela 1.0E9); o jogador opta por restringir conscientemente na GUI (ciclo Ilimitado → 96 → 64 → 32).
- [x] **Falsos positivos corrigidos:** oclusão de cavernas por coluna completa (antes testava 1 único bloco); enclausuramento de BlockEntities com margem de anel ±1 (antes escondia camas/modelos multi-bloco).
- [x] **Código morto removido:** `EntityCullCache`, `BlockEntityCullCache`, `ParticleCullCache`, `FrustumUtil`, flags/gates duplicados e imports órfãos (javadocs agora descrevem o comportamento real).
- [x] **MixinExtras:** `@Redirect` substituído por `@WrapOperation` no culling de partículas — composível com outros mods (livre de colisão).
- [x] **Benchmark íntegro:** teste "Vanilla" desativa também o resíduo compilado (`build/resources/main`); a suíte limpa `run/mods`/mundo no fim; `runVanilla.bat` com auto-reparo; 1% Low corrigido (média dos 1% piores, não 1 amostra); fallback de processo restrito; aborto quando o cliente não entra no mundo.
- [x] **Granularidade de performance:** zero alocação por entidade/BE/frame (sem `new Vec3`, `Direction[]` estático, `Set` de isenções pré-resolvido, distância calculada 1×).
- [x] **Manifestação de limites (ferramenta fps-recorder):** satisfaz contexto de benchmark separado do JAR principal; validada a existência de `BackupConfirmScreen.onProceed` no JAR 26.3 via `javap`.
- [x] **F3 completo:** linha de diagnóstico com `Anims/HUD/BE-Cull/Ent-Cull/Part-Limit` (antes faltava 2/2).
- [x] **LICENSE LGPL-3.0** incluída no repositório e no JAR; `gradle.properties` sincronizado (removida a variável yarn_mappings morta; corrigida a propriedade `archives_base_name` no Gradle 9).

**Ferramentas da auditoria:** `gradlew clean build` (Java 25), `javap` contra o JAR oficial `minecraft-merged-26.3-*.jar`, parser de sintaxe PowerShell e revisão linha a linha dos 45 arquivos Java.

---

## ⚡ Fase 8: Otimizações de Runtime e Workflow — ✅ CONCLUÍDO
**Objetivo:** Segunda análise completa, focada em melhorias e otimizações: cache real por frame, eliminação de alocações residuais no hot loop, culling de entidades por hitbox e workflow de build mais rápido.

- [x] **Workflow de build:** `org.gradle.parallel` + `org.gradle.caching` + `org.gradle.configuration-cache` ativados (build reusa cache de configuração em ~1s); compatibilidade do configuration cache garantida em `:jar`/`:processResources` (closures de copy-spec capturadas em tempo de configuração, nunca em execução); plugin `maven-publish` sem configuração removido.
- [x] **`FrameTracker` (`modules/core/`):** ponto único de sincronização por frame via `MixinLevelRenderer.prepareChunkRenders` (chamada incondicional, custo amortizado O(1)); coordena `AnimationTracker` (sprites animados visíveis) + `ParticleCullCache` (oclusão de partículas), ambos com estado exclusivo da thread de render/main.
- [x] **`ParticleCullCache` real (Fase 8):** o conceito removido na Fase 7 por ser órfão foi refeito **com uso real**: consultado por `ParticleCuller.isOccluded` — chave `BlockPos.asLong`, valor empacotado `(frameId << 1) | bitOclusao` (resultado "stale" impossível no cache ativo), double-buffer trocado por frame (memória limitada a ~2 rodadas, clear amortizado nunca pago no frame em curso). Partículas do mesmo bloco no mesmo frame compartilham UMA consulta `getBlockState` (hit rate alto em chuva/neve/explosões).
- [x] **Coluna de caverna com 1 fetch de chunk (`EntityCuller`):** coluna vertical nunca cruza chunks (x/z fixos) — `LevelChunk` resolvido UMA vez e seções iteradas via `chunk.getBlockState` (16× menos resoluções de chunk por mob subterrâneo; comportamento idêntico ao `level.getBlockState`).
- [x] **Zero alocações residuais (Pacote 3):** `ParticleLimiter` sem `box.getCenter()` (centro da AABB em matemática manual `(min+max)*0.5`); `ParticleCuller`/`EntityCuller`/`BlockEntityCuller` com `MutableBlockPos` estáticos reutilizáveis e aritmética manual para faces/anel ±1 (6 a 32 alocações a menos por BlockEntity por frame), todos documentados como confinados à thread de render/main.
- [x] **Culling de entidades por hitbox (Pacote 4):** o limite de distância mede o PONTO MAIS PRÓXIMO da AABB (clamp da câmera no envelope, zero alocação) — anti pop-in/out para mobs grandes (Ghast, Ravager); teste estritamente mais conservador (só oculta quando a hitbox inteira passa do limite); proximidade 2.5/12 blocos preservada ponto-a-ponto.
- [x] **Decisão deliberada (fidelidade, Regra 4):** NÃO existe cache de entidades entre frames — `shouldRender` roda 1×/frame/entidade (cache intra-frame não ganha nada) e cache inter-frame exigiria invalidação por atualização de bloco (janela de "stale invisível" após quebrar paredes). Em vez disso, o scan interno ganhou o fetch único de chunk acima.
- [x] Compilação `./gradlew.bat build` validada no Java 25 com configuration cache e empacotamento no JAR verificado.

---

## 📋 Critérios de Aceite para Cada Etapa
1. Segue as 5 regras de [REGRAS_DO_PROJETO.md](file:///c:/Users/Admin/Desktop/ProjetosCode/minecraft/ModOtimização/REGRAS_DO_PROJETO.md).
2. Não quebra compilação nem gera warnings de Mixin.
3. Não altera nenhum dado de save, pacotes de rede ou lógica de servidor.
