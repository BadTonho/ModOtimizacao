# 🗺️ Roadmap de Desenvolvimento

Plano de execução passo a passo para o desenvolvimento do mod de otimização client-side no Fabric para Minecraft 26.3.

---

## 📌 Visão Geral das Fases

```
[Fase 1: Alicerce] ➔ [Fase 2: Animações] ➔ [Fase 3: BlockEntity Culling] ➔ [Fase 4: Entity Culling] ➔ [Fase 5: HUD & Partículas] ➔ [Fase 6: Polimento & Config]
```

---

## 🏗️ Fase 1: Alicerce do Projeto (Setup & Arquitetura Base)
**Objetivo:** Ter o ambiente Fabric Loom pronto, compilável e estruturado para receber os módulos.

- [ ] **Configuração do Gradle e Fabric Loom:**
  - `build.gradle` com suporte ao Java 25 / JVM moderna.
  - `gradle.properties` com as variáveis de versão (Minecraft 26.3, Loom, Yarn/Mapeamentos).
  - `settings.gradle`.
- [ ] **Metadados do Mod (`fabric.mod.json`):**
  - Declarar modid provisório (`modotimizacao`).
  - Definir estritamente `"environment": "client"` (Regra 5).
- [ ] **Configuração de Mixins (`modotimizacao.mixins.json`):**
  - Mapear injeções de bytecode para o lado do cliente.
- [ ] **Esqueleto de Pastas e Pacotes (Regra 3):**
  - `com.modotimizacao.core`: Classe de inicialização `ClientModInitializer`.
  - `com.modotimizacao.config`: Estrutura para leitura e persistência de opções.
  - `com.modotimizacao.modules`: Diretório base para os sub-módulos independentes.
  - `com.modotimizacao.util`: Utilitários matemáticos e estruturas rápidas.

---

## ⚡ Fase 2: Primeiro Módulo — Pausa de Animações Invisíveis
**Objetivo:** Reduzir consumo de banda da GPU pausando o upload de texturas animadas fora de visão.

- [ ] Identificar o ponto de atualização de animação de sprites no Minecraft (`SpriteContents.Animator` / renderizador de texturas).
- [ ] Criar mixin cirúrgico para verificar se a textura está sendo desenhada no frame atual antes de avançar o tick da animação.
- [ ] Garantir 100% de paridade visual (assim que a textura entra na tela, ela roda fluidamente sem delay).
- [ ] Validar conformidade com a Regra 4 (Fidelidade Vanilla) e Regra 5 (Zero impacto no mundo).

---

## 📦 Fase 3: Culling de BlockEntities (Baús, Molduras e Placas)
**Objetivo:** Eliminar quedas de FPS em vilas e salas de baús.

- [ ] Criar sub-módulo isolado em `modules/culling/blockentity/`.
- [ ] Implementar teste rápido de oclusão/frustum para caixas de colisão de blocos especiais.
- [ ] Interceptar o `BlockEntityRenderDispatcher` com injeção de Mixin condicional.
- [ ] Criar chave de configuração individual (ativar/desativar).

---

## 👥 Fase 4: Occlusion Culling de Entidades (Mobs e Jogadores)
**Objetivo:** Evitar o processamento e envio à GPU de modelos de mobs escondidos atrás de blocos sólidos.

- [ ] Criar sub-módulo isolado em `modules/culling/entity/`.
- [ ] Interceptar `EntityRenderDispatcher` para pular entidades ocluídas.
- [ ] Blindagem: garantir que efeitos essenciais (como brilho de contorno/glowing) continuem respeitando o vanilla.

---

## 🎨 Fase 5: Otimização de Interface e Partículas
**Objetivo:** Melhorar a resposta de menus/inventários e evitar travamentos com partículas.

- [ ] **Batching de Fontes/HUD:** Agrupar chamadas de desenho de glifos de texto.
- [ ] **Culling de Partículas:** Descartar partículas criadas atrás de paredes sólidas.
- [ ] **Limitador Adaptativo de Partículas:** Evitar lag spikes em explosões massivas.

---

## ⚙️ Fase 6: Sistema de Configurações e Diagnóstico
**Objetivo:** Permitir ao jogador ligar/desligar cada recurso e mensurar os ganhos.

- [ ] Criar arquivo de configuração JSON local em `.minecraft/config/modotimizacao.json`.
- [ ] Adicionar tela de opções na interface (opcional, com suporte a ModMenu se disponível).
- [ ] Testes de frametime (1% lows) e perfilamento de consumo de VRAM e GPU.

---

## 📋 Critérios de Aceite para Cada Etapa
1. Segue as 5 regras de [REGRAS_DO_PROJETO.md](file:///c:/Users/Admin/Desktop/ProjetosCode/minecraft/ModOtimização/REGRAS_DO_PROJETO.md).
2. Não quebra compilação nem gera warnings de Mixin.
3. Não altera nenhum dado de save, pacotes de rede ou lógica de servidor.
