# ⚡ Mod de Otimização (ModOtimizacao)

Mod de otimização **client-side** de alto desempenho para **Minecraft 26.3**. Maximiza FPS, estabiliza frametimes (1% e 0.1% lows), reduz consumo de GPU e alivia alocações de memória no loop de renderização — **sem remover, desativar ou alterar nenhuma mecânica vanilla** e **sem nunca tocar** no estado do mundo, servidores ou saves.

| | |
|---|---|
| **Id** | `modotimizacao` |
| **Versão** | 1.0.0-alpha |
| **Minecraft** | 26.3 (Fabric Loader ≥ 0.19.5) |
| **Java** | ≥ 25 |
| **Environment** | **client only** |
| **Licença** | [LGPL-3.0](LICENSE) |
| **Autor** | [BadTonho](https://github.com/BadTonho/ModOtimizacao) |

> 💡 **Filosofia:** a única diferença perceptível para o jogador é o ganho de desempenho. O jogo continua sendo o Minecraft puro em essência (Regra 4), e 100% da lógica roda no cliente (Regra 5) — zero risco de corrupção de mundos ou dessincronização em multiplayer.

---

## 🖥️ Requisitos & Instalação

1. Instale o **[Fabric Loader](https://fabricmc.net/use/installer/)** igual ou superior a `0.19.5` (o mod é autônomo — sem dependências de Fabric API ou outros mods auxiliares).
2. Use Java **25+** (o próprio Minecraft 26.3 exige).
3. Copie o JAR (`ModOtimizacao-1.0.0-alpha.jar`) para a pasta `.minecraft/mods/`.

O mod roda **apenas no lado do cliente**: em servidores multiplayer, ninguém precisa instalá-lo — e quem instalar não altera nada no servidor. Funciona igual em singleplayer e em servidores vanilla.

---

## 🚀 Recursos

### ✨ Animações Inteligentes (Texturas Dinâmicas)
Água corrente, lava, fogo, portal do Nether e outros sprites animados são **pausados na GPU** quando não visíveis:
- Fora do campo de visão da câmera (seção de chunk não visível, ou sprite não consultado por entidades/GUI) → **atualização de VRAM pulada** (render pass da animação suprimido).
- **Paridade temporal garantida:** o estado `tick` do sprite continua avançando vanilla (nenhum sprite "congela"); ao voltar à visão, ele retoma exatamente de onde parou.
- Rastreamento duplo de visibilidade: seções de chunk visíveis + sprites consultados (entidades/GUI).

### 🛑 Culling de BlockEntities (Baús, Placas, Molduras…)
Em bases densas, o vanilla tenta renderizar centenas de BlockEntities mesmo atrás de portas e pisos. Pipeline em camadas — O(1), zero raycasts:
1. Distância máxima configurável (+ frustum com margem que protege baús duplos/camas);
2. Teste de **enclausuramento sólido** com margem protetora (6 faces + anel de 26 células) — nada que se projete no vizinho é escondido indevidamente;
3. **Isenções absolutas (Regra 4):** beacons, portais, blocos quebrando e espectador.

### 🐗 Occlusion Culling de Entidades (Mobs e Jogadores)
Mobs totalmente escondidos atrás de blocos sólidos não geram cálculo ósseo nem draw calls:
- **Isenções completas:** jogador local, efeito Glowing/Spectral Arrow, câmera, veículos/montarias em uso, bosses (Ender Dragon, Wither, Elder Guardian), laços e espectador;
- Coluna de caverna completa (até 16 blocos acima do mob até o nível da câmera, resolve o chunk 1 única vez — 16× menos buscas por frame por mob; testes com "indecisão" fazem fallback vanilla);
- Limite de distância medido pelo **ponto mais próximo da hitbox** (mobs grandes não sofrem pop-in/out na borda).

### 💨 Sistema de Partículas Eficiente
- **Culling por grade de blocos:** partículas dentro de blocos sólidos opacos são descartadas — consultas servidas por um cache real por frame (`ParticleCullCache`, double-buffer): partículas do mesmo bloco no mesmo frame compartilham **uma única** consulta `getBlockState` (hit rate alto em chuva/explosões);
- Proteção de proximidade: partículas a ≤ 3 blocos são sempre visíveis;
- **Limitador adaptativo** anti-lag em explosões: máximo configurável por bloco (35 por padrão), janela alinhada ao tick do jogo e teto global de segurança de 1000 partículas por tick;
- **Isenções de feedback de gameplay:** crit, dano/inimigo (enchanted-hit), nota de música e totem jamais são cortados.

### 🔤 HUD & Fontes Mais Leves
- `FontWidthCache`: medições de largura de texto repetidas (HUD, menus, scoreboard) servidas por cache **por fonte**, invalidado no reload de recursos — elimina decomposições de strings por frame.

---

## ⚙️ Configuração

**Onde configurar:** tela nativa do jogo, acessível pelo botão **⚡ Mod de Otimização** no rodapé da tela de Opções de Gráficos (*Esc → Opções → Gráficos...*). As escolhas são salvas automaticamente ao fechar.

**Arquivo:** as preferências persistem em `.minecraft/config/modotimizacao.json` (Gson pretty-printing, com reparo automático contra arquivos ausentes/corrompidos).

| Opção | Padrão | Descrição |
|---|:---:|---|
| `pauseHiddenAnimations` | `true` | Pausa na GPU a atualização de texturas animadas invisíveis |
| `enableBlockEntityCulling` | `true` | Occlusion culling de baús/placas/molduras/etc. |
| `blockEntityMaxDistance` | `1.0E9` | Distância máxima de BlockEntities (**Ilimitado** = vanilla) |
| `blockEntityOcclusionCulling` | `true` | Teste de enclausuramento sólido em BlockEntities |
| `enableEntityCulling` | `true` | Occlusion culling de entidades |
| `entityMaxDistance` | `1.0E9` | Distância máxima de entidades (**Ilimitado** = vanilla) |
| `entityOcclusionCulling` | `true` | Scan de coluna de caverna para mobs |
| `optimizeFontAndHud` | `true` | Cache de largura de fontes (HUD, menus, scoreboard) |
| `optimizeParticles` | `true` | Culling de partículas ocluídas |
| `particleMaxDistance` | `1.0E9` | Distância máxima de partículas (**Ilimitado** = vanilla) |
| `maxParticlesPerBlock` | `35` | Limite adaptativo por bloco anti-lag de explosões |

> 📐 **Por que os padrões são "Ilimitado"?** Fidelidade vanilla absoluta (Regra 4): qualquer restritor além do vanilla é um trade-off que deve ser **ativado conscientemente** pelo jogador na GUI (ciclo *Ilimitado → 96 → 64 → 32*).

### 📋 Diagnóstico na tela F3
A linha `⚡ OTIMIZAÇÃO` no menu F3 mostra em tempo real o estado de cada subsistema: `Anims`, `HUD`, `BE-Cull`, `Ent-Cull`, `Part-Limit` (ligado verde/desligado vermelho).

---

## 🏆 Benchmark

Suíte autônoma comparativa (`benchmark.ps1`) executada em cenário pesado **Stonehill Castle - v1.4** (60s por instância, GTX 1660 SUPER, Minecraft 26.3):

| Configuração | FPS Médio | 1% Lows | GPU Média | VRAM Máx. |
|---|:---:|:---:|:---:|:---:|
| Minecraft Vanilla | 953.2 | 99.8 | 73.5% | 2506 MB |
| Minecraft + Sodium 0.9.2 | 1029.4 | 35.0 | 68.5% | 2246 MB |
| **Minecraft + Nosso Mod** | **1076.5** | **95.6** | 74.4% | 2493 MB |

- **+12.9% FPS** sobre o vanilla puro e **+4.6%** sobre o Sodium;
- **1% Low ~3× maior** que o Sodium — módulo de eliminação de micro-travamentos;
- Suíte só aparece em desenvolvimento: a ferramenta de captura (`fps-recorder`) e o pacote `benchmark/` ficam **excluídos do JAR principal**.

Detalhes: [BENCHMARK_RESULTS.md](BENCHMARK_RESULTS.md) · [benchmark/README.md](benchmark/README.md).

---

## 🔨 Compilando do código

Requisitos: **JDK 25** e o wrapper do Gradle 9 que já vem no repositório.

```bash
# Windows
.\gradlew.bat build

# Linux/macOS
./gradlew build
```

- JAR de saída: `build/libs/ModOtimizacao-1.0.0-alpha.jar`
- **Caches ativos:** paralelismo + build cache + configuration cache (builds incrementais reusam a configuração em ~1s); para builds offline, use `./gradlew build --offline`.

O projeto compila contra **nomes oficiais Mojang** (via `loom.noIntermediateMappings()`), sem camada intermediária de mappings.

---

## 🗂️ Estrutura do projeto

```
ModOtimizacao/
├── src/main/java/com/modotimizacao/
│   ├── core/                    # ModClientInitializer (entrypoint, Regra 5)
│   ├── config/                  # ModConfig (opções do mod) + ConfigManager (JSON)
│   ├── gui/                     # ModConfigScreen (tela nativa in-game)
│   ├── modules/
│   │   ├── core/                # FrameTracker (sincronização única por frame)
│   │   ├── animations/          # Otimizador de animações invisíveis (Fase 2)
│   │   ├── culling/blockentity/ # Culling de BlockEntities (Fase 3)
│   │   ├── culling/entity/      # Culling de entidades (Fase 4)
│   │   ├── particles/           # Culling + limitador de partículas (Fase 5/8)
│   │   └── gui/                 # Fonte/HUD (FontWidthCache; GuiOptimizationModule)
│   ├── mixin/                   # 17 mixins cirúrgicos (registrados em modotimizacao.mixins.json)
│   └── benchmark/               # fps-recorder (dev; excluído do JAR principal)
├── benchmark/                   # Suíte de benchmark (collector.ps1 + ferramenta)
├── benchmark.ps1 / runVanilla.bat
├── ARQUITETURA.md               # Índice da arquitetura (docs por função)
├── docs/arquitetura/            # Um documento técnico por função (9 arquivos)
├── IDEIA_DO_PROJETO.md          # Visão, arquitetura e histórico de decisões
├── ROADMAP.md                   # Fases 1–8 detalhadas e concluídas
├── REGRAS_DO_PROJETO.md         # As 5 regras fundamentais
├── BENCHMARK_RESULTS.md         # Resultados da suíte
├── build.gradle / gradle.properties
└── LICENSE                      # GNU LGPL v3 (texto verbatim)
```

**Princípios de arquitetura (Regra 3):** cada subsistema é um pacote independente (um módulo = uma funcionalidade, um mixin por preocupação), sem monólitos; cada cache/estado é confiado a um dono único e documentado com seu regime de thread (tudo confinado à thread de render/main).

---

## 🧠 Decisões-chave de design

| Decisão | Por quê |
|---|---|
| Defaults "Ilimitado" nas distâncias | Fidelidade vanilla (Regra 4): restringir é escolha consciente do usuário |
| Cache de oclusão por **frame** (double-buffer) | Partículas do mesmo bloco compartilham 1 consulta; resultado "stale" impossível (frame embutido no valor) |
| **Sem** cache de entidades entre frames | `shouldRender` roda 1×/frame/entidade (cache intra-frame sem ganho); entre frames criaria janela de "mob invisível" após quebrar paredes (viola Regra 4) |
| `@WrapOperation` (MixinExtras) em vez de `@Redirect` | Composição de mixins livre de colisões com outros mods |
| `FrameTracker` central | Reset por frame convergindo em 1 ponto (`prepareChunkRenders`), O(1) amortizado |
| Zero alocação no hot loop | Cursores mutáveis reutilizáveis, matemática manual — sem GC pressure por frame |
| Controle de qualidade via JAR oficial | Todos os alvos de mixin validados com `javap` contra o JAR do Minecraft 26.3 |

---

## 📚 Documentação complementar

| Documento | Conteúdo |
|---|---|
| [IDEIA_DO_PROJETO.md](IDEIA_DO_PROJETO.md) | Visão geral, pilares de otimização, histórico de decisões (fonte da verdade) |
| [ROADMAP.md](ROADMAP.md) | Fases 1–8 detalhadas com status ✅ e critérios de aceite |
| [ARQUITETURA.md](ARQUITETURA.md) | **Documentação técnica da arquitetura, separada por funções** (índice + 9 documentos em `docs/arquitetura/`) |
| [REGRAS_DO_PROJETO.md](REGRAS_DO_PROJETO.md) | As 5 regras fundamentais (obrigatórias para contribuição) |
| [BENCHMARK_RESULTS.md](BENCHMARK_RESULTS.md) | Relatório completo da suíte de benchmarks |
| [benchmark/README.md](benchmark/README.md) | Como usar a suíte de benchmark |
| [AGENTS.md](AGENTS.md) | Regras para assistentes de IA que colaboram no código |

---

## 📄 Licença

Distribuído sob os termos da **[GNU Lesser General Public License v3.0](LICENSE)** — o texto está no repositório verbatim, como a própria licença exige.
