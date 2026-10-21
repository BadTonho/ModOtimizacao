# Função 8 — Suite de Benchmark (tooling de dev)

> Índice: [ARQUITETURA.md](../../ARQUITETURA.md) · ← anterior [07 Config/GUI](07-configuracao-e-gui.md) · próxima → [09 Mixins](09-referencia-de-mixins.md)

---

## O que esta função faz

**Exclusiva de desenvolvimento:** medir o impacto numero do mod (e comparar com Vanilla/Sodium) de forma automática e isolada. **Nada do que está aqui vai para o JAR distribuído.**

---

## Garantias de isolamento

| Mecânica | Como |
|---|---|
| Código fora do JAR | `jar { exclude "com/modotimizacao/benchmark/**"; exclude "fpsrecorder*" }` no `build.gradle` |
| Mod auxiliar separado | `fpsrecorder.fabric.mod.json` + `fpsrecorder.mixins.json` próprios — mixin NUNCA registrado no `modotimizacao.mixins.json` principal |
| Build dedicado | task Gradle `buildFpsRecorder` produz `fps-recorder-1.0.0.jar` (benchmark/tools) com os três mixins |
| Script orquestrador | `benchmark.ps1` + `benchmark/collector.ps1`: instala/limpa mods por instância, roda cenário fixo, junta métricas, e estatísticas (média, 1% low = média do pior 1% das amostras) |
| Reprodutibilidade | `runVanilla.bat` (auto-reparo, benchmark "vanilla" desativa também resíduos de `build/resources/main`); aborto automático se o cliente não entra no mundo |

---

## Os 3 mixins do fps-recorder (Componentes da ferramenta auxiliar)

| Mixin | Alvo | Ponto | Papel |
|---|---|---|---|
| `MixinFpsRecorder` | `Minecraft.renderFrame` | `@Inject HEAD` | amostra FPS 1×/s em `benchmark_fps.csv` (`Timestamp,ElapsedSec,Fps,InstantFps`; `flush` + shutdown hook) — só amostra se `level`/`player` existem |
| `MixinLevelSummary` | `LevelSummary.shouldBackup` | `@Inject HEAD` *cancellable* | abre mundos pesados antigos sem diálogo de backup durante o benchmark |
| `MixinBackupConfirmScreen` | `BackupConfirmScreen.init` | `@Inject TAIL` | auto-confirma a tela de backup caso apareça (`onProceed.proceed(false, true)`) |

Ambos os hooks de backup são de **uso exclusivo do benchmark**: finalidade do QuickPlay em mundos de teste; zero efeito no mod real.

---

## Fluxo da suíte

```
benchmark.ps1 (por configuração/NVM: vanilla | sodium | nosso-mod)
  ├─ limpa/instala mods corretos no run/ (island de perfil isolado)
  ├─ lança cliente (QuickPlay direto no mundo de teste Stonehill Castle v1.4)
  ├─ captura por 60s (fps-recorder → benchmark_fps.csv)
  ├─ coleta (collector.ps1): média, 1% low (média do pior 1%), CPU/RAM/GPU/VRAM
  └─ limpa run/mods + mundo ao terminar; BENCHMARK_RESULTS.md atualizado manualmente
```

---

## Resultados oficiais

Ver [`BENCHMARK_RESULTS.md`](../../BENCHMARK_RESULTS.md) (GTX 1660 SUPER, 60 s, Stonehill Castle v1.4):
mod **1076.5 FPS** médio / **95.6** 1% low — vs Vanilla 953.2/99.8 e Sodium 1029.4/**35.0**.

---

## Config / Regras

- Nenhuma chave de configuração (tooling ativo só quando o JAR auxiliar está instalado).
- Regra 1 (segurança): CSVs/logs do benchmark ficam em pastas locais do `run/`, ignorados pelo Git; nenhum dado sensível é coletado.
