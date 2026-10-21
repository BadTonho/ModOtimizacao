# Função 4 — Culling de Entidades (Mobs e Jogadores)

> Índice: [ARQUITETURA.md](../../ARQUITETURA.md) · ← anterior [03 BlockEntities](03-culling-blockentities.md) · próxima → [05 Partículas](05-particulas.md)

---

## O que esta função faz

Evita que entidades escondidas atrás de blocos sólidos (dentro de cavernas, paredes, fundações) gerem **cálculo ósseo/animação na CPU e draw calls na GPU**. A decisão roda **depois** do frustum vanilla (nunca o substitui), de forma cirúrgica: o mod apenas cancela o "sim" que o vanilla já aprovou quando provável que a entidade esteja invisível de verdade.

---

## Classes

| Classe | Papel (SRP) |
|---|---|
| `modules.culling.entity.EntityCullingModule` | Gate puro → `ModConfig.enableEntityCulling`. |
| `modules.culling.entity.EntityCuller` | Pipeline de decisão (proximidade → distância por hitbox → coluna de caverna). |
| `modules.culling.entity.ExemptEntities` | Isenções de fidelidade (Regra 4), lazy + `volatile`. |

---

## Fluxo de decisão (`EntityCuller.shouldCull`)

1. **Gate** módulo/entidade null → renderiza.
2. **Isenções (Regra 4), em ordem:**
   1. jogador local e `cameraEntity`;
   2. `player.isSpectator()` → todas as entidades visíveis (vanilla);
   3. `entity.isCurrentlyGlowing()` → Glowing/Spectral Arrow **sempre** atravessa paredes;
   4. veículos/montarias compartilhadas com o jogador (`isPassengerOfSameVehicle`, `getRootVehicle()`, `hasIndirectPassenger`);
   5. `Leashable.isLeashed()` (laço visual precisa existir);
   6. bosses: `ender_dragon`, `wither`, `elder_guardian` — `EntityType` resolvidos 1× do `BuiltInRegistries.ENTITY_TYPE` (lookup O(1)).
3. **Proximidade ponto-a-ponto** (semântica original): `distSq ≤ 6.25` (< 2.5 blocos, juntura de canto/porta) e `distSq ≤ 144.0` (< 12 blocos, combate) → nunca ocultar.
4. **Distância máxima por hitbox (Pacote 4/Fase 8):** ponto **mais próximo da AABB** da entidade (câmera *clampada* no envelope, zero alocação) vs `entityMaxDistance` — mobs grandes (Ghast, Ravager) não sofrem pop-in/out; teste **estritamente mais conservador** que ponto-a-ponto (só oculta quando a hitbox inteira passa do limite). Sentinela `1.0E9` ("Ilimitado") nunca oculta (dist² máxima do mundo ≈ 2.7E15 < 1.0E18).
5. **Coluna de caverna (`entityOcclusionCulling`):** só avaliada se `camY − entityY > 12`:
   - coluna vertical de blocos **acima da entidade** (start `Y+2`) **até o nível da câmera**, máx. **16 blocos** (mais alto que isso → *indecisão* → vanilla);
   - **1 fetch de `LevelChunk`** (`x>>4, z>>4` — coluna vertical nunca cruza chunks) e iteração direta `chunk.getBlockState`;
   - **todos** os blocos sólidos/opacos → oculto; qualquer abertura → renderiza;
   - cursor `EntityCuller.CURSOR` (`MutableBlockPos` estático) reutilizado — zero alocação por mob por frame.

---

## Integração com vanilla

| Mixin | Alvo | Ponto | Papel |
|---|---|---|---|
| `MixinEntityRenderDispatcher` | `EntityRenderDispatcher.shouldRender(E, Frustum, d, d, d, float)` | `@Inject RETURN`, *cancellable* | se o vanilla aprovou e o `EntityCuller` ocultar, `setReturnValue(false)` |

Vanilla já garante frustum + limites de distância de tracking; o mod **não duplica** esses testes — adiciona apenas oclusão sólida e o limite opcional do usuário.

---

## Garantias de fidelidade (Regra 4)

- **Sem cache entre frames (decisão deliberada):** `shouldRender` roda 1×/frame/entidade — cache intra-frame não ganha nada; entre frames criaria janela de *"mob invisível após quebrar a parede"* (violaria Regra 4). A otimização vive no scan (1 fetch de chunk; 16× menos resoluções de chunk por mob).
- Fail-safe do scan (coluna > 16) → vanilla.
- Todos os estados/efeitos que a gameplay usa para "ver" estão isentos.

---

## Threading

`shouldRender` roda na **thread de render/main**; cursores/estado confinados (documentados). `ExemptEntities.exemptBossTypes` usa `volatile` para a resolução lazy 1× de registro.

---

## Custo (por entidade, por frame)

| Caso | Trabalho |
|---|---|
| Isenta | O(1) — algumas buscas de campo |
| Dentro das proximidades/fora do alcance | O(1) — distância pura |
| Candidata a caverna | O(h) com h ≤ 16 consultas **no chunk já resolvido** (1 resolução de chunk) |

---

## Config

| Chave | Default | Descrição |
|---|---|---|
| `enableEntityCulling` | `true` | Liga/desliga o culling de entidades |
| `entityMaxDistance` | `1.0E9` | Distância máxima (**Ilimitado** = vanilla, medida por hitbox) |
| `entityOcclusionCulling` | `true` | Scan de coluna de caverna |
