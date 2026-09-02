import { Link } from "react-router-dom";
import { Sparkles, Check, ArrowRight } from "lucide-react";
import { useAuth } from "../lib/auth";
import { Button } from "@/components/ui/button";
import {
  marca as marcaConteudo,
  navegacao,
  hero,
  recursos,
  comoFunciona,
  lexoIA,
  depoimentos,
  precos,
  ctaFinal,
  rodape,
} from "@/content/landing";

function LogoMark({ tamanho }: { tamanho: number }) {
  return (
    <svg viewBox="0 0 24 24" width={tamanho} height={tamanho} fill="none" aria-hidden>
      <path
        d="M7 4V17C7 18.66 8.34 20 10 20H18"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function Marca({ tamanho = 34 }: { tamanho?: number }) {
  return (
    <div className="flex items-center gap-2.5">
      <div
        className="grid place-items-center rounded-[27%] bg-white text-[#0b1220]"
        style={{ width: tamanho, height: tamanho }}
      >
        <LogoMark tamanho={tamanho * 0.58} />
      </div>
      <span className="font-bold tracking-tight" style={{ fontSize: tamanho * 0.55 }}>
        {marcaConteudo.nome}
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

export function Landing() {
  const { usuario } = useAuth();

  return (
    <div className="dark min-h-screen" style={{ backgroundColor: "var(--color-bg)", color: "var(--color-text)" }}>

      <header className="sticky top-0 z-50 border-b border-border bg-background/75 backdrop-blur">
        <div className="mx-auto flex h-16 w-full max-w-6xl items-center justify-between px-6">
          <Marca />
          <nav className="hidden items-center gap-7 text-sm text-muted-foreground md:flex">
            {navegacao.map((item) => (
              <a key={item.href} href={item.href} className="hover:text-foreground">
                {item.label}
              </a>
            ))}
          </nav>
          <div className="flex items-center gap-3">
            {usuario ? (
              <Button asChild size="sm" className="bg-white text-black hover:bg-white/85">
                <Link to="/app">Ir para o app</Link>
              </Button>
            ) : (
              <>
                <Button asChild variant="ghost" size="sm" className="hidden sm:inline-flex">
                  <Link to="/login">Entrar</Link>
                </Button>
                <Button asChild size="sm" className="bg-white text-black hover:bg-white/85">
                  <Link to="/login?modo=registro">Criar conta grátis</Link>
                </Button>
              </>
            )}
          </div>
        </div>
      </header>

      <Secao className="relative overflow-hidden pt-24 pb-16 text-left">
        <div
          aria-hidden
          className="pointer-events-none absolute -right-24 bottom-0 -z-10 h-96 w-96 rounded-full bg-white/10 blur-3xl"
        />

        <span className="inline-flex items-center gap-1.5 rounded-full border border-border bg-secondary px-3 py-1 text-xs font-medium text-muted-foreground">
          {hero.badge}
        </span>
        <h1 className="mt-6 max-w-4xl text-5xl leading-[1.05] font-bold tracking-tight md:text-7xl">
          {hero.tituloParte1}
          <br />
          {hero.tituloParte2}
        </h1>
        <p className="mt-6 max-w-2xl text-lg text-muted-foreground">{hero.subtitulo}</p>
        <div className="mt-9 flex flex-wrap items-center justify-start gap-3">
          <Button asChild size="lg" className="bg-white px-6 text-[0.95rem] text-black hover:bg-white/85">
            <Link to={hero.ctaPrimaria.href}>
              {hero.ctaPrimaria.label}
              <ArrowRight className="size-4" />
            </Link>
          </Button>
          <Button asChild variant="secondary" size="lg" className="px-6 text-[0.95rem]">
            <Link to={hero.ctaSecundaria.href}>{hero.ctaSecundaria.label}</Link>
          </Button>
        </div>
        <div className="mt-6 flex flex-wrap items-center justify-start gap-x-6 gap-y-2 text-xs text-muted-foreground">
          {hero.selos.map((t) => (
            <span key={t} className="inline-flex items-center gap-1.5">
              <Check size={14} strokeWidth={3} className="text-foreground" /> {t}
            </span>
          ))}
        </div>

        <div className="card relative z-10 mt-14 max-w-4xl overflow-hidden text-left shadow-[0_30px_80px_-30px_rgba(255,255,255,0.15)]">
          <div className="flex items-center gap-2 border-b border-border px-5 py-3">
            <span className="h-3 w-3 rounded-full bg-destructive" />
            <span className="h-3 w-3 rounded-full bg-warning" />
            <span className="h-3 w-3 rounded-full bg-success" />
            <span className="ml-3 text-xs text-muted-foreground">{hero.mockup.urlFake}</span>
          </div>
          <div className="grid gap-4 p-6 md:grid-cols-3">
            {hero.mockup.metricas.map((m) => (
              <div key={m.rotulo} className="card bg-muted p-4">
                <div className="text-3xl font-extrabold text-foreground">{m.valor}</div>
                <div className="mt-1 text-sm text-muted-foreground">{m.rotulo}</div>
              </div>
            ))}
          </div>
          <div className="mx-6 mb-6 rounded-xl border border-border bg-secondary p-4">
            <div className="mb-1 flex items-center gap-1.5 text-xs font-semibold text-foreground">
              <Sparkles size={14} strokeWidth={2.4} /> {hero.mockup.resumoIA.titulo}
            </div>
            <p className="text-sm text-foreground">{hero.mockup.resumoIA.texto}</p>
          </div>
        </div>

        <div className="mt-14">
          <p className="text-xs tracking-widest text-muted-foreground uppercase">{hero.logosConfianca.rotulo}</p>
          <div className="mt-5 flex flex-wrap items-center justify-start gap-x-10 gap-y-3 text-sm font-semibold text-muted-foreground">
            {hero.logosConfianca.nomes.map((n) => (
              <span key={n}>{n}</span>
            ))}
          </div>
        </div>
      </Secao>

      <Secao id="recursos" className="py-20">
        <div className="text-center">
          <h2 className="text-3xl font-extrabold tracking-tight md:text-4xl">{recursos.titulo}</h2>
          <p className="mx-auto mt-3 max-w-2xl text-muted-foreground">{recursos.subtitulo}</p>
        </div>
        <div className="mt-12 grid gap-5 md:grid-cols-2 lg:grid-cols-3">
          {recursos.itens.map((r) => (
            <div key={r.titulo} className="card p-6 transition-transform hover:-translate-y-1">
              <div className="grid h-11 w-11 place-items-center rounded-xl bg-secondary text-foreground">
                <r.icone size={22} strokeWidth={2.2} />
              </div>
              <h3 className="mt-4 text-lg font-bold">{r.titulo}</h3>
              <p className="mt-2 text-sm text-muted-foreground">{r.texto}</p>
            </div>
          ))}
        </div>
      </Secao>

      <Secao id="como-funciona" className="py-20">
        <div className="text-center">
          <h2 className="text-3xl font-extrabold tracking-tight md:text-4xl">{comoFunciona.titulo}</h2>
        </div>
        <div className="mt-12 grid gap-6 md:grid-cols-3">
          {comoFunciona.passos.map((p) => (
            <div key={p.n} className="relative">
              <div className="text-5xl font-extrabold text-muted-foreground/40">{p.n}</div>
              <h3 className="mt-2 text-lg font-bold">{p.titulo}</h3>
              <p className="mt-2 text-sm text-muted-foreground">{p.texto}</p>
            </div>
          ))}
        </div>
      </Secao>

      <Secao id="ia" className="py-20">
        <div className="card grid gap-10 p-8 md:grid-cols-2 md:p-12">
          <div>
            <span className="text-xs font-semibold text-muted-foreground">{lexoIA.eyebrow}</span>
            <h2 className="mt-3 text-3xl font-extrabold tracking-tight">{lexoIA.titulo}</h2>
            <p className="mt-4 text-muted-foreground">{lexoIA.texto}</p>
            <ul className="mt-6 space-y-3 text-sm">
              {lexoIA.beneficios.map((i) => (
                <li key={i} className="flex items-start gap-2">
                  <Check size={16} strokeWidth={3} className="mt-1 shrink-0 text-foreground" />
                  <span>{i}</span>
                </li>
              ))}
            </ul>
          </div>
          <div className="grid grid-cols-2 gap-4 self-center">
            {lexoIA.metricas.map((m) => (
              <div key={m.rotulo} className="card bg-muted p-5 text-center">
                <div className="text-3xl font-extrabold text-foreground">{m.valor}</div>
                <div className="mt-1 text-xs text-muted-foreground">{m.rotulo}</div>
              </div>
            ))}
          </div>
        </div>
      </Secao>

      <Secao className="py-20">
        <div className="text-center">
          <h2 className="text-3xl font-extrabold tracking-tight md:text-4xl">{depoimentos.titulo}</h2>
        </div>
        <div className="mt-12 grid gap-5 md:grid-cols-3">
          {depoimentos.itens.map((d) => (
            <div key={d.nome} className="card p-6">
              <p className="text-sm leading-relaxed">"{d.texto}"</p>
              <div className="mt-5 flex items-center gap-3">
                <div className="grid h-9 w-9 place-items-center rounded-full bg-secondary text-xs font-bold text-foreground">
                  {d.nome.split(" ").slice(-2).map((p) => p[0]).join("")}
                </div>
                <div>
                  <div className="text-sm font-semibold">{d.nome}</div>
                  <div className="text-xs text-muted-foreground">{d.firma}</div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </Secao>

      <Secao id="precos" className="py-20">
        <div className="text-center">
          <h2 className="text-3xl font-extrabold tracking-tight md:text-4xl">{precos.titulo}</h2>
          <p className="mx-auto mt-3 max-w-2xl text-muted-foreground">{precos.subtitulo}</p>
        </div>
        <div className="mt-12 grid gap-6 md:grid-cols-3">
          {precos.planos.map((p) => (
            <div
              key={p.nome}
              className={`card p-7 ${p.destaque ? "border-foreground/60 shadow-[0_20px_60px_-30px_rgba(255,255,255,0.35)]" : ""}`}
            >
              {p.destaque && (
                <span className="mb-3 inline-block rounded-full bg-foreground px-2.5 py-0.5 text-xs font-semibold text-background">
                  Mais popular
                </span>
              )}
              <h3 className="text-lg font-bold">{p.nome}</h3>
              <p className="text-sm text-muted-foreground">{p.desc}</p>
              <div className="mt-4">
                {p.preco ? (
                  <>
                    <span className="text-4xl font-extrabold">R${p.preco}</span>
                    <span className="text-sm text-muted-foreground"> /usuário/mês</span>
                  </>
                ) : (
                  <span className="text-4xl font-extrabold">Sob consulta</span>
                )}
              </div>
              <ul className="mt-6 space-y-2.5 text-sm">
                {p.itens.map((i) => (
                  <li key={i} className="flex items-start gap-2">
                    <Check size={16} strokeWidth={3} className="mt-1 shrink-0 text-foreground" />
                    <span>{i}</span>
                  </li>
                ))}
              </ul>
              <Button
                asChild
                variant={p.destaque ? "default" : "secondary"}
                className={`mt-7 w-full ${p.destaque ? "bg-white text-black hover:bg-white/85" : ""}`}
              >
                <Link to="/login?modo=registro">{p.preco ? "Começar agora" : "Falar com vendas"}</Link>
              </Button>
            </div>
          ))}
        </div>
      </Secao>

      <Secao className="py-20">
        <div className="card overflow-hidden bg-secondary p-10 text-center md:p-16">
          <h2 className="mx-auto max-w-2xl text-3xl font-extrabold tracking-tight md:text-4xl">
            {ctaFinal.titulo}
          </h2>
          <p className="mx-auto mt-4 max-w-xl text-muted-foreground">{ctaFinal.texto}</p>
          <Button asChild size="lg" className="mt-8 bg-white px-6 text-[0.95rem] text-black hover:bg-white/85">
            <Link to={ctaFinal.cta.href}>{ctaFinal.cta.label}</Link>
          </Button>
        </div>
      </Secao>

      <footer className="border-t border-border">
        <Secao className="flex flex-col items-center justify-between gap-4 py-10 md:flex-row">
          <Marca tamanho={28} />
          <div className="flex flex-wrap items-center justify-center gap-x-6 gap-y-2 text-sm text-muted-foreground">
            {rodape.links.map((link) =>
              link.href.startsWith("/") ? (
                <Link key={link.href} to={link.href} className="hover:text-foreground">
                  {link.label}
                </Link>
              ) : (
                <a key={link.href} href={link.href} className="hover:text-foreground">
                  {link.label}
                </a>
              ),
            )}
            <span>{rodape.selo}</span>
          </div>
          <div className="text-xs text-muted-foreground">{rodape.copyright(new Date().getFullYear())}</div>
        </Secao>
      </footer>
    </div>
  );
}
