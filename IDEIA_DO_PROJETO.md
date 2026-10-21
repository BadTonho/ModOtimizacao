# ⚡ Projeto: Mod de Otimização para Minecraft

Documento vivo com o planejamento, arquitetura, ideias e escopo do mod de otimização.

---

## 📌 1. Visão Geral
* **Nome Provisório:** *A definir* (ex: *NovaOptim*, *Acelera*, *QuantumMC*)
* **Plataforma / Modloader:** **Fabric**
* **Versão Alvo:** **Minecraft 26.3** (Fabric Loader)
* **Linguagem Principal:** Java (JVM moderna)
* **Escopo:** **100% Lado do Cliente (Client-Side)**
* **Objetivo:** Maximizar taxas de quadros (FPS), estabilizar frametimes (1% e 0.1% lows), reduzir consumo de GPU e aliviar alocações de memória na renderização.

---

## 🎯 2. Pilares de Otimização (Foco Client-Side)

### A. Culling Avançado de Entidades e Blocos Especiais (BlockEntities)
* **Occlusion Culling para Mobs:** Se uma vaca, zumbi ou jogador está completamente atrás de uma parede sólida de blocos, a GPU e a CPU não devem gastar tempo calculando nem renderizando seus modelos e sombras.
* **BlockEntity Culling (Baús, Molduras, Placas, Sinalizadores):** Em bases com centenas de baús e organizadores, o Minecraft vanilla tenta renderizar tudo mesmo atrás de portas ou pisos. Fazer culling cirúrgico aqui traz ganhos massivos de FPS.

### B. Gestão Inteligente de Texturas e Animações
* **Skip de Animações Invisíveis:** Texturas dinâmicas (água corrente, fogo, lava, portal do Nether) consomem largura de banda atualizando na VRAM a cada frame. Se o jogador não está olhando para elas, as animações são pausadas.
* **Redução de Overhead de VBO/VAO:** Reutilização eficiente de buffers na GPU para não recriar buffers a todo frame.

### C. Otimização de Interface (HUD, Telas e Fontes)
* **Batching de Texto e Glifos:** O renderizador de fontes do Minecraft quebra lotes (*draw batches*) com facilidade. Agrupar a renderização de textos no HUD, menus e inventários cheios evita centenas de draw calls inúteis.
* **Cache de Tooltips e Ícones de Itens:** Em inventários complexos ou com muitos mods, reutilizar renderizações já preparadas.

### D. Sistema de Partículas Eficiente
* **Culling de Partículas:** Não processar partículas de fumaça, poções ou explosões que estejam atrás de paredes opacas.
* **Controle de Densidade:** Reduzir inteligentemente a taxa de emissão quando houver excesso na tela (evita travadas em batalhas com poções e fogos de artifício).

### E. Estabilidade de Frametimes (Redução de Micro-travamentos)
* **Alocações no Loop de Render:** Eliminar alocações temporárias de matrizes (`MatrixStack`), vetores e quatérnios que ocorrem dezenas de milhares de vezes por segundo no loop gráfico, poupando o Garbage Collector.

---

## 🛠️ 3. Ferramentas e Tecnologias Base
* **Fabric Loader & Fabric Loom:** Ambiente oficial de compilação e execução.
* **SpongePowered Mixin:** Injeção precisa de bytecode em tempo de execução no pipeline do cliente (`GameRenderer`, `WorldRenderer`, `EntityRenderDispatcher`, etc.).
* **Profiling de GPU e CPU:** Ferramentas para medir draw calls e frametimes (ex: mangohud/renderdoc/spark).

---

## 📜 4. Diretrizes e Regras
O desenvolvimento segue estritamente as regras definidas em [REGRAS_DO_PROJETO.md](file:///c:/Users/Admin/Desktop/ProjetosCode/minecraft/ModOtimização/REGRAS_DO_PROJETO.md):
1. **Segurança e Sigilo:** Jamais subir informações sensíveis ou credenciais para o GitHub.
2. **Documentação Viva:** Manter sempre a documentação atualizada a cada passo.
3. **Modularidade e Organização (Zero Monólitos):** Arquitetura distribuída em classes, arquivos e pastas/subpastas dedicadas com responsabilidades bem definidas.
4. **Fidelidade Vanilla Absoluta:** Jamais remover funções, mecânicas ou recursos visuais do jogo; a otimização deve ser 100% transparente.
5. **Estritamente Client-Side:** O mod nunca altera blocos, chunks, itens, rede ou o estado do mundo. Totalmente seguro contra corrupção de mundos ou dessincronização em servidores.

A **documentação geral para usuários e publicação** (instalação, recursos, configuração in-game, build, benchmark e estrutura) vive no [README.md](file:///c:/Users/Admin/Desktop/ProjetosCode/minecraft/ModOtimização/README.md).

---

## 🗺️ 5. Plano de Execução
O detalhamento passo a passo de cada fase de implementação, critérios de aceite e ordem de desenvolvimento está documentado em [ROADMAP.md](file:///c:/Users/Admin/Desktop/ProjetosCode/minecraft/ModOtimização/ROADMAP.md). A documentação técnica da arquitetura — **separada por funções** (bootstrap/frame, animações, culling de BlockEntities/entidades, partículas, fontes/HUD, configuração/GUI, benchmark e referência de mixins) — vive no [ARQUITETURA.md](file:///c:/Users/Admin/Desktop/ProjetosCode/minecraft/ModOtimização/ARQUITETURA.md) e em [docs/arquitetura/](file:///c:/Users/Admin/Desktop/ProjetosCode/minecraft/ModOtimização/docs/arquitetura).

---

## 📝 6. Histórico de Decisões & Ideias
* **[Início]**: Definição do uso do Fabric como loader principal.
* **[Versão]**: Registro da versão alvo **Minecraft 26.3**.
* **[Escopo]**: Foco confirmado em **Client-Side** (FPS, fluidez visual e renderização). Nome do mod a ser decidido futuramente.
* **[Governança]**: Estabelecimento das 5 regras fundamentais (Segurança, Documentação Viva, Modularidade, Preservação Vanilla e Client-Side Estrito sem alteração de mundo).
* **[Planejamento]**: Criação do [ROADMAP.md](file:///c:/Users/Admin/Desktop/ProjetosCode/minecraft/ModOtimização/ROADMAP.md) estruturando as fases de implementação.
* **[Fase 1 Concluída]**: Configuração do ambiente Fabric Loom 1.18.4 + Gradle 9.7.0 + Java 25, metadados client-side, modularização de pacotes e build validado gerando o JAR `ModOtimizacao-1.0.0-alpha.jar`.
* **[Fase 2 Concluída]**: Módulo de Pausa de Animações Invisíveis (Smart Texture Animations) implementado e testado. Rastreamento duplo de visibilidade (terreno e entidades/GUI), supressão de render pass RenderPearl na GPU e avanço temporal vanilla contínuo. Build validado com sucesso.
* **[Fase 3 Concluída]**: Módulo de Culling de BlockEntities implementado e validado. Pipeline de 3 etapas com zero alocações de heap no frustum culling, enclausuramento sólido e raycast de linha de visão com cache rápido, com preservação estrita de beacons, portais, blocos quebrando e modo espectador (Regra 4). Build compilado em 12s no Java 25.
* **[Fase 4 Concluída]**: Módulo de Occlusion Culling de Entidades (Mobs e Jogadores) implementado e validado. Interceptação cirúrgica pós-frustum em `EntityRenderDispatcher.shouldRender`, teste visual de 3 pontos (olhos, centro e pés) com tolerância geométrica, cache rápido de 100ms e preservação 100% fiel de efeitos Glowing, veículos, montarias e bosses (Regra 4). Poupando tempo de CPU de cálculo ósseo e draw calls na GPU. Build compilado em 12s no Java 25.
* **[Fase 5 Concluída]**: Módulo duplo de Otimização de Interface (HUD/Fontes) e Partículas implementado e validado. Cache de alta velocidade de medição de texto (`FontWidthCache`) eliminando alocações de `MutableFloat` e decomposições de strings, culling de partículas ocluídas por grade de blocos com orçamento de raycasts por frame e limitador adaptativo contra picos de lag em explosões. Build compilado em 11s no Java 25.
* **[Fase 6 Concluída]**: Sistema completo de Configurações, Interface e Diagnóstico implementado e validado. Persistência automática em `.minecraft/config/modotimizacao.json` com Gson pretty-printing, tela de configurações in-game (`ModConfigScreen`) com botões interativos e seletor cíclico de distâncias, injeção nativa no menu vanilla de Gráficos (`VideoSettingsScreen`) e exibição em tempo real de telemetria e status dos módulos na tela F3 (`DebugScreenOverlay`). Compilação completa com sucesso no Java 25 LTS gerando o JAR final.
* **[Benchmark Automatizado & Validação de FPS Concluídos]**: Construção e validação da suíte autônoma de benchmarks comparativos multi-instância (`benchmark.ps1` + `collector.ps1` + ferramenta neutra `fps-recorder.jar`). Teste executado no cenário de teste pesado `Stonehill Castle - v1.4` por 60s por instância. Nosso mod atingiu **1076.5 FPS Médio** (superando tanto o Vanilla Puro em +123.3 FPS quanto o Sodium em +47.1 FPS), com estabilidade de **95.6 FPS nos 1% Lows** (quase 3x superior aos 35.0 FPS do Sodium, comprovando eliminação de micro-travamentos). Resultados consolidados em `BENCHMARK_RESULTS.md`.
* **[Fase 7 Concluída]**: Auditoria completa de integridade (código, mixins × JAR oficial via `javap`, scripts e documentação). Os 9 mixins faltantes registrados, `FontWidthCache` com chave por fonte e invalidação, falsos positivos de oclusão corrigidos (coluna completa de caverna; anel ±1 em BlockEntities), código morto removido, `@Redirect` → `@WrapOperation` (MixinExtras, composível), defaults Ilimitados (Regra 4), benchmark íntegro (1% Low correto, Vanilla pleno) e licença migração MIT → **LGPL-3.0** (`LICENSE` verbatim + `fabric.mod.json`). Consolidado no ROADMAP.md.
* **[Fase 8 Concluída]**: Otimizações de runtime e workflow aplicadas de uma análise focada em performance. `FrameTracker` centraliza a sincronização por frame (`prepareChunkRenders`); `ParticleCullCache` refeito como cache real (double-buffer por frame, key `BlockPos.asLong`, valor `(frameId << 1) | bitOclusao`) compartilhando UMA consulta `getBlockState` por bloco/frame entre partículas; coluna de caverna das entidades com 1 fetch único de `LevelChunk` (16× menos resoluções por mob); alocações residuais eliminadas no hot loop (sem `getCenter`/`new BlockPos`/`pos.relative`); culling de entidades por ponto mais próximo da hitbox (anti pop-in/out). Decisão deliberada: sem cache de entidades entre frames (risco de stale invisível — Regra 4). Build validado com `org.gradle.configuration-cache` (+compatibilidade das closures do `:jar`/`:processResources`) e `maven-publish` removido.

---

## 💡 Próximas Ideias & Compatibilidades Futuras
* Criar compatibilidade com o Iris Shaders (verificação de shaders ativos para sincronização de pipeline).
* Compatibilidade com mods de Crash Log (Not Enough Crashes, Crash Assistant): totalmente compatível, pois o mod não altera o estado do mundo nem dados persistentes.