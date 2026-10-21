# Função 7 — Configuração, GUI e Diagnóstico

> Índice: [ARQUITETURA.md](../../ARQUITETURA.md) · ← anterior [06 Fontes/HUD](06-fontes-e-hud.md) · próxima → [08 Benchmark](08-benchmark-dev.md)

---

## O que esta função faz

Governança do mod no runtime do jogador: **configuração (ModConfig)** → **persistência (ConfigManager)** → **interface (tela nativa + acesso pelo menu de Gráficos)** → **diagnóstico em tempo real (F3)**.

---

## Classes

| Classe | Papel (SRP) |
|---|---|
| `config.ModConfig` | Singleton de campos públicos — a **única fonte de verdade** dos gates (todos os `XModule.isEnabled()` derivam daqui). |
| `config.ConfigManager` | Persistência JSON (Gson pretty-printing) + cargas/normalização defensivas. |
| `gui.ModConfigScreen` | Tela nativa de opções: 7 toggles + 1 ciclo de distância + Concluído. |
| *(mixin)* `gui.MixinVideoSettingsScreen` | Injeta botão/cabeçalho na tela vanilla de Gráficos. |
| *(mixin)* `gui.MixinDebugScreenOverlay` | Linha de telemetria no F3. |

---

## Fluxo de configuração

```
Boot: ModClientInitializer.onInitializeClient → ConfigManager.load()
  ├─ arquivo ausente        → save() cria `.minecraft/config/modotimizacao.json` com defaults
  ├─ JSON válido            → apply(copiado para o singleton) com normalização defensiva
  ├─ JSON vazio/corrompido  → fallback defaults (save() restaurando padrão) — jogo nunca quebra
  └─ gating: cada módulo consulta o singleton no hot path (desligado = custo zero)

Uso: ModConfigScreen (7 toggles + ciclo) → onClose() → ConfigManager.save() → volta ao parent
```

### Normalização defensiva (anti-config-mal-intencionada)
- Distâncias: válidas se `0 < d < 1.0E8`; caso contrário → fallback (default *Ilimitado* `1.0E9`).
- `maxParticlesPerBlock ≤ 0` → `35`.

Esse contrato é o que **impossibilita** o Gson serializar `Infinity` (problema real histórico — por isso o sentinela é `1.0E9`, não `Double.POSITIVE_INFINITY`).

---

## Tela nativa (`ModConfigScreen`)

- Título `⚡ Configurações de Otimização`; grade 2×4 de botões:
  - `Smart Animations`, `Culling de Baús`, `Oclusão de Baús`, `Culling de Mobs`, `Oclusão de Mobs`, `Otimização HUD/Fontes`, `Culling de Partículas`
  - `Distância de Culling: …` — ciclo único aplicado a `blockEntityMaxDistance` **e** `entityMaxDistance`: *Ilimitado (Vanilla) → 96 → 64 → 32 → Ilimitado…* (valores fora do ciclo voltam ao início).
- `Concluído` → `save()` + retorno à tela anterior.
- Acesso orgânico: **⚡ Mod de Otimização** (header) + botão grande `Configurar Otimizações (FPS & Gráficos)...` injetados no `addOptions TAIL` do `VideoSettingsScreen` (mixin estende `OptionsSubScreen` para acesso ao `list`).

> Nota honesta: `particleMaxDistance` e `pauseHiddenAnimations`-relacionados exóticos não têm botão próprio (JSON-only para distância de partículas).

---

## Diagnóstico F3 (`MixinDebugScreenOverlay`)

`@Inject HEAD` em `extractLines(GuiGraphicsExtractor, List<String>, boolean, int)`: quando `left`, adiciona linha vazia + linha formatada:

```
§a[ModOtimizacao 26.3]§r Anims: ON | HUD: ON | BE-Cull: ON | Ent-Cull: ON | Part-Limit: ON
```

- Estados verdes (`§a`) / vermelhos (`§c`) direto dos campos do singleton — sempre verídicos (mesma fonte que os gates).
- Custo: só com F3 aberto, 1 `String.format` por frame (irrelevante).

---

## Integração com vanilla

| Mixin | Alvo | Ponto | Papel |
|---|---|---|---|
| `MixinVideoSettingsScreen` | `VideoSettingsScreen.addOptions` | `@Inject TAIL` | header + botão de acesso |
| `MixinDebugScreenOverlay` | `DebugScreenOverlay.extractLines(...)` | `@Inject HEAD` | telemetria no F3 |

---

## Garantias (Regras 4/5)

- A Configuração **não muda comportamento vanilla por si só** — só escolhe entre "com mod" e "vanilla integral".
- Escritas de disco limitadas a `config/modotimizacao.json` (padrão do Fabric Loader; nada toca mundos/saves).
- Falha de leitura/escrita → log de erro + jogo segue com defaults (nunca hard-crash por config).

---

## Threading

Telas/F3 executam na **thread main** do cliente (padrão vanilla). `save()` ocorre no `onClose` da tela; `load()` no próprio boot de inicialização.

---

## Config (o próprio objeto)

| Chave | Default |
|---|---|
| `pauseHiddenAnimations` | `true` |
| `enableBlockEntityCulling` | `true` |
| `blockEntityMaxDistance` | `1.0E9` |
| `blockEntityOcclusionCulling` | `true` |
| `enableEntityCulling` | `true` |
| `entityMaxDistance` | `1.0E9` |
| `entityOcclusionCulling` | `true` |
| `optimizeFontAndHud` | `true` |
| `optimizeParticles` | `true` |
| `particleMaxDistance` | `1.0E9` |
| `maxParticlesPerBlock` | `35` |
