import type { LucideIcon } from "lucide-react";
import { Briefcase, CalendarClock, Sparkles, BarChart3, Wallet, ShieldCheck } from "lucide-react";

export const marca = {
  nome: "Lexo",
};

export const navegacao = [
  { href: "#recursos", label: "Recursos" },
  { href: "#como-funciona", label: "Como funciona" },
  { href: "#ia", label: "Lexo IA" },
  { href: "#precos", label: "Preços" },
];

export const hero = {
  subtitulo: "Gestão jurídica para escritórios de advocacia.",
  ctaPrimaria: { label: "Começar teste de 14 dias", href: "/login?modo=registro" },
  ctaSecundaria: { label: "Área do cliente", href: "/login" },
  selos: ["Sem cartão de crédito", "Autenticação em dois fatores", "Trilha de auditoria completa"],
  mockup: {
    urlFake: "app.lexo.com.br",
    metricas: [
      { valor: "148", rotulo: "Processos ativos" },
      { valor: "9", rotulo: "Prazos urgentes" },
      { valor: "312h", rotulo: "Horas faturáveis no mês" },
    ],
    resumoIA: {
      titulo: "Resumo da Lexo IA",
      texto:
        "3 processos entram em prazo fatal nesta semana. O caso 0001-23.2026 aguarda contestação " +
        "até sexta. Recomendo priorizar a defesa de Andrade vs. Município.",
    },
  },
};

export const recursos: {
  titulo: string;
  subtitulo: string;
  itens: { icone: LucideIcon; titulo: string; texto: string }[];
} = {
  titulo: "Tudo de que o escritório precisa",
  subtitulo: "Uma plataforma única no lugar de planilhas e ferramentas soltas.",
  itens: [
    { icone: Briefcase, titulo: "Gestão de processos", texto: "Cada processo com andamentos, prazos e responsável num só lugar — organizado por cliente e área." },
    { icone: CalendarClock, titulo: "Controle de prazos", texto: "Prazos, audiências e reuniões numa só agenda, com aviso automático por e-mail antes do vencimento." },
    { icone: Sparkles, titulo: "Lexo IA", texto: "Resume um processo inteiro, rascunha petições e responde perguntas sobre os seus casos." },
    { icone: BarChart3, titulo: "Indicadores", texto: "Processos por status, prazos do período e valores em aberto, consolidados num painel." },
    { icone: Wallet, titulo: "Financeiro", texto: "Honorários, faturas e recebimentos vinculados a cliente e processo, com relatório consolidado." },
    { icone: ShieldCheck, titulo: "Portal do cliente", texto: "Seu cliente acompanha processos e honorários por um link com acesso controlado e prazo de validade." },
  ],
};

export const comoFunciona = {
  titulo: "Do caos à clareza em 3 passos",
  passos: [
    { n: "01", titulo: "Monte o escritório", texto: "Crie a organização, convide a equipe e defina quem é administrador, advogado ou secretaria." },
    { n: "02", titulo: "Traga seus casos", texto: "Cadastre clientes e processos, registre andamentos e marque os prazos de cada um." },
    { n: "03", titulo: "Deixe o sistema avisar", texto: "A agenda notifica os prazos por e-mail e a Lexo IA resume qualquer processo quando você precisar." },
  ],
};

export const lexoIA = {
  eyebrow: "LEXO IA",
  titulo: "Um assistente jurídico que nunca dorme",
  texto:
    "A Lexo IA lê seus processos, resume o essencial e ajuda a redigir peças. " +
    "Você revisa e aprova — ela cuida do trabalho repetitivo.",
  beneficios: [
    "Resumo automático de qualquer processo",
    "Rascunho de petições e notificações",
    "Perguntas sobre seus casos em linguagem natural",
    "Nada é enviado sem a sua revisão",
  ],
  metricas: [
    { valor: "620+", rotulo: "escritórios usando" },
    { valor: "1.200+", rotulo: "processos monitorados" },
    { valor: "9h", rotulo: "economizadas por semana" },
    { valor: "99,9%", rotulo: "de disponibilidade" },
  ],
};

export const depoimentos = {
  titulo: "Quem usa, recomenda",
  itens: [
    { nome: "Dra. Helena Andrade", firma: "Andrade Advocacia", texto: "Cortamos pela metade o tempo gasto organizando prazos. A IA resume um processo novo melhor que meu estagiário." },
    { nome: "Dr. Rafael Mendonça", firma: "Mendonça & Cruz", texto: "O portal do cliente reduziu drasticamente as ligações de acompanhamento. Os clientes adoram a transparência." },
    { nome: "Dra. Beatriz Vector", firma: "Vector Legal", texto: "Os indicadores me deram clareza de onde o escritório perdia dinheiro. Hoje decido com dados, não no achismo." },
  ],
  logos: {
    rotulo: "Escritórios que confiam na Lexo",
    nomes: ["Andrade Adv.", "Mendonça & Cruz", "Vector Legal", "Bittencourt Adv.", "Núcleo Jurídico"],
  },
};

export const precos = {
  titulo: "Preços que cabem no escritório",
  subtitulo: "Por usuário/mês. Comece grátis por 14 dias, sem cartão.",
  planos: [
    {
      nome: "Solo",
      preco: "79" as string | null,
      desc: "Para advogados autônomos",
      itens: ["1 usuário", "Processos e prazos ilimitados", "Lexo IA (100 resumos/mês)", "Portal do cliente"],
      destaque: false,
    },
    {
      nome: "Escritório",
      preco: "149" as string | null,
      desc: "Para equipes em crescimento",
      itens: ["Até 10 usuários", "Tudo do Solo, sem limites de IA", "Painel de indicadores", "Auditoria e permissões por papel"],
      destaque: true,
    },
    {
      nome: "Enterprise",
      preco: null as string | null,
      desc: "Para grandes escritórios",
      itens: ["Usuários ilimitados", "SLA e suporte dedicado", "Trilha de auditoria completa", "Onboarding personalizado"],
      destaque: false,
    },
  ],
};

export const ctaFinal = {
  titulo: "Pronto para organizar o escritório inteiro num lugar só?",
  texto: "14 dias grátis, sem cartão. Seus processos e prazos organizados ainda esta semana.",
  cta: { label: "Criar conta grátis", href: "/login?modo=registro" },
};

export const rodape = {
  links: [
    { href: "#recursos", label: "Recursos" },
    { href: "#precos", label: "Preços" },
    { href: "/login", label: "Entrar" },
  ],
  selo: "Dados protegidos com 2FA e trilha de auditoria",
  copyright: (ano: number) => `© ${ano} Lexo. Todos os direitos reservados.`,
};
