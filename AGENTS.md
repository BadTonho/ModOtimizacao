# Regras e Diretrizes para Assistentes de IA

Sempre siga estritamente as regras definidas pelo projeto:

1. **Segurança e Privacidade Absoluta:**
   - NUNCA exponha, commite ou gere arquivos contendo senhas, chaves, tokens, credenciais ou informações pessoais sensíveis.
   - Verifique sempre se arquivos ignorados estão no `.gitignore`.

2. **Documentação Viva e Sempre Atualizada:**
   - Mantenha `IDEIA_DO_PROJETO.md` e a documentação sincronizadas a cada nova decisão técnica, escopo ou adição de código.

3. **Arquitetura Modular (Sem Monólitos):**
   - Não crie arquivos gigantescos ou monólitos.
   - Não hesite em criar arquivos, módulos e classes dedicadas para cada responsabilidade específica.
   - Não hesite em criar pastas e subpastas para organizar melhor a hierarquia do projeto.

4. **Fidelidade Vanilla Absoluta (Nunca Remover Funções):**
   - NUNCA remova, desative ou quebre nenhuma mecânica, efeito ou função nativa do Minecraft.
   - Otimizar significa fazer a mesma coisa de forma mais inteligente e eficiente, preservando 100% da experiência vanilla.

5. **Estritamente Client-Side (Zero Alterações no Mundo):**
   - NUNCA injete código que altere o estado do mundo, lógica de servidor, salvamento em disco ou pacotes de sincronização.
   - O mod roda exclusivamente no cliente (environment: `client`), garantindo que o mundo e servidores multiplayer permaneçam 100% intocados e seguros contra qualquer corrupção ou dessincronização.
