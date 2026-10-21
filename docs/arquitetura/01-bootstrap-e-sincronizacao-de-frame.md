# Função 1 — Bootstrap & Sincronização por Frame

> Índice: [ARQUITETURA.md](../../ARQUITETURA.md) · Funções: [02 Animações](02-animacoes-inteligentes.md) · [03 BlockEntities](03-culling-blockentities.md) · [04 Entidades](04-culling-entidades.md) · [05 Partículas](05-particulas.md) · [06 Fontes/HUD](06-fontes-e-hud.md) · [07 Config/GUI](07-configuracao-e-gui.md) · [08 Benchmark](08-benchmark-dev.md) · [09 Mixins](09-referencia-de-mixins.md)

---

## O que esta função faz

Duas responsabilidades de "infraestrutura" que servem todas as outras funções do mod:

1. **Bootstrap (`core`):** ponto de entrada client-side único — registra o mod, carrega as configurações do disco.
2. **Sincronização por frame (`modules/core/FrameTracker`):** único ponto onde todo estado "por frame" é resetado no início do frame de renderização.

---

## Classes

| Classe | Papel (SRP) |
|---|---|
| `core.ModClientInitializer` | `ClientModInitializer` — loga o banner e chama `ConfigManager.load()`. Não conhece nenhuma função de otimização. |
| `modules.core.FrameTracker` | Dispatcher estático de frame-start: repassa o sinal aos subsistemas com estado por frame. |
| `modules.animations.AnimationTracker` | *(subsistema)* contador de epoch do frame + conjunto de sprites animados ativos. |
| `modules.particles.ParticleCullCache` | *(subsistema)* cache de oclusão de partículas com double-buffer trocado por frame. |

---

## Fluxo por frame

```
LevelRenderer.prepareChunkRenders (HEAD, 1× por frame, thread render/main)
  └─ MixinLevelRenderer → FrameTracker.onFrameStart()   ← chamada INCONDICIONAL
       ├─ AnimationTracker.onFrameStart()
       │    └─ frame++; ACTIVE_SPRITES.clear()
       └─ ParticleCullCache.onFrameStart()
            └─ frameId++; troca front ↔ back; front.clear()
```

Decisões de design importantes:

- **Incondicional:** o `FrameTracker` roda mesmo com todas as funções desligadas. Custo O(1) amortizado (contador + troca de dois mapas, com clear sempre sobre a *rodada anterior*, nunca sobre o frame em curso). Ligar/desligar os módulos fica nos *hot paths* dos próprios cullers — desligado = zero custo por frame.
- **Sem stale no cache de partículas:** ao trocar os buffers, o mapa *promovido a ativo* já nasce limpo; portanto nenhuma entrada de um frame anterior pode ser lida como se fosse do frame atual.
- **Extensibilidade:** nova função com estado por frame = acrescentar uma linha delegada em `FrameTracker.onFrameStart()` (o "como adicionar" está no índice).

---

## Integração com vanilla

| Mixin | Alvo | Ponto | Papel |
|---|---|---|---|
| `MixinLevelRenderer` | `LevelRenderer.prepareChunkRenders(Matrix4fc, boolean)` | `@Inject HEAD` | chama `FrameTracker.onFrameStart()` e, **se Anims ON**, varre `visibleSections` marcando sprites animados de meshes visíveis (ver [02](02-animacoes-inteligentes.md)) |

---

## Garantias (Regras 4 e 5)

- O frame-start **não altera nenhum comportamento vanilla**: é apenas bookkeeping interno (limpar caches). Se tudo estiver desligado, o frame vanilla é idêntico.
- Zero escrita de mundo/servidor; tudo roda na thread de render/main.

---

## Threading

- `FrameTracker.onFrameStart()` roda na **thread de render/main** (chamada por `prepareChunkRenders`).
- `AnimationTracker` usa `ConcurrentHashMap` porque sprites também são marcados fora do gate do FrameTracker (consultas `getU0`/`getV0` de entidades/GUI).
- `ParticleCullCache` é confinado à thread de render/main por construção (consulta e troca ocorrem sequencialmente nesse thread).

---

## Config

Nenhuma — infraestrutura sempre ativa (custo O(1)). O estado de `visibleSections` consumido pela varredura de animações continua dentro do gate `pauseHiddenAnimations`.
