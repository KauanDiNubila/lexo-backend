import { Link } from "react-router-dom";
import { useAuth } from "../lib/auth";

/** Marca (logo + nome), reutilizada na nav e no footer. */
function Marca({ tamanho = 34 }: { tamanho?: number }) {
  return (
    <div className="flex items-center gap-2.5">
      <div
        className="grid place-items-center font-extrabold text-white"
        style={{
          width: tamanho,
          height: tamanho,
          borderRadius: tamanho * 0.27,
          background: "linear-gradient(135deg,#6366f1,#8b5cf6)",
        }}
      >
        L
      </div>
      <span className="font-bold tracking-tight" style={{ fontSize: tamanho * 0.55 }}>
        Lexo
      </span>
    </div>
  );
}

function Secao({ id, children, className = "" }: { id?: string; children: React.ReactNode; className?: string }) {
  return (
    <section id={id} className={`mx-auto w-full max-w-6xl px-6 ${className}`}>
      {children}
    </section>
  );
}

const RECURSOS = [
  { icone: "📂", titulo: "Gestão de processos", texto: "Cada processo com partes, andamentos, documentos e prazos num só lugar — organizado por cliente e área." },
  { icone: "📅", titulo: "Controle de prazos", texto: "Agenda com alertas automáticos de prazos fatais. Nunca mais perca uma data por falta de aviso." },
  { icone: "🤖", titulo: "Lexo IA", texto: "Resumos de casos, sugestão de prazos e petições em segundos, com um assistente treinado para o jurídico." },
  { icone: "📊", titulo: "Jurimetria", texto: "Indicadores do escritório: processos por status, produtividade da equipe e valores em aberto." },
  { icone: "💰", titulo: "Financeiro", texto: "Honorários, faturas e recebimentos vinculados a cliente e processo, com relatório consolidado." },
  { icone: "🔐", titulo: "Portal do cliente", texto: "Seu cliente acompanha o andamento com transparência, sem precisar ligar para o escritório." },
];

const PASSOS = [
  { n: "01", titulo: "Migre seus dados", texto: "Importamos clientes e processos da sua planilha ou sistema atual. Migração assistida, sem retrabalho." },
  { n: "02", titulo: "Deixe a IA trabalhar", texto: "A Lexo IA resume os casos, sugere prazos e organiza a agenda automaticamente conforme você cadastra." },
  { n: "03", titulo: "Acompanhe e cresça", texto: "Painéis de jurimetria mostram gargalos e oportunidades para o escritório evoluir com dados." },
];

const METRICAS_IA = [
  { valor: "620+", rotulo: "escritórios usando" },
  { valor: "1.200+", rotulo: "processos monitorados" },
  { valor: "9h", rotulo: "economizadas por semana" },
  { valor: "99,9%", rotulo: "de disponibilidade" },
];

const DEPOIMENTOS = [
  { nome: "Dra. Helena Andrade", firma: "Andrade Advocacia", texto: "Cortamos pela metade o tempo gasto organizando prazos. A IA resume um processo novo melhor que meu estagiário." },
  { nome: "Dr. Rafael Mendonça", firma: "Mendonça & Cruz", texto: "O portal do cliente reduziu drasticamente as ligações de acompanhamento. Os clientes adoram a transparência." },
  { nome: "Dra. Beatriz Vector", firma: "Vector Legal", texto: "A jurimetria me deu clareza de onde o escritório perdia dinheiro. Hoje decido com dados, não no achismo." },
];

const PLANOS = [
  { nome: "Solo", preco: "79", desc: "Para advogados autônomos", itens: ["1 usuário", "Processos e prazos ilimitados", "Lexo IA (100 resumos/mês)", "Portal do cliente"], destaque: false },
  { nome: "Escritório", preco: "149", desc: "Para equipes em crescimento", itens: ["Até 10 usuários", "Tudo do Solo, sem limites de IA", "Jurimetria avançada", "Auditoria e permissões"], destaque: true },
  { nome: "Enterprise", preco: null, desc: "Para grandes bancas", itens: ["Usuários ilimitados", "SLA e suporte dedicado", "SSO e integrações", "Onboarding personalizado"], destaque: false },
];

export function Landing() {
  const { usuario } = useAuth();

  return (
    <div className="min-h-screen" style={{ backgroundColor: "var(--color-bg)" }}>
      {/* NAV */}
      <header
        className="sticky top-0 z-50 backdrop-blur"
        style={{ borderBottom: "1px solid var(--color-border)", background: "rgba(11,13,20,0.75)" }}
      >
        <div className="mx-auto flex h-16 w-full max-w-6xl items-center justify-between px-6">
          <Marca />
          <nav className="hidden items-center gap-7 text-sm md:flex" style={{ color: "var(--color-text-muted)" }}>
            <a href="#recursos" className="hover:text-white">Recursos</a>
            <a href="#como-funciona" className="hover:text-white">Como funciona</a>
            <a href="#ia" className="hover:text-white">Lexo IA</a>
            <a href="#precos" className="hover:text-white">Preços</a>
          </nav>
          <div className="flex items-center gap-3">
            {usuario ? (
              <Link to="/app" className="btn btn-primary">Ir para o app</Link>
            ) : (
              <>
                <Link to="/login" className="btn btn-ghost hidden sm:inline-flex">Entrar</Link>
                <Link to="/login?modo=registro" className="btn btn-primary">Criar conta grátis</Link>
              </>
            )}
          </div>
        </div>
      </header>

      {/* HERO */}
      <Secao className="pt-20 pb-16 text-center">
        <span
          className="inline-block rounded-full px-3 py-1 text-xs font-semibold"
          style={{ background: "var(--color-primary-soft)", color: "var(--color-primary)" }}
        >
          Gestão jurídica com inteligência artificial
        </span>
        <h1 className="mx-auto mt-6 max-w-3xl text-4xl font-extrabold leading-tight tracking-tight md:text-6xl">
          O escritório inteiro,{" "}
          <span style={{ background: "linear-gradient(135deg,#818cf8,#c084fc)", WebkitBackgroundClip: "text", backgroundClip: "text", color: "transparent" }}>
            unificado e inteligente
          </span>
        </h1>
        <p className="mx-auto mt-6 max-w-2xl text-lg" style={{ color: "var(--color-text-muted)" }}>
          Processos, prazos, financeiro e relacionamento com o cliente numa só plataforma —
          com a Lexo IA cuidando do trabalho repetitivo para você advogar.
        </p>
        <div className="mt-9 flex flex-wrap items-center justify-center gap-3">
          <Link to="/login?modo=registro" className="btn btn-primary" style={{ padding: "0.7rem 1.4rem", fontSize: "0.95rem" }}>
            Começar teste de 14 dias
          </Link>
          <Link to="/login" className="btn btn-ghost" style={{ padding: "0.7rem 1.4rem", fontSize: "0.95rem" }}>
            Área do cliente
          </Link>
        </div>
        <div className="mt-6 flex flex-wrap items-center justify-center gap-x-6 gap-y-2 text-xs" style={{ color: "var(--color-text-muted)" }}>
          <span>✓ Sem cartão de crédito</span>
          <span>✓ Migração assistida</span>
          <span>✓ LGPD & ISO 27001</span>
        </div>

        {/* Prévia do painel */}
        <div className="card mx-auto mt-14 max-w-4xl overflow-hidden text-left" style={{ boxShadow: "0 30px 80px -40px rgba(99,102,241,0.5)" }}>
          <div className="flex items-center gap-2 px-5 py-3" style={{ borderBottom: "1px solid var(--color-border)" }}>
            <span className="h-3 w-3 rounded-full" style={{ background: "#f87171" }} />
            <span className="h-3 w-3 rounded-full" style={{ background: "#fbbf24" }} />
            <span className="h-3 w-3 rounded-full" style={{ background: "#34d399" }} />
            <span className="ml-3 text-xs" style={{ color: "var(--color-text-muted)" }}>app.lexo.com.br</span>
          </div>
          <div className="grid gap-4 p-6 md:grid-cols-3">
            {[
              { v: "148", r: "Processos ativos", c: "var(--color-primary)" },
              { v: "9", r: "Prazos urgentes", c: "var(--color-warning)" },
              { v: "312h", r: "Horas faturáveis no mês", c: "var(--color-success)" },
            ].map((m) => (
              <div key={m.r} className="card p-4" style={{ background: "var(--color-surface-2)" }}>
                <div className="text-3xl font-extrabold" style={{ color: m.c }}>{m.v}</div>
                <div className="mt-1 text-sm" style={{ color: "var(--color-text-muted)" }}>{m.r}</div>
              </div>
            ))}
          </div>
          <div className="mx-6 mb-6 rounded-xl p-4" style={{ background: "var(--color-primary-soft)", border: "1px solid var(--color-border)" }}>
            <div className="mb-1 text-xs font-semibold" style={{ color: "var(--color-primary)" }}>🤖 Resumo da Lexo IA</div>
            <p className="text-sm" style={{ color: "var(--color-text)" }}>
              3 processos entram em prazo fatal nesta semana. O caso <b>0001-23.2026</b> aguarda contestação
              até sexta. Recomendo priorizar a defesa de <b>Andrade vs. Município</b>.
            </p>
          </div>
        </div>

        {/* Prova social */}
        <div className="mt-14">
          <p className="text-xs uppercase tracking-widest" style={{ color: "var(--color-text-muted)" }}>
            Escritórios que confiam na Lexo
          </p>
          <div className="mt-5 flex flex-wrap items-center justify-center gap-x-10 gap-y-3 text-sm font-semibold" style={{ color: "var(--color-text-muted)" }}>
            {["Andrade Adv.", "Mendonça & Cruz", "Vector Legal", "Bittencourt Adv.", "Núcleo Jurídico"].map((n) => (
              <span key={n}>{n}</span>
            ))}
          </div>
        </div>
      </Secao>

      {/* RECURSOS */}
      <Secao id="recursos" className="py-20">
        <div className="text-center">
          <h2 className="text-3xl font-extrabold tracking-tight md:text-4xl">Tudo que o escritório precisa</h2>
          <p className="mx-auto mt-3 max-w-2xl" style={{ color: "var(--color-text-muted)" }}>
            Uma plataforma única no lugar de cinco ferramentas desconexas.
          </p>
        </div>
        <div className="mt-12 grid gap-5 md:grid-cols-2 lg:grid-cols-3">
          {RECURSOS.map((r) => (
            <div key={r.titulo} className="card p-6 transition-transform hover:-translate-y-1">
              <div className="grid h-11 w-11 place-items-center rounded-xl text-xl" style={{ background: "var(--color-primary-soft)" }}>
                {r.icone}
              </div>
              <h3 className="mt-4 text-lg font-bold">{r.titulo}</h3>
              <p className="mt-2 text-sm" style={{ color: "var(--color-text-muted)" }}>{r.texto}</p>
            </div>
          ))}
        </div>
      </Secao>

      {/* COMO FUNCIONA */}
      <Secao id="como-funciona" className="py-20">
        <div className="text-center">
          <h2 className="text-3xl font-extrabold tracking-tight md:text-4xl">Do caos à clareza em 3 passos</h2>
        </div>
        <div className="mt-12 grid gap-6 md:grid-cols-3">
          {PASSOS.map((p) => (
            <div key={p.n} className="relative">
              <div className="text-5xl font-extrabold" style={{ color: "var(--color-primary-soft)" }}>{p.n}</div>
              <h3 className="mt-2 text-lg font-bold">{p.titulo}</h3>
              <p className="mt-2 text-sm" style={{ color: "var(--color-text-muted)" }}>{p.texto}</p>
            </div>
          ))}
        </div>
      </Secao>

      {/* LEXO IA */}
      <Secao id="ia" className="py-20">
        <div className="card grid gap-10 p-8 md:grid-cols-2 md:p-12">
          <div>
            <span className="text-xs font-semibold" style={{ color: "var(--color-primary)" }}>LEXO IA</span>
            <h2 className="mt-3 text-3xl font-extrabold tracking-tight">Um assistente jurídico que nunca dorme</h2>
            <p className="mt-4" style={{ color: "var(--color-text-muted)" }}>
              A Lexo IA lê seus processos, resume o essencial, sugere os próximos prazos e ainda ajuda a
              redigir peças. Você revisa e aprova — a máquina cuida do trabalho braçal.
            </p>
            <ul className="mt-6 space-y-3 text-sm">
              {["Resumo automático de qualquer processo", "Sugestão de prazos a partir dos andamentos", "Rascunho de petições e notificações", "Análise de jurimetria em linguagem natural"].map((i) => (
                <li key={i} className="flex items-start gap-2">
                  <span style={{ color: "var(--color-success)" }}>✓</span>
                  <span>{i}</span>
                </li>
              ))}
            </ul>
          </div>
          <div className="grid grid-cols-2 gap-4 self-center">
            {METRICAS_IA.map((m) => (
              <div key={m.rotulo} className="card p-5 text-center" style={{ background: "var(--color-surface-2)" }}>
                <div className="text-3xl font-extrabold" style={{ color: "var(--color-primary)" }}>{m.valor}</div>
                <div className="mt-1 text-xs" style={{ color: "var(--color-text-muted)" }}>{m.rotulo}</div>
              </div>
            ))}
          </div>
        </div>
      </Secao>

      {/* DEPOIMENTOS */}
      <Secao className="py-20">
        <div className="text-center">
          <h2 className="text-3xl font-extrabold tracking-tight md:text-4xl">Quem usa, recomenda</h2>
        </div>
        <div className="mt-12 grid gap-5 md:grid-cols-3">
          {DEPOIMENTOS.map((d) => (
            <div key={d.nome} className="card p-6">
              <p className="text-sm leading-relaxed">"{d.texto}"</p>
              <div className="mt-5 flex items-center gap-3">
                <div className="grid h-9 w-9 place-items-center rounded-full text-xs font-bold" style={{ background: "var(--color-primary-soft)", color: "var(--color-primary)" }}>
                  {d.nome.split(" ").slice(-2).map((p) => p[0]).join("")}
                </div>
                <div>
                  <div className="text-sm font-semibold">{d.nome}</div>
                  <div className="text-xs" style={{ color: "var(--color-text-muted)" }}>{d.firma}</div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </Secao>

      {/* PREÇOS */}
      <Secao id="precos" className="py-20">
        <div className="text-center">
          <h2 className="text-3xl font-extrabold tracking-tight md:text-4xl">Preços que cabem no escritório</h2>
          <p className="mx-auto mt-3 max-w-2xl" style={{ color: "var(--color-text-muted)" }}>
            Por usuário/mês. Comece grátis por 14 dias, sem cartão.
          </p>
        </div>
        <div className="mt-12 grid gap-6 md:grid-cols-3">
          {PLANOS.map((p) => (
            <div
              key={p.nome}
              className="card p-7"
              style={p.destaque ? { borderColor: "var(--color-primary)", boxShadow: "0 20px 60px -30px rgba(99,102,241,0.6)" } : undefined}
            >
              {p.destaque && (
                <span className="mb-3 inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold" style={{ background: "var(--color-primary)", color: "white" }}>
                  Mais popular
                </span>
              )}
              <h3 className="text-lg font-bold">{p.nome}</h3>
              <p className="text-sm" style={{ color: "var(--color-text-muted)" }}>{p.desc}</p>
              <div className="mt-4">
                {p.preco ? (
                  <>
                    <span className="text-4xl font-extrabold">R${p.preco}</span>
                    <span className="text-sm" style={{ color: "var(--color-text-muted)" }}> /usuário/mês</span>
                  </>
                ) : (
                  <span className="text-4xl font-extrabold">Sob consulta</span>
                )}
              </div>
              <ul className="mt-6 space-y-2.5 text-sm">
                {p.itens.map((i) => (
                  <li key={i} className="flex items-start gap-2">
                    <span style={{ color: "var(--color-success)" }}>✓</span>
                    <span>{i}</span>
                  </li>
                ))}
              </ul>
              <Link
                to="/login?modo=registro"
                className={`btn ${p.destaque ? "btn-primary" : "btn-ghost"} mt-7 w-full`}
              >
                {p.preco ? "Começar agora" : "Falar com vendas"}
              </Link>
            </div>
          ))}
        </div>
      </Secao>

      {/* CTA FINAL */}
      <Secao className="py-20">
        <div
          className="card overflow-hidden p-10 text-center md:p-16"
          style={{ background: "linear-gradient(135deg, var(--color-primary-soft), var(--color-surface))" }}
        >
          <h2 className="mx-auto max-w-2xl text-3xl font-extrabold tracking-tight md:text-4xl">
            Pronto para dar à sua banca a inteligência que ela merece?
          </h2>
          <p className="mx-auto mt-4 max-w-xl" style={{ color: "var(--color-text-muted)" }}>
            Migração assistida e 14 dias grátis. Seus processos organizados ainda esta semana.
          </p>
          <Link to="/login?modo=registro" className="btn btn-primary mt-8" style={{ padding: "0.75rem 1.6rem", fontSize: "0.95rem" }}>
            Criar conta grátis
          </Link>
        </div>
      </Secao>

      {/* FOOTER */}
      <footer style={{ borderTop: "1px solid var(--color-border)" }}>
        <Secao className="flex flex-col items-center justify-between gap-4 py-10 md:flex-row">
          <Marca tamanho={28} />
          <div className="flex flex-wrap items-center justify-center gap-x-6 gap-y-2 text-sm" style={{ color: "var(--color-text-muted)" }}>
            <a href="#recursos" className="hover:text-white">Recursos</a>
            <a href="#precos" className="hover:text-white">Preços</a>
            <Link to="/login" className="hover:text-white">Entrar</Link>
            <span>LGPD & ISO 27001</span>
          </div>
          <div className="text-xs" style={{ color: "var(--color-text-muted)" }}>
            © {new Date().getFullYear()} Lexo. Todos os direitos reservados.
          </div>
        </Secao>
      </footer>
    </div>
  );
}
