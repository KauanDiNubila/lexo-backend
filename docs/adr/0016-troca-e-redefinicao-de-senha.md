# ADR 0016: Troca e redefinição de senha

- Status: Proposto
- Data: 2026-11-16

## Contexto

O sistema não tinha como trocar a senha nem como recuperar o acesso de quem a esqueceu. A única forma de voltar a entrar era um administrador remover o usuário e convidá-lo de novo.

## Decisão

**Três operações no auth.** `POST /api/auth/senha/esqueci` pede o link, `POST /api/auth/senha/redefinir` define a senha nova com o link, e `PUT /api/conta/senha` troca a senha de quem está logado. As duas primeiras são públicas (auth e gateway).

**O link carrega um token que só existe no e-mail.** São 32 bytes aleatórios (`TokenSecret`, o mesmo gerador do token de renovação). O banco guarda só o SHA-256, na tabela `password_resets`. O token vale 1 hora e só uma vez: a linha é apagada ao usar. Um pedido novo não invalida os links anteriores, porque senão quem pedisse links com o e-mail de outra pessoa derrubaria o link que ela já recebeu. Usar qualquer link apaga todos os do usuário, e cada pedido apaga os links expirados dele. A linha também some junto com o usuário (`on delete cascade`).

**O pedido de link não revela quem tem conta.** A resposta é 204 para qualquer e-mail válido. O limite de 3 pedidos por hora conta por e-mail, existindo conta ou não. Uma falha na fila de e-mail não muda a resposta. Com o Redis fora a resposta é 503 para todos, porque o limite não pode ser verificado. Todo token inválido, expirado, já usado ou de usuário removido responde a mesma mensagem, "Link inválido ou expirado".

**Trocar ou redefinir encerra todas as sessões.** Usa o mesmo mecanismo da remoção de usuário e da troca de papel (ADR 0006), agora num único lugar, o `SessionTerminator`. Se o Redis não aceitar o corte, a operação responde 503 e é desfeita inteira. Quem troca a senha cai e precisa entrar de novo, inclusive o próprio dispositivo: um token novo emitido no mesmo segundo do corte seria recusado pela regra `iat <= corte`.

**Redefinir a senha não desliga o 2FA.** O link prova posse do e-mail, não do segundo fator. Quem tem 2FA continua precisando do código no login.

**A troca exige a senha atual e limita as tentativas.** São 5 tentativas em 15 minutos por usuário, depois disso 429, mesmo com a senha certa. O limite protege contra quem tem um token de acesso roubado e tenta adivinhar a senha atual. Os erros de validação são 400, nunca 401, porque o frontend trata 401 como sessão expirada e deslogaria o usuário. A senha nova precisa ser diferente da atual.

**Auditoria e aviso.** Os dois caminhos publicam `ALTEROU_SENHA` e `REDEFINIU_SENHA` no Kafka, e mandam um e-mail "sua senha foi alterada" ao dono da conta, para que uma troca feita por outra pessoa não passe despercebida. O aviso é de melhor esforço: se a fila estiver fora, a troca continua valendo.

## Consequências

- Os limites usam o `AttemptCounterPort`, o contador de tentativas por janela que o login já usava (antes `LoginAttemptsPort`, nome de login).
- O limite do pedido de link conta só por e-mail. O limite por endereço já existe no gateway, que vale para toda rota.
- O pedido de link para uma conta existente faz um insert e enfileira um e-mail, então pode levar alguns milissegundos a mais que para um e-mail sem conta. Não é um canal fácil de medir pela internet, mas existe.
- O link aponta para `BASE_URL` (`/redefinir-senha/<token>`). Se essa variável estiver errada em produção, os links também estarão.
- O frontend ganhou as telas `/esqueci-senha` e `/redefinir-senha/:token`, o link "Esqueci minha senha" no login e o cartão "Senha" nas configurações.

## Referências

- ADR 0006, corte de sessão para tokens de acesso.
- ADR 0010, fila de e-mails no notificacao-service.
