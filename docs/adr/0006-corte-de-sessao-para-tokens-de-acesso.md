# ADR 0006: Corte de sessão para tokens de acesso

- Status: Proposto
- Data: 2026-11-13

## Contexto

O token de acesso é um JWT de 15 minutos validado só pelo gateway, sem consulta a banco. Quando o auth removia um usuário ou trocava o papel dele, só os tokens de renovação eram revogados. O token de acesso que o usuário já tinha continuava valendo até expirar, então um usuário removido seguia acessando a API por até 15 minutos, e um papel rebaixado seguia com os poderes antigos.

## Decisão

**O auth grava um corte por usuário no Redis.** Ao remover um usuário, trocar o papel dele ou ligar ou desligar o 2FA, o auth escreve a chave `session-cutoff:<userId>` com o instante da operação em segundos e um prazo de vida igual ao do token de acesso mais um minuto. Depois disso nenhum token emitido antes do corte deve valer.

**O gateway recusa o que foi emitido até o corte.** Depois de validar a assinatura, o gateway lê a chave. Se existir e o `iat` do token for menor ou igual ao corte, responde 401. Um token sem `iat` é recusado quando há corte, porque não dá para provar que é posterior. A chave e seu formato ficam em `lexo-signing` (`SessionCutoff`), um só lugar para os dois lados.

**O auth falha fechado, o gateway falha aberto.** Se o Redis não aceitar a escrita, a remoção ou a troca de papel responde 503 e é desfeita inteira, porque uma revogação que não aconteceu não pode parecer concluída. Se o gateway não conseguir ler o Redis, a requisição segue, como já seguia antes do corte existir e como o limitador de taxa do próprio gateway faz. Trocar para falhar fechado derrubaria todo o sistema quando o Redis caísse.

**Rotas públicas não consultam o Redis.**

## Consequências

- O custo é uma leitura no Redis por requisição autenticada no gateway. Ele já usa Redis em toda requisição para o limite de taxa.
- Quem tem o token no momento do corte precisa entrar de novo. É o efeito desejado: o refresh também foi revogado.
- O corte vale por 16 minutos. Depois disso todo token anterior já expirou sozinho e a chave some.
- Ligar e desligar o 2FA também encerram as sessões, pelo mesmo mecanismo: quem liga o 2FA cai e precisa entrar de novo, já com o código, e a falha em gravar o corte desfaz a mudança, como na troca de papel. Não existe troca de senha no sistema (nem "esqueci a senha"), então não há sessão a revogar nesse caso; quando existir, deve usar o mesmo mecanismo.

## Referências

- ADR 0005, sessão no caso de uso e revogação por reuso de token.
