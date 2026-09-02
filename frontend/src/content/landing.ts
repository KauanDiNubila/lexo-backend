/**
 * Conteúdo textual/dados da landing page, extraído da versão atual de
 * src/pages/Landing.tsx antes da reforma visual completa. A estética vai
 * mudar do zero (shadcn/ui + Aceternity UI) — isto é só o que é exibido,
 * pra não se perder na troca.
 */
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
  badge: "Gestão jurídica com inteligência artificial",
  // A segunda parte do título era destacada com um gradiente índigo/violeta —
  // esse tratamento específico não necessariamente se repete no novo visual.
  tituloParte1: "O escritório inteiro,",
  tituloParte2: "unificado e inteligente",
  subtitulo:
    "Processos, prazos, financeiro e relacionamento com o cliente numa só plataforma — " +
    "com a Lexo IA cuidando do trabalho repetitivo para você advogar.",
  ctaPrimaria: { label: "Começar teste de 14 dias", href: "/login?modo=registro" },
  ctaSecundaria: { label: "Área do cliente", href: "/login" },
  selos: ["Sem cartão de crédito", "Migração assistida", "LGPD & ISO 27001"],
  mockup: {
    urlFake: "app.lexo.com.br",
    metricas: [
      { valor: "148", rotulo: "Processos ativos" },
      { valor: "9", rotulo: "Prazos urgentes" },
      { valor: "312h", rotulo: "Horas faturáveis no mês" },
    ],
    resumoIA: {
      titulo: "Resumo da Lexo IA",
      // HTML: "0001-23.2026" e "Andrade vs. Município" apareciam em negrito
      texto:
        "3 processos entram em prazo fatal nesta semana. O caso 0001-23.2026 aguarda contestação " +
        "até sexta. Recomendo priorizar a defesa de Andrade vs. Município.",
    },
  },
  logosConfianca: {
    rotulo: "Escritórios que confiam na Lexo",
    nomes: ["Andrade Adv.", "Mendonça & Cruz", "Vector Legal", "Bittencourt Adv.", "Núcleo Jurídico"],
  },
};

export const recursos: {
  titulo: string;
  subtitulo: string;
  itens: { icone: LucideIcon; titulo: string; texto: string }[];
} = {
  titulo: "Tudo que o escritório precisa",
  subtitulo: "Uma plataforma única no lugar de cinco ferramentas desconexas.",
  itens: [
    { icone: Briefcase, titulo: "Gestão de processos", texto: "Cada processo com partes, andamentos, documentos e prazos num só lugar — organizado por cliente e área." },
    { icone: CalendarClock, titulo: "Controle de prazos", texto: "Agenda com alertas automáticos de prazos fatais. Nunca mais perca uma data por falta de aviso." },
    { icone: Sparkles, titulo: "Lexo IA", texto: "Resumos de casos, sugestão de prazos e petições em segundos, com um assistente treinado para o jurídico." },
    { icone: BarChart3, titulo: "Jurimetria", texto: "Indicadores do escritório: processos por status, produtividade da equipe e valores em aberto." },
    { icone: Wallet, titulo: "Financeiro", texto: "Honorários, faturas e recebimentos vinculados a cliente e processo, com relatório consolidado." },
    { icone: ShieldCheck, titulo: "Portal do cliente", texto: "Seu cliente acompanha o andamento com transparência, sem precisar ligar para o escritório." },
  ],
};

export const comoFunciona = {
  titulo: "Do caos à clareza em 3 passos",
  passos: [
    { n: "01", titulo: "Migre seus dados", texto: "Importamos clientes e processos da sua planilha ou sistema atual. Migração assistida, sem retrabalho." },
    { n: "02", titulo: "Deixe a IA trabalhar", texto: "A Lexo IA resume os casos, sugere prazos e organiza a agenda automaticamente conforme você cadastra." },
    { n: "03", titulo: "Acompanhe e cresça", texto: "Painéis de jurimetria mostram gargalos e oportunidades para o escritório evoluir com dados." },
  ],
};

export const lexoIA = {
  eyebrow: "LEXO IA",
  titulo: "Um assistente jurídico que nunca dorme",
  texto:
    "A Lexo IA lê seus processos, resume o essencial, sugere os próximos prazos e ainda ajuda a " +
    "redigir peças. Você revisa e aprova — a máquina cuida do trabalho braçal.",
  beneficios: [
    "Resumo automático de qualquer processo",
    "Sugestão de prazos a partir dos andamentos",
    "Rascunho de petições e notificações",
    "Análise de jurimetria em linguagem natural",
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
    { nome: "Dra. Beatriz Vector", firma: "Vector Legal", texto: "A jurimetria me deu clareza de onde o escritório perdia dinheiro. Hoje decido com dados, não no achismo." },
  ],
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
      itens: ["Até 10 usuários", "Tudo do Solo, sem limites de IA", "Jurimetria avançada", "Auditoria e permissões"],
      destaque: true,
    },
    {
      nome: "Enterprise",
      preco: null as string | null,
      desc: "Para grandes bancas",
      itens: ["Usuários ilimitados", "SLA e suporte dedicado", "SSO e integrações", "Onboarding personalizado"],
      destaque: false,
    },
  ],
};

export const ctaFinal = {
  titulo: "Pronto para dar à sua banca a inteligência que ela merece?",
  texto: "Migração assistida e 14 dias grátis. Seus processos organizados ainda esta semana.",
  cta: { label: "Criar conta grátis", href: "/login?modo=registro" },
};

export const rodape = {
  links: [
    { href: "#recursos", label: "Recursos" },
    { href: "#precos", label: "Preços" },
    { href: "/login", label: "Entrar" },
  ],
  selo: "LGPD & ISO 27001",
  copyright: (ano: number) => `© ${ano} Lexo. Todos os direitos reservados.`,
};
