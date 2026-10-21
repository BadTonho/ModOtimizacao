# 🏗️ Arquitetura do Mod de Otimização

Documentação técnica da arquitetura, **separada por funções**: cada vertente de otimização tem seu próprio documento em [`docs/arquitetura/`](docs/arquitetura/), seguindo a Regra 3 (modularidade) também na documentação.

---

## 🧩 Mapa de funções

| # | Função | Documento |
|---|---|---|
| 1 | **Bootstrap & sincronização por frame** | [01-bootstrap-e-sincronizacao-de-frame.md](docs/arquitetura/01-bootstrap-e-sincronizacao-de-frame.md) |
| 2 | **Animações inteligentes** (pausa de sprites invisíveis) | [02-animacoes-inteligentes.md](docs/arquitetura/02-animacoes-inteligentes.md) |
| 3 | **Culling de BlockEntities** | [03-culling-blockentities.md](docs/arquitetura/03-culling-blockentities.md) |
| 4 | **Culling de entidades** (mobs e jogadores) | [04-culling-entidades.md](docs/arquitetura/04-culling-entidades.md) |
| 5 | **Sistema de partículas** (culling + limitador) | [05-particulas.md](docs/arquitetura/05-particulas.md) |
| 6 | **Fontes & HUD** (cache de medição de texto) | [06-fontes-e-hud.md](docs/arquitetura/06-fontes-e-hud.md) |
| 7 | **Configuração & GUI** (tela nativa, JSON, F3) | [07-configuracao-e-gui.md](docs/arquitetura/07-configuracao-e-gui.md) |
| 8 | **Benchmark** (tooling de dev, fora do JAR) | [08-benchmark-dev.md](docs/arquitetura/08-benchmark-dev.md) |
| 9 | **Referência completa de mixins** (tabela técnica) | [09-referencia-de-mixins.md](docs/arquitetura/09-referencia-de-mixins.md) |

---

## 🎬 O pipeline por frame (visão consolidada)

Todos os pontos de integração do mod no frame de renderização (thread render/main):

```
Frame de renderização (1× por frame)
│
├─ 1. LevelRenderer.prepareChunkRenders — MixinLevelRenderer @Inject HEAD
│      └─ FrameTracker.onFrameStart()   [incondicional]
│           ├─ AnimationTracker.onFrameStart()      → zera sprites animados ativos
│           └─ ParticleCullCache.onFrameStart()     → troca double-buffer de oclusão
│      └─ Varredura de visibleSections              [se Anims ON]
│           └─ marcar sprites animados dos meshes visíveis (AnimationTracker.markActive)
│
├─ 2. EntityRenderDispatcher.shouldRender — MixinEntityRenderDispatcher @Inject RETURN
│      └─ EntityCuller.shouldCull                    [se Ent-Cull ON]
│           isenções → proximidade → distância por hitbox → coluna de caverna
│
├─ 3. BlockEntityRenderDispatcher.submit — MixinBlockEntityRenderDispatcher @Inject HEAD
│      └─ BlockEntityCuller.shouldCull               [se BE-Cull ON, cancela o envio]
│           isenções → distância → frustum (margem) → enclausuramento (6 faces + anel ±1)
│
├─ 4. QuadParticleGroup.extractRenderState — MixinQuadParticleGroup @WrapOperation
│      └─ ParticleCuller.isOccluded                  [se Part-Limit ON]
│           proximidade ≤3 → distância → estado do bloco servido pelo ParticleCullCache
│
├─ 5. SpriteContents$AnimationState.needsToDraw — MixinSpriteContentsAnimationState @Inject HEAD
│      └─ AnimationTracker.isActive                  [se Anims ON]
│           sprite não marcado no frame → false → upload à VRAM pulado
│
└─ 6. (ciclos de spawn, fora do frame gráfico)
       └─ ParticleEngine.add — MixinParticleEngine @Inject HEAD   [se Part-Limit ON]
            └─ ParticleLimiter.shouldThrottle (janela por tick, teto por bloco e global)
```

> **Nota de ordem:** o diagrama lista os *pontos de integração*, não uma ordem estrita de execução — cada um roda no momento vanilla correspondente ao seu método interceptado.

---

## 🧭 Princípios cross-cutting

### Regra 4 — Fidelidade vanilla (em toda função)
- **Nada é removido ou desativado:** o mod adiciona checagens *depois* (ou *antes*, de forma conservadora) das decisões vanilla; nunca sobrepõe um verdicto vanilla de "renderizar" com destruição de funcionalidade.
- **Isenções em todas as camadas de culling:** jogador/espectador, Glowing, veículos, laços, bosses, beacons/portais, partículas de feedback (crit/nota/totem).
- **Defaults "Ilimitado"** (sentinela `1.0E9`): qualquer restrição extra só existe porque o usuário escolheu.
- **Fail-safe:** testes inconclusivos (ex.: coluna de caverna acima do limite) → comporta-se como vanilla.

### Regra 5 — Client-side estrito (em toda função)
- **Zero escrita de estado de mundo/servidor/saves/pacotes.** Toda a arquitetura é leitura (`getBlockState`, consultas de câmera/frustum/região) + decisão de renderização.
- `"environment": "client"` no `fabric.mod.json`; entrypoint único `ModClientInitializer implements ClientModInitializer`.

### Zero alocação no hot loop
- Cursores mutáveis reutilizáveis e estáticos, confinados à thread de render (`ParticleCuller.POS`, `EntityCuller.CURSOR`, `BlockEntityCuller.NEIGHBOR`), aritmética manual em vez de `relative()/offset()/getCenter()`.
- Caches com valores empacotados/primivos (fastutil: `Long2LongOpenHashMap`, `Long2IntOpenHashMap`, `Object2FloatOpenHashMap`).

### Confinação de thread (contrato de estado)
| Estado | Thread(s) | Proteção |
|---|---|---|
| `ParticleCullCache` (double-buffer) | render/main (extração) | confinada + frame embutido no valor |
| `AnimationTracker.ACTIVE_SPRITES` | escrita/leitura entre render e workers | `ConcurrentHashMap` |
| `SectionAnimationContext` (captura por seção) | workers de compilação de chunk | `ThreadLocal` |
| Cursores mutáveis (`POS`/`CURSOR`/`NEIGHBOR`) | render/main, sem reentrância | confinada, documentada por javadoc |
| `FontWidthCache` (mapa por instância) | render/main (medições de HUD) | confinada |
| Isenções lazy (`ExemptEntities`/`ExemptBlockEntities`) | primeira leitura após freeze de registros | `volatile` set imutável |

### Configuração como única fonte de gate
Cada função expõe ` ClasseModule.isEnabled()` que devolve **exclusivamente** o campo do `ModConfig` (sem estado duplicado ou caches de gate órfãos — lição da auditoria da Fase 7). O mixin chama o gate no *hot path*, de forma que desligar uma função zera seu custo por frame.

---

## 🔩 Camada mixin (como tudo se liga ao vanilla)

- **17 mixins principais** registrados em `src/main/resources/modotimizacao.mixins.json` (todos alvos verificados contra o JAR oficial do Minecraft 26.3 via `javap`).
- **3 mixins de tooling** (`benchmark/`, `fpsrecorder.mixins.json`): exclusivos do pacote de benchmark, **excluídos do JAR principal**.
- **`@WrapOperation` (MixinExtras)** no ponto de conflito potencial com outros mods (partículas) — composição livre; `@Inject` e `@Invoker` nos demais; duck-typing interfaces (`SpriteHolder`, `AnimatedSpriteHolder`) para estado adicional em classes vanilla.

Detalhe por detalhe: [09-referencia-de-mixins.md](docs/arquitetura/09-referencia-de-mixins.md).

---

## ➕ Como adicionar uma nova função (padrão do projeto)

1. **Pasta própria** em `modules/<funcao>/` com três papéis: `XModule` (gate puro), `XCuller`/`XOptimizer` (lógica), `ExemptX` quando aplicável;
2. **Mixin cirúrgico** em `mixin/<funcao>/` com alvo validado (`javap` no JAR 26.3) e registro em `modotimizacao.mixins.json`;
3. **Flag** no `ModConfig` (default fiel — ex.: distâncias *Ilimitadas*), botão na `ModConfigScreen` quando interativo, e linha na F3 (`MixinDebugScreenOverlay`);
4. **Estado por frame:** registrá-lo no `FrameTracker.onFrameStart()` (troca de buffer/limpeza amortizada);
5. **Documentação:** novo arquivo em `docs/arquitetura/` + entrada no mapa de funções desta página + sync com IDEIA/ROADMAP (Regra 2).
