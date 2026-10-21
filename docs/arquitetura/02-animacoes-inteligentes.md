# Função 2 — Animações Inteligentes (Pausa de Sprites Invisíveis)

> Índice: [ARQUITETURA.md](../../ARQUITETURA.md) · ← anterior [01 Bootstrap](01-bootstrap-e-sincronizacao-de-frame.md) · próxima → [03 BlockEntities](03-culling-blockentities.md)

---

## O que esta função faz

Texturas animadas (água corrente, lava, fogo, portal do Nether, faróis de dados dinâmicos) são **atualizadas na VRAM pelo vanilla a cada frame**. Esta função suprime a atualização dos sprites que **não participam do frame atual** (não estão em nenhum mesh de terreno visível, nem em entidades/GUI), eliminando uploads desnecessários à GPU — com **paridade temporal vanilla** (nenhuma animação "congela").

---

## Classes

| Classe | Papel (SRP) |
|---|---|
| `modules.animations.AnimationOptimizer` | Gate puro → `ModConfig.pauseHiddenAnimations`. |
| `modules.animations.AnimationTracker` | Epoch do frame (`currentFrame`) + `ACTIVE_SPRITES` (`ConcurrentHashMap`-backed Set). `markActive(sprite)` no frame; `isActive(sprite)` consultado por sprite animado. |
| `modules.animations.SectionAnimationContext` | `ThreadLocal<Set<SpriteContents>>`: captura dos sprites animados durante a **compilação paralela** de uma seção. |
| `modules.animations.AnimatedSpriteHolder` | Interface duck-typing injetada em `CompiledSectionMesh`: memória dos sprites animados contidos naquele mesh. |
| `modules.animations.SpriteHolder` | Interface duck-typing injetada em `SpriteContents$AnimationState`: back-reference do estado para o seu `SpriteContents` dono. |

---

## Fluxo completo

### A. Descoberta (build de chunk — thread worker)
```
SectionCompiler.compile (HEAD)
  └─ SectionAnimationContext.beginSection()          [reseta ThreadLocal]
ModelBlockRenderer.putQuadWithTint (HEAD, por quad)
  └─ sprite do quad animado? → SectionAnimationContext.recordSprite(contents)
FluidRenderer.tesselate (HEAD, por fluido)
  └─ materiais still/flowing animados? → recordSprite(contents)
CompiledSectionMesh.<init> (RETURN)
  └─ captureAndReset() → mesh guarda o Set (ou emptySet imutável = zero custo para seções estáticas)
```
Resultado: **cada mesh de terreno sabe quais sprites animados contém** — computado 1 vez por rebuild de seção, não por frame.

### B. Marcação por frame (thread render/main)
```
prepareChunkRenders (HEAD)
  └─ FrameTracker.onFrameStart() → AnimationTracker.onFrameStart()
  └─ para cada seção em visibleSections:
       mesh instanceof AnimatedSpriteHolder → getAnimatedSprites()
       → AnimationTracker.markActive(sprite)              [se Anims ON]
TextureAtlasSprite.getU0/getV0 (HEAD, qualquer consumo de sprite)
  └─ contents.isAnimated()? → markActive(contents)         [sempre — cobre entidades/GUI]
```

### C. Supressão (por sprite animado por frame)
```
SpriteContents$AnimationState.needsToDraw (HEAD, cancellable)
  └─ Anims ON && sprite vinculado && !AnimationTracker.isActive(sprite)
       → setReturnValue(false)  ⇒ este sprite não é atualizado na VRAM neste frame
```

### D. Vínculo estado ↔ sprite (1× na criação)
```
SpriteContents.createAnimationState (RETURN)
  └─ passou por SpriteHolder? → holder.setSprite(spriteContents)
```
Sem isto, `MixinSpriteContentsAnimationState` não saberia a qual sprite um dado `AnimationState` pertence.

---

## Integração com vanilla

| Mixin | Alvo | Ponto | Papel |
|---|---|---|---|
| `MixinSectionCompiler` | `SectionCompiler.compile(...)` | `@Inject HEAD` | inicia captura por seção |
| `MixinModelBlockRenderer` | `putQuadWithTint(...)` | `@Inject HEAD` | registra sprite do quad |
| `MixinFluidRenderer` | `FluidRenderer.tesselate(...)` | `@Inject HEAD` | registra sprites still/flowing |
| `MixinCompiledSectionMesh` | `CompiledSectionMesh.<init>` | `@Inject RETURN` | anexa o Set capturado ao mesh (duck `AnimatedSpriteHolder`) |
| `MixinLevelRenderer` | `prepareChunkRenders` | `@Inject HEAD` | marca sprites de meshes visíveis (via `FrameTracker`) |
| `MixinTextureAtlasSprite` | `getU0` / `getV0` | `@Inject HEAD` | marca sprites consumidos (entidades/GUI) |
| `MixinSpriteContentsAnimationState` | `SpriteContents$AnimationState.needsToDraw` | `@Inject HEAD`, *cancellable* | **suprime** o upload do sprite invisível |
| `MixinSpriteContents` | `createAnimationState(...)` | `@Inject RETURN` | liga `AnimationState` → `SpriteContents` |

---

## Garantias de fidelidade (Regra 4)

- **Paridade temporal:** o skip atua na *decisão de desenho* do frame; quando o sprite volta à visão, renderiza de novo — nenhuma fase de animação é perdida ou duplicada (o ROADMAP registra a "paridade temporal contínua").
- **Sem falsos negativos:** a marcação cobre os dois caminhos de consumo — meshes de terreno (varredura de `visibleSections`) **e** qualquer consumer de UV (entidades/GUI via `getU0`/`getV0`).
- **Custo zero quando desligada:** todos os hooks guardam atrás de `AnimationOptimizer.isEnabled()` (falso → HEAD injeta e retorna sem efeito colateral).

---

## Threading

- **A** (descoberta) roda nas **threads worker** de compilação de chunks → `ThreadLocal` isola a captura por worker.
- **B/C/D** rodam na thread **render/main**; `ACTIVE_SPRITES` é `ConcurrentHashMap` justamente porque recebe escritas entre threads (worker build → mesh publicado → leitura no mesmo frame e marcações de GUI).

---

## Config

| Chave | Default | Descrição |
|---|---|---|
| `pauseHiddenAnimations` | `true` | Liga/desliga a pausa de sprites invisíveis |
