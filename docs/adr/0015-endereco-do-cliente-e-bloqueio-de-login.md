# ADR 0015: Endereço do cliente e bloqueio de login

- Status: Proposto
- Data: 2026-11-17

## Contexto

O auth limita as tentativas de login em dois contadores: 10 por e-mail e 30 por endereço, numa janela de 15 minutos. Dois defeitos faziam esse limite proteger pouco e punir todos.

**O endereço contado era o de quem fala com o gateway, não o do cliente.** O auth escolhia o endereço pela última entrada de `X-Forwarded-For`. Mas o Spring Cloud Gateway acrescenta a esse cabeçalho o endereço de quem falou com ele. Em produção esse é o Caddy, o mesmo para todo mundo. Foi confirmado numa stack de verdade: quatro logins com `X-Forwarded-For` diferentes (`1.1.1.1`, `2.2.2.2`, `3.3.3.3, 4.4.4.4` e nenhum) geraram uma única chave de endereço, a do peer do gateway, e o contador somou todos. Com o Caddy na frente, 30 tentativas erradas de qualquer pessoa na internet travariam o login de todos por 15 minutos.

**O contador por e-mail deixava um estranho travar a conta de outra pessoa.** Dez senhas erradas para o e-mail da vítima, de qualquer endereço, recusavam o login dela, mesmo com a senha certa, por 15 minutos, e o ataque se repetia.

## Decisão

**O gateway diz ao serviço quem é o cliente.** Um filtro novo, `ClientAddressFilter`, resolve o endereço uma vez (a última entrada de `X-Forwarded-For` quando há proxy confiável, como na ADR 0013, ou o endereço da conexão), guarda no exchange e escreve o cabeçalho `X-Client-Address` na requisição que segue para os serviços, descartando qualquer `X-Client-Address` que o cliente tenha mandado. O limite de requisições do gateway passa a ler esse mesmo valor em vez de resolvê-lo por conta própria. O auth lê `X-Client-Address`. Sem ele o endereço é desconhecido, e não o da conexão, que atrás do gateway seria o do próprio gateway e juntaria todo mundo de volta num balde só. `X-Forwarded-For` deixa de ser usado no auth.

**Três contadores, por e-mail e endereço juntos, por e-mail e por endereço.** 10 tentativas para o par e-mail e endereço, 50 só para o e-mail (o teto contra quem tenta de muitos endereços) e 30 para o endereço. Quem erra a senha de outra pessoa de outro endereço só esgota o próprio par, e o dono continua entrando. Sem endereço conhecido, o par usa o endereço `unknown` e o contador por endereço não existe, então valem 10 por e-mail para todos os desconhecidos juntos e o teto de 50. Todos os contadores contam toda tentativa, mesmo a recusada.

**Um login certo limpa só o contador do par.** Os de e-mail e de endereço expiram sozinhos. O cadastro é aberto, então quem tem uma conta própria poderia zerar o contador do endereço com um login certo e testar senhas em muitas contas a partir do mesmo endereço. O custo é que o contador de endereço passa a contar também os logins certos: um escritório atrás de um único endereço não passa de 30 logins em 15 minutos.

## Consequências

- O auth passa a confiar em `X-Client-Address`. Em produção só o gateway fala com o auth (a porta do auth não é publicada), então o cabeçalho não pode ser forjado. No desenvolvimento a porta é publicada, e lá quem chama o auth direto escolhe o próprio endereço; é só um ambiente de laboratório.
- Um atacante com muitos endereços ainda pode tentar 10 senhas por endereço, até o teto de 50 por e-mail por janela. Isso vale cerca de 4.800 tentativas por dia por conta, contra uma senha de no mínimo 8 caracteres. O mesmo teto de 50 permite a quem controla muitos endereços recusar o login do dono por 15 minutos de cada vez.
- Se um dia houver outro proxy na frente do Caddy, `lexo.rate-limit.trusted-proxies` precisa subir; agora ele afeta também o endereço que o auth usa. O nome da propriedade foi mantido para não mudar o `docker-compose.prod.yml` nesta rodada.
- O gateway não vê o endereço da conexão quando o servidor não o expõe (aconteceu no teste com `WebTestClient`, não no Compose). Nesse caso o cabeçalho não é enviado e o auth trata o endereço como desconhecido. Chamadas diretas ao auth em desenvolvimento, sem o cabeçalho, caem no mesmo caso.

## Referências

- ADR 0013, limite por cliente atrás do proxy e identidade completa no gateway.
