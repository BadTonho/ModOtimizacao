# Função 6 — Cache de Fontes & HUD

> Índice: [ARQUITETURA.md](../../ARQUITETURA.md) · ← anterior [05 Partículas](05-particulas.md) · próxima → [07 Config/GUI](07-configuracao-e-gui.md)

---

## O que esta função faz

Medições de largura de texto (`stringWidth(String)`) repetidas no HUD, chat, scoreboard e inventários são servidas por cache: evita **decomposição caractere a caractere e busca de métricas por glifo** dezenas/centenas de vezes por frame.

---

## Classes

| Classe | Papel (SRP) |
|---|---|
| `modules.gui.GuiOptimizationModule` | Gate puro → `ModConfig.optimizeFontAndHud`. |
| `modules.gui.FontWidthCache` | Cache por **instância de fonte**, com tetos de memória e invalidação dupla. |

---

## Fluxo

```
StringSplitter.stringWidth(String)F                          [2 hooks no mesmo método]
├─ @Inject HEAD cancellable: FontWidthCache.get(splitter, s)
│    └─ largura ≥ 0? → setReturnValue(width)       [módulo off → vanilla]
└─ @Inject RETURN: FontWidthCache.put(splitter, s, width)
```

### Estrutura do cache (fastutil, primitives)
```
IdentityHashMap<StringSplitter, Object2FloatOpenHashMap<String>>  INSTANCES
  MAX_INSTANCES = 8 instâncias de splitter (fonte)
  MAX_ENTRIES_PER_INSTANCE = 512 textos
  MAX_STRING_LENGTH = 256 (strings maiores não entram)
```
- `defaultReturnValue(-1.0f)` = "não está em cache" (larguras válidas nunca são negativas).
- Overflow por instância → `clear()` daquela instância (workflow simples e previsível).

---

## Invalidação (por que nunca mente)

1. **Primária (natural):** cada reload de recursos/idioma recria suas instâncias de `Font`/`StringSplitter` (`FontManager#createFont`) — os caches obsoletos ficam **órfãos** (nunca são consultados pela instância nova) e são reciclados pelo teto de 8 instâncias.
2. **Secundária (proativa):** `MixinFontManager` @Inject `RETURN` de `FontManager.reload(...)` → `FontWidthCache.clearAll()` — limpa qualquer residual entre recargas.

**Resultado:** larguras **nunca** são servidas de uma fonte usando métricas de outra (bug da versão pré-Fase 7 — cache global único).

---

## Integração com vanilla

| Mixin | Alvo | Ponto | Papel |
|---|---|---|---|
| `MixinStringSplitter` | `StringSplitter.stringWidth(String)F` | `@Inject HEAD` + `@Inject RETURN` | get (HEAD, cancellable) + put (RETURN) |
| `MixinFontManager` | `FontManager.reload(...)` | `@Inject RETURN` | invalidação proativa |

Escopo deliberado: **apenas a sobrecarga `stringWidth(String)`**. As sobrecargas de `FormattedText`/`FormattedCharSequence` não são cacheadas (chave de cache problemática com estilos dinâmicos) — documentado como candidato de Fase 9.

---

## Garantias de fidelidade (Regra 4)

- Cache serve **exatamente o valor que o vanilla teria computado** (put acontece quando o vanilla calcula real; get só devolve valor previamente calculado pela própria instância).
- OFF → hooks retornam sem efeito (HEAD não cancela; chamada vanilla segue).
- Tetos de memória explícitos (8 × 512 entradas) — sem crescimento descontrolado.

---

## Threading

Medições de HUD/telas rodam na **render/main thread**; `IdentityHashMap` não é thread-safe e é confinado a ela (documentado). Nada é tocado por workers.

---

## Custo

| Caminho | Trabalho |
|---|---|
| Hit | 1 lookup primário de string |
| Miss | cálculo vanilla + 1 put |

---

## Config

| Chave | Default | Descrição |
|---|---|---|
| `optimizeFontAndHud` | `true` | Liga/desliga o cache de larguras |
