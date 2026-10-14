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

---

## 🗺️ 5. Plano de Execução
O detalhamento passo a passo de cada fase de implementação, critérios de aceite e ordem de desenvolvimento está documentado em [ROADMAP.md](file:///c:/Users/Admin/Desktop/ProjetosCode/minecraft/ModOtimização/ROADMAP.md).

---

## 📝 6. Histórico de Decisões & Ideias
* **[Início]**: Definição do uso do Fabric como loader principal.
* **[Versão]**: Registro da versão alvo **Minecraft 26.3**.
* **[Escopo]**: Foco confirmado em **Client-Side** (FPS, fluidez visual e renderização). Nome do mod a ser decidido futuramente.
* **[Governança]**: Estabelecimento das 5 regras fundamentais (Segurança, Documentação Viva, Modularidade, Preservação Vanilla e Client-Side Estrito sem alteração de mundo).
* **[Planejamento]**: Criação do [ROADMAP.md](file:///c:/Users/Admin/Desktop/ProjetosCode/minecraft/ModOtimização/ROADMAP.md) estruturando as 6 fases de implementação.
* **[Fase 1 Concluída]**: Configuração do ambiente Fabric Loom 1.18.4 + Gradle 9.7.0 + Java 25, metadados client-side, modularização de pacotes e build validado gerando o JAR `ModOtimizacao-1.0.0-alpha.jar`.
