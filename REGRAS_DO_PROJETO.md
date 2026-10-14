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
