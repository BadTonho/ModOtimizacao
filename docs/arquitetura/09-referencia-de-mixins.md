# Função 9 — Referência Completa de Mixins

> Índice: [ARQUITETURA.md](../../ARQUITETURA.md) · ← anterior [08 Benchmark](08-benchmark-dev.md)

Tabela técnica de **todos** os mixins do projeto. Os 17 principais estão registrados em `src/main/resources/modotimizacao.mixins.json` (`client[]`, `required: true`, `minVersion 0.8`, `compatibilityLevel JAVA_25`; todos alvos validados via `javap` contra o JAR oficial do Minecraft 26.3). Os 3 de tooling ficam em `fpsrecorder.mixins.json` e **fora do JAR principal**.

---

## 🎬 Animações — `mixin/animations/`

| Mixin | Alvo (classe) | Método | Tec. | Efeito |
|---|---|---|---|---|
| `MixinSectionCompiler` | `SectionCompiler` | `compile` | `@Inject HEAD` | inicia captura de sprites animados por seção (ThreadLocal) |
| `MixinModelBlockRenderer` | `ModelBlockRenderer` | `putQuadWithTint` | `@Inject HEAD` | registra sprite do quad animado |
| `MixinFluidRenderer` | `FluidRenderer` | `tesselate` | `@Inject HEAD` | registra sprites still/flowing animados |
| `MixinCompiledSectionMesh` | `CompiledSectionMesh` | `<init>` | `@Inject RETURN` | anexa Set capturado (duck `AnimatedSpriteHolder`) |
| `MixinLevelRenderer` | `LevelRenderer` | `prepareChunkRenders` | `@Inject HEAD` | `FrameTracker.onFrameStart()` + marca sprites de meshes visíveis |
| `MixinTextureAtlasSprite` | `TextureAtlasSprite` | `getU0`, `getV0` | `@Inject HEAD` ×2 | marca sprite consumido (entidades/GUI) |
| `MixinSpriteContentsAnimationState` | `SpriteContents$AnimationState` | `needsToDraw` | `@Inject HEAD` *cancellable* | **suprime upload à VRAM** do sprite invisível no frame |
| `MixinSpriteContents` | `SpriteContents` | `createAnimationState` | `@Inject RETURN` | liga `AnimationState` → `SpriteContents` (duck `SpriteHolder`) |

---

## 🛑 Culling de BlockEntities — `mixin/culling/blockentity/`

| Mixin | Alvo | Método | Tec. | Efeito |
|---|---|---|---|---|
| `MixinBlockEntityRenderDispatcher` | `BlockEntityRenderDispatcher` | `submit` | `@Inject HEAD` *cancellable* | cancela envio à GPU de BE invisível/ocluso |
| `MixinFrustumAccessor` | `Frustum` | `cubeInFrustum` | `@Invoker` | teste de cubo com primitivos (zero alocação) |

---

## 🎮 Culling de entidades — `mixin/culling/entity/`

| Mixin | Alvo | Método | Tec. | Efeito |
|---|---|---|---|---|
| `MixinEntityRenderDispatcher` | `EntityRenderDispatcher` | `shouldRender` | `@Inject RETURN` *cancellable* | cancela o "renderizar" aprovado pelo vanilla quando o EntityCuller oculta |

---

## 💨 Partículas — `mixin/particles/`

| Mixin | Alvo | Método | Tec. | Efeito |
|---|---|---|---|---|
| `MixinQuadParticleGroup` | `QuadParticleGroup` | `extractRenderState` | `@WrapOperation` (MixinExtras) na chamada `Frustum.pointInFrustum(DDD)` | vanilla avaliado primeiro; depois oculto por sólido/distância |

---

## 🖥️ GUI / Config — `mixin/gui/`

| Mixin | Alvo | Método | Tec. | Efeito |
|---|---|---|---|---|
| `MixinStringSplitter` | `StringSplitter` | `stringWidth(String)F` | `@Inject HEAD` *cancellable* + `@Inject RETURN` | get/put no `FontWidthCache` (por fonte) |
| `MixinFontManager` | `FontManager` | `reload` | `@Inject RETURN` | invalidação proativa do cache de fontes |
| `MixinVideoSettingsScreen` | `VideoSettingsScreen` | `addOptions` | `@Inject TAIL` | header ⚡ + botão de acesso às otimizações |
| `MixinDebugScreenOverlay` | `DebugScreenOverlay` | `extractLines(...)` | `@Inject HEAD` | linha de telemetria no F3 |

---

## 🧪 Tooling de benchmark — `benchmark/` (fora do JAR principal)

| Mixin | Alvo | Método | Tec. | Efeito |
|---|---|---|---|---|
| `MixinFpsRecorder` | `Minecraft` | `renderFrame` | `@Inject HEAD` | CSV 1×/s de FPS (telemetria) |
| `MixinLevelSummary` | `LevelSummary` | `shouldBackup` | `@Inject HEAD` *cancellable* | sem diálogo de backup durante benchmark |
| `MixinBackupConfirmScreen` | `BackupConfirmScreen` | `init` | `@Inject TAIL` | auto-confirma tela de backup |

---

## 📐 Convenções técnicas aplicadas

- **`@Inject`** é a técnica padrão (HEAD para gates, RETURN para captura/vínculo); *cancellable* somente quando o mod precisa sobrescrever (HEAD) ou ajustar (RETURN) o fluxo.
- **`@WrapOperation` (MixinExtras 0.5.5, embutido no Fabric Loader)** no único ponto potencialmente disputado por outros mods (`pointInFrustum` das partículas) — composição livre, sem disputa de PRIORIDADE entre mixins.
- **`@Invoker`** para acessar métodos privados do vanilla, compilando contra os nomes Mojang oficiais (sem camada intermediária de mappings).
- **Duck interfaces** (`SpriteHolder`, `AnimatedSpriteHolder`) adicionam estado às classes vanilla — o estado mora na própria instância, sem campos shadow duplicados em outra parte.
- **Zero `@Redirect`** (padrão introduzido na Fase 7 para interoperabilidade).
- **`defaultRequire = 1`** no `injectors`: se um hook falhar a aplicar, o jogo falha alto e cedo — nunca silenciosamente "otimizando" nada errado.

---

## 🔒 Regras de segurança de mixin (que sustentam a arquitetura)

1. **Nenhum alvo é instanciado/duplicado**; todo estado adicionado é @Unique e privado.
2. Todos os hooks **guardam atrás de gate** (`XModule.isEnabled()` ou checagem de null) → módulo off = vanilla integral.
3. Nenhum hook escreve estado de mundo/rede/saves (Regra 5).
4. Cada alvo foi confirmado existir no JAR 26.3 (consultas: `javap`); renames futuros do Minecraft são o risco auditado a cada update de versão.
