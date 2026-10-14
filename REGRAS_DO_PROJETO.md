# 📜 Regras Fundamentais do Projeto

Diretrizes obrigatórias de desenvolvimento, governança e boas práticas para o projeto.

---

### 🛡️ Regra 1: Segurança e Sigilo (Zero Informações Sensíveis)
* **Nunca** comitar ou subir para o GitHub ou qualquer repositório público:
  * Tokens de acesso, senhas, chaves de API, credenciais ou segredos.
  * Informações pessoais, caminhos locais absolutos do sistema com dados de identificação, ou dados de autenticação de contas de teste do Minecraft.
  * Arquivos de build desnecessários, caches (`.gradle`, `build/`, `run/`, `.idea`).
* **Ação contínua:** Manter sempre um arquivo `.gitignore` rigoroso e revisar diffs antes de qualquer commit.

---

### 📚 Regra 2: Documentação Viva e Atualizada
* **Sempre** manter os documentos de especificação, decisões arquiteturais e READMEs sincronizados com o código real.
* Qualquer nova funcionalidade, alteração de foco ou refatoração estrutural deve ser refletida na documentação imediatamente.
* O arquivo `IDEIA_DO_PROJETO.md` (e outros futuros documentos) serve como a fonte da verdade do roadmap e do design.

---

### 🧩 Regra 3: Arquitetura Modular (Sem Monólitos)
* **Evite monólitos:** Não tenha medo de criar novos arquivos, pacotes e classes especializadas.
* **Organização em Pastas e Subpastas:** Crie pastas, subpastas e pacotes dedicados sempre que necessário para manter código, recursos e documentação limpos, isolados e com navegação intuitiva.
* **Princípio da Responsabilidade Única (SRP):** Cada classe/arquivo deve resolver um problema bem delimitado (ex: um mixin separado para culling de entidades, outro para partículas, utilitários matemáticos dedicados, etc.).
* **Alta coesão e baixo acoplamento:** Isolar as lógicas de otimização para que possam ser testadas, ligadas ou desligadas individualmente através de configurações sem afetar o resto do mod.

---

### 🎮 Regra 4: Fidelidade Vanilla Absoluta (Nunca Remover Funções)
* **Nenhuma função nativa deve ser removida ou desativada:** Otimizar significa executar o que o jogo faz de forma mais inteligente, rápida e eficiente — e **nunca** "cortar caminhos" deletando mecânicas, recursos gráficos ou comportamentos originais.
* **Paridade Visual e de Gameplay:** Tudo o que existe no Minecraft vanilla (efeitos, partículas, animações, renderização de blocos, mecânicas de entidades e interface) deve continuar existindo e funcionando perfeitamente.
* **Invisibilidade ao Usuário:** A única diferença perceptível para o jogador deve ser o ganho expressivo de FPS, estabilidade e redução de consumo de recursos; o jogo deve continuar sendo o Minecraft puro em sua essência.

---

### 🌐 Regra 5: Estritamente Client-Side (Zero Alterações no Mundo)
* **100% Lado do Cliente:** O mod opera e sempre operará exclusivamente no cliente. Ele nunca deve exigir instalação em servidores nem tentar gerenciar regras de servidor.
* **Intocabilidade do Mundo:** O mod NUNCA altera o estado do mundo, blocos, itens do inventário lógico, salvamento em disco (`.mca`, `level.dat`) ou mecânicas de sobrevivência.
* **Zero Risco de Corrupção ou Dessincronização:** É estritamente proibido qualquer código que possa gerar blocos fantasmas (*ghost blocks*), dessincronização com servidores multiplayer ou risco de corromper mundos salvos. Qualquer jogador pode entrar e sair de mundos ou servidores com o mod ativado com total segurança.
