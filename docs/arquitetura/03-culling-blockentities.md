# Função 3 — Culling de BlockEntities

> Índice: [ARQUITETURA.md](../../ARQUITETURA.md) · ← anterior [02 Animações](02-animacoes-inteligentes.md) · próxima → [04 Entidades](04-culling-entidades.md)

---

## O que esta função faz

Impede que BlockEntities (baús, placas, molduras de portal, toras…) sejam **enviados à GPU** quando estão fora do campo de visão ou escondidos atrás de blocos sólidos — em bases densas o vanilla as renderiza mesmo sem visibilidade. Pipeline em camadas, tudo O(1) e **zero raycasts**.

---

## Classes

| Classe | Papel (SRP) |
|---|---|
| `modules.culling.blockentity.BlockEntityCullingModule` | Gate puro → `ModConfig.enableBlockEntityCulling`. |
| `modules.culling.blockentity.BlockEntityCuller` | Pipeline de decisão (distância → frustum → enclausuramento). |
| `modules.culling.blockentity.ExemptBlockEntities` | Isenções de fidelidade (Regra 4), resolvidas lazy por identidade de registro. |

---

## Fluxo de decisão (`BlockEntityCuller.shouldCull`)

1. **Gate:** módulo desligado / `state`/`blockPos`/`cameraState` null → renderiza (fail-safe).
2. **Isenções (Regra 4):**
   - `state.breakProgress != null` → bloco sendo **quebrado/minerado** — a animação de progresso precisa ser vista;
   - tipos isentos: `beacon`, `end_gateway`, `end_portal`, `structure_block` (`Set` lazy + `volatile`, lookup O(1) por identidade do registro — sem `String.contains` por frame).
3. **Proximidade (< 2 blocos; `distSq ≤ 4.0`):** nunca ocluir — evita cortes visuais ao virar esquinas.
4. **Distância máxima configurable** (default *Ilimitado* `1.0E9`): centro do bloco (+0.5) vs câmera.
5. **Frustum com margem:** invoca `Frustum.cubeInFrustum(DDDDDD)` **privado** do vanilla via `@Invoker` (`MixinFrustumAccessor`) com primitivos (zero alocação) sobre a caixa expandida `−1…+2` — protege baús duplos e camas; resultado: fora do tronco (`≠ −2 INSIDE, ≠ −1 INTERSECT`) → oculto.
6. **Enclausuramento sólido (config `blockEntityOcclusionCulling`):**
   - espectador → vê através de paredes (vanilla);
   - **as 6 faces adjacentes** precisam ser sólidas/opacas;
   - **anel ±1 de 26 células** ao redor, idem — protege modelos que se projetam no bloco vizinho (camas, baús, placas grandes de mods);
   - qualquer abertura → renderiza (conservador).
7. pontos 5 e 6 consultam `level` via `Minecraft.getInstance()`; lock de proximidade/distância sem alocação (aritmética pura).

---

## Integração com vanilla

| Mixin | Alvo | Ponto | Papel |
|---|---|---|---|
| `MixinBlockEntityRenderDispatcher` | `BlockEntityRenderDispatcher.submit(S, PoseStack, SubmitNodeCollector, CameraRenderState)` | `@Inject HEAD`, *cancellable* | cancela o envio à GPU quando invisível |
| `MixinFrustumAccessor` | `Frustum.cubeInFrustum` | `@Invoker` | acesso ao teste "cubo vs tronco" com primitivos |

---

## Garantias de fidelidade (Regra 4)

- Isenções explícitas (efeitos luminosos/portal/quebra/estrutura) — itens com efeitos especiais nunca desaparecem.
- Margens protetoras (frustum `−1/+2` e anel ±1) eliminam falsos positivos de modelos multi-bloco.
- Inconcluso → **vanilla** (renderiza).

---

## Threading

`submit` roda na thread de **render/main**. O cursor estático reutilizável `BlockEntityCuller.NEIGHBOR` é confinado a essa thread (documentado por javadoc; sem reentrância durante o teste de 6 faces/anel).

---

## Custo

| Caso | Consultas de bloco/frame por BE |
|---|---|
| Fora do frustum/fora da distância | 0 |
| Caixa aberta (faces testadas) | 6 (rótulo: interrompe na 1ª abertura) |
| Caixa fechada (falso candidato) | 6 + até 26 (anel) |

---

## Config

| Chave | Default | Descrição |
|---|---|---|
| `enableBlockEntityCulling` | `true` | Liga/desliga o culling de BlockEntities |
| `blockEntityMaxDistance` | `1.0E9` | Distância máxima (**Ilimitado** = vanilla) |
| `blockEntityOcclusionCulling` | `true` | Teste de enclausuramento (6 faces + anel ±1) |
