# Função 5 — Sistema de Partículas (Culling + Limitador)

> Índice: [ARQUITETURA.md](../../ARQUITETURA.md) · ← anterior [04 Entidades](04-culling-entidades.md) · próxima → [06 Fontes/HUD](06-fontes-e-hud.md)

---

## O que esta função faz

Duas vertentes complementares sobre partículas:

1. **Culling de renderização** — partículas dentro de blocos sólidos opacos (ou além da distância opcional) não vão para a GPU;
2. **Limitador adaptativo anti-lag** — no *spawn*, trava explosões/emissores que saturam um mesmo bloco (TNT, fogos, poções).

---

## Classes

| Classe | Papel (SRP) |
|---|---|
| `modules.particles.ParticleOptimizationModule` | Gate puro → `ModConfig.optimizeParticles`. |
| `modules.particles.ParticleCuller` | Decisão de occlusão por partícula (distância + estado do bloco). |
| `modules.particles.ParticleCullCache` | Cache real por frame do resultado de oclusão do bloco (double-buffer). |
| `modules.particles.ParticleLimiter` | Limitador de densidade no spawn (janela por tick, teto por bloco e global). |

---

## A. Culling na renderização

### Fluxo
```
QuadParticleGroup.extractRenderState
  └─ vanilla: frustum.pointInFrustum(x, y, z)   ← @WrapOperation envolve esta chamada
       └─ original.call() primeiro: fora do tronco → false (vanilla)
       └─ dentro do tronco → ParticleCuller.isOccluded(x, y, z)   [se Part ON]
            dist² ≤ 9.0 (≤3 blocos) → visível (proteção de juntura)
            dist² > maxDist²       → oculto
            estado do bloco (floor coords):
              - hit no cache → bit empacotado (zero getBlockState, zero alocação)
              - miss → level.getBlockState(POS reutilizado) → store(key, occluded)
```

### `ParticleCullCache` — o cache por frame (Fase 8)
- **Chave:** `BlockPos.asLong(bx, by, bz)`; **valor:** `(frameId << 1) | bitOclusao` — o frame é parte do dado; é impossível ler um valor de outro frame como se fosse atual.
- **Double-buffer:** dois `Long2LongOpenHashMap` (default `0L` = vazio; `frameId` começa em 1). A cada `onFrameStart` (via `FrameTracker`): swap + `front.clear()`; o frame em curso nunca paga a limpeza, e a memória é limitada a ~2 rodadas de blocos consultados (dezenas de KB em chuva extrema).
- **Motivação:** milhares de partículas/frame consultando o *mesmo* bloco (chuva, neve, explosões) — o mesmo bloco compartilha um único resultado por frame; hit rate altíssimo.

---

## B. Limitador adaptativo no spawn

```
ParticleEngine.add(Particle)  ← @Inject HEAD cancellable
  └─ ParticleLimiter.shouldThrottle(particle)
       1. isFeedbackParticle? CritParticle / NoteParticle / TotemParticle → nunca limita (Regra 4)
       2. checkTickReset(): janela alinhada ao gameTime do mundo (relógio de parede só fora de mundo, janela 50 ms)
       3. teto global: ++totalThisTick > 1000 → descarta
       4. densidade por bloco: centro da AABB em aritmética manual (sem getCenter()/Vec3);
          key = BlockPos.asLong; PARTICLES_PER_BLOCK (Long2IntOpenHashMap, limpo por tick)
          ≥ maxParticlesPerBlock (default 35) → descarta; senão incrementa e deixa passar
```

No 26.3, crit, dano-inimigo e enchanted-hit são a mesma classe `CritParticle` — a isenção cobre os três.

---

## Integração com vanilla

| Mixin | Alvo | Ponto | Papel |
|---|---|---|---|
| `MixinQuadParticleGroup` | `extractRenderState` → chamada `Frustum.pointInFrustum(DDD)` | `@WrapOperation` (MixinExtras) | envolve a decisão vanilla: primeiro o vanilla, depois a oclusão |
| `MixinParticleEngine` | `ParticleEngine.add(Particle)` | `@Inject HEAD`, *cancellable* | limita spawn em massa |

> `@WrapOperation` foi escolhido em vez de `@Redirect` justamente aqui: é o ponto mais provável de conflito com outros mods de partículas — MixinExtras (embutido no Fabric Loader) permite que vários mods envolvam a mesma chamada.

---

## Garantias de fidelidade (Regra 4)

- Partículas de **feedback de gameplay** jamais limitadas.
- Proximidade ≤ 3 blocos sempre visível (nunca "piscar" partículas na face do jogador).
- Cache válido por **no máximo 1 frame** (efeitos transitórios — diferença imperceptível) e efeito estritamente visual: **nada de estado de mundo/rede**.
- Fail-safe: módulo off → comportamento vanilla integral.

---

## Threading

Extração de grupos de partículas e `add()` rodam na thread **render/main**; os caches e o cursor `ParticleCuller.POS` (estático reutilizável) são confinados a essa thread — documentados e sem reentrância.

---

## Custo

| Caminho | Trabalho |
|---|---|
| Partícula ≤ 3 blocos | O(1) puro |
| Hit no cache | O(1) — consulta primária, sem alocação |
| Miss (bloco novo no frame) | 1 `getBlockState` + 1 write no mapa |
| Spawn (limiter) | O(1) por partícula |

---

## Config

| Chave | Default | Descrição |
|---|---|---|
| `optimizeParticles` | `true` | Liga/desliga as duas vertentes |
| `particleMaxDistance` | `1.0E9` | Distância máxima (**Ilimitado** = vanilla; ajustável via JSON) |
| `maxParticlesPerBlock` | `35` | Teto por bloco por tick (spawning) |

> Nota honesta: `particleMaxDistance` ainda não tem botão na GUI (ciclo de distância da tela cobre BlockEntities + entidades); ajuste direto no `modotimizacao.json`.
