# 📊 Suíte de Benchmark Automatizado — Mod de Otimização

Esta pasta contém a estrutura completa para execução de testes comparativos de desempenho automatizados entre:
1. **Minecraft Vanilla** (Baseline original)
2. **Minecraft + Sodium** (Se o jar for adicionado)
3. **Minecraft + Nosso Mod de Otimização**

---

## 📂 Onde colocar os arquivos:

* **Mundo Pesado:** Cole a pasta do seu mundo (que contém o `level.dat`) dentro de:
  📁 `benchmark/world/`
  *(Exemplo: `benchmark/world/MundoPesado/level.dat`)*

* **Mods Externos para Comparar (Opcional - ex: Sodium):** Se você tiver o arquivo `.jar` do Sodium ou outro mod para comparar, coloque-o em:
  📁 `benchmark/mods/`

---

## 🚀 Como executar o benchmark automatizado:

No PowerShell na raiz do projeto, execute:
```powershell
.\benchmark.ps1 -DurationSeconds 60
```

O script:
1. Clona o mundo pesado de forma idêntica para cada teste.
2. Inicia o **Vanilla**, monitora CPU, RAM, GPU e VRAM por 1 minuto e encerra.
3. Inicia o **Sodium** (se presente), monitora por 1 minuto e encerra.
4. Inicia o **Nosso Mod**, monitora por 1 minuto e encerra.
5. Gera automaticamente o relatório comparativo em `BENCHMARK_RESULTS.md`!
