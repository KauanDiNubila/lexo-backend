import { useEffect } from "react";
import { Link } from "react-router-dom";
import { Check, type LucideIcon } from "lucide-react";
import { useAuth } from "../lib/auth";
import { Button } from "@/components/ui/button";
import { GlowingEffect } from "@/components/ui/glowing-effect";
import Silk from "@/components/ui/silk";
import {
  marca as marcaConteudo,
  navegacao,
  hero,
  recursos,
  comoFunciona,
  lexoIA,
  depoimentos,
  precos,
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

const SUPERFICIES = {
  base: "bg-background",
  elevada: "bg-[#0a0a0a] border-y border-border",
  marca: "bg-[#08293d]",
} as const;

function Bloco({
  id,
  superficie = "base",
  className = "",
  children,
}: {
  id?: string;
  superficie?: keyof typeof SUPERFICIES;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <div className={`w-full ${SUPERFICIES[superficie]}`}>
      <Secao id={id} className={className}>
        {children}
      </Secao>
    </div>
  );
}

function RecursoItem({
  icone: Icone,
  titulo,
  texto,
}: {
  icone: LucideIcon;
  titulo: string;
  texto: string;
}) {
  return (
    <div className="relative h-full rounded-2xl border border-border p-2 md:rounded-3xl md:p-3">
      <GlowingEffect
        variant="white"
        spread={40}
        glow={true}
        disabled={false}
        proximity={64}
        inactiveZone={0.01}
      />
      <div className="relative flex h-full flex-col gap-4 overflow-hidden rounded-xl bg-card p-5 shadow-[inset_0_1px_0_rgba(255,255,255,0.06)]">
        <div className="grid h-11 w-11 place-items-center rounded-xl border border-border bg-secondary">
          <Icone size={22} strokeWidth={2.2} />
        </div>
        <h3 className="text-lg font-bold">{titulo}</h3>
        <p className="text-sm text-muted-foreground">{texto}</p>
      </div>
    </div>
  );
}

export function Landing() {
  const { usuario } = useAuth();

  useEffect(() => {
    document.documentElement.classList.add("scrollbar-fina");
    return () => document.documentElement.classList.remove("scrollbar-fina");
  }, []);

  return (
    <div className="dark min-h-screen" style={{ backgroundColor: "var(--color-bg)", color: "var(--color-text)" }}>
      <header className="sticky top-0 z-50 border-b border-border bg-background/75 backdrop-blur">
        <div className="mx-auto flex h-16 w-full max-w-6xl items-center justify-between px-6">
          <Marca />
          <nav className="hidden items-center gap-7 text-sm text-muted-foreground md:flex">
            {navegacao.map((item) => (
              <a key={item.href} href={item.href} className="hover:text-primary">
                {item.label}
              </a>
            ))}
          </nav>
          <div className="flex items-center gap-3">
            {usuario ? (
              <Button asChild size="sm" className="bg-primary text-primary-foreground hover:bg-primary/90">
                <Link to="/app">Ir para o app</Link>
              </Button>
            ) : (
              <>
                <Button asChild variant="ghost" size="sm" className="hidden sm:inline-flex">
                  <Link to="/login">Entrar</Link>
                </Button>
                <Button asChild size="sm" className="bg-primary text-primary-foreground hover:bg-primary/90">
                  <Link to="/login?modo=registro">Criar conta grátis</Link>
                </Button>
              </>
            )}
          </div>
        </div>
      </header>

      <div className="relative w-full overflow-hidden">
        <div aria-hidden className="pointer-events-none absolute top-0 left-0 h-[400px] w-full animate-in fade-in duration-500 md:h-[540px]">
          <Silk color="#0b3450" speed={5} scale={1.3} noiseIntensity={1.1} rotation={0.15} />
        </div>

        <div className="relative mx-auto flex h-[400px] w-full max-w-[1600px] flex-col justify-center px-8 text-left md:h-[540px] md:px-14">
          <h1 className="text-[clamp(3.5rem,13vw,13rem)] leading-[0.9] font-bold tracking-tighter">
            {marcaConteudo.nome}
          </h1>
          <p className="mt-6 max-w-2xl text-lg text-white/70 md:text-xl">{hero.subtitulo}</p>
        </div>

      </div>

      <Bloco id="recursos" className="py-24">
        <div className="text-center">
          <h2 className="text-3xl font-extrabold tracking-tight md:text-4xl">{recursos.titulo}</h2>
          <p className="mx-auto mt-3 max-w-2xl text-muted-foreground">{recursos.subtitulo}</p>
        </div>
        <div className="mt-12 grid gap-5 md:grid-cols-2 lg:grid-cols-3">
          {recursos.itens.map((r) => (
            <RecursoItem key={r.titulo} icone={r.icone} titulo={r.titulo} texto={r.texto} />
          ))}
        </div>
      </Bloco>

      <Bloco id="como-funciona" superficie="elevada" className="py-24">
        <div className="max-w-2xl">
          <h2 className="text-3xl font-extrabold tracking-tight md:text-4xl">{comoFunciona.titulo}</h2>
        </div>
        <div className="relative mt-16">
          <div aria-hidden className="absolute top-6 right-0 left-0 hidden h-px bg-border md:block" />
          <div className="grid gap-12 md:grid-cols-3 md:gap-8">
            {comoFunciona.passos.map((p) => (
              <div key={p.n} className="relative">
                <div className="grid h-12 w-12 place-items-center rounded-full border border-border bg-background text-sm font-bold text-foreground">
                  {p.n}
                </div>
                <h3 className="mt-5 text-lg font-bold">{p.titulo}</h3>
                <p className="mt-2 text-sm text-muted-foreground">{p.texto}</p>
              </div>
            ))}
          </div>
        </div>
      </Bloco>

      <Bloco id="ia" superficie="marca" className="py-24 md:py-32">
        <div className="grid gap-12 md:grid-cols-12 md:gap-16">
          <div className="md:col-span-7">
            <span className="text-xs font-semibold tracking-widest text-white/50 uppercase">{lexoIA.eyebrow}</span>
            <h2 className="mt-4 text-3xl font-extrabold tracking-tight text-white md:text-5xl">{lexoIA.titulo}</h2>
            <p className="mt-5 max-w-xl text-white/70">{lexoIA.texto}</p>
            <ul className="mt-8 space-y-3 text-sm text-white/90">
              {lexoIA.beneficios.map((i) => (
                <li key={i} className="flex items-start gap-2.5">
                  <Check size={16} strokeWidth={3} className="mt-1 shrink-0 text-white" />
                  <span>{i}</span>
                </li>
              ))}
            </ul>
          </div>
          <div className="grid grid-cols-2 gap-px overflow-hidden rounded-2xl border border-white/15 bg-white/15 self-center md:col-span-5">
            {lexoIA.metricas.map((m) => (
              <div key={m.rotulo} className="bg-[#08293d] p-6 text-center">
                <div className="text-3xl font-extrabold text-white">{m.valor}</div>
                <div className="mt-1 text-xs text-white/60">{m.rotulo}</div>
              </div>
            ))}
          </div>
        </div>
      </Bloco>

      <Bloco className="py-24">
        <div className="text-center">
          <h2 className="text-3xl font-extrabold tracking-tight md:text-4xl">{depoimentos.titulo}</h2>
        </div>
        <div className="mt-12 grid items-start gap-5 md:grid-cols-3">
          {depoimentos.itens.map((d, i) => (
            <div key={d.nome} className={`card-fundo p-6 ${i === 1 ? "md:mt-10" : ""}`}>
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

        <div className="mt-20 border-t border-border pt-12 text-center">
          <p className="text-xs tracking-widest text-muted-foreground uppercase">{depoimentos.logos.rotulo}</p>
          <div className="mt-6 flex flex-wrap items-center justify-center gap-x-10 gap-y-3 text-sm font-semibold text-muted-foreground">
            {depoimentos.logos.nomes.map((n) => (
              <span key={n}>{n}</span>
            ))}
          </div>
        </div>
      </Bloco>

      <Bloco id="precos" superficie="elevada" className="py-24">
        <div className="text-center">
          <h2 className="text-3xl font-extrabold tracking-tight md:text-4xl">{precos.titulo}</h2>
          <p className="mx-auto mt-3 max-w-2xl text-muted-foreground">{precos.subtitulo}</p>
        </div>
        <div className="mt-12 grid items-start gap-6 md:grid-cols-3">
          {precos.planos.map((p) => (
            <div
              key={p.nome}
              className={`card-fundo p-7 ${p.destaque ? "md:-mt-4 md:pb-11 border-foreground/60 shadow-[0_20px_60px_-30px_rgba(255,255,255,0.35)]" : ""}`}
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
                className={`mt-7 w-full ${p.destaque ? "bg-primary text-primary-foreground hover:bg-primary/90" : ""}`}
              >
                <Link to="/login?modo=registro">{p.preco ? "Começar agora" : "Falar com vendas"}</Link>
              </Button>
            </div>
          ))}
        </div>
      </Bloco>

      <footer className="border-t border-border">
        <Secao className="flex flex-col items-center justify-between gap-4 py-10 md:flex-row">
          <Marca tamanho={28} />
          <div className="flex flex-wrap items-center justify-center gap-x-6 gap-y-2 text-sm text-muted-foreground">
            {rodape.links.map((link) =>
              link.href.startsWith("/") ? (
                <Link key={link.href} to={link.href} className="hover:text-primary">
                  {link.label}
                </Link>
              ) : (
                <a key={link.href} href={link.href} className="hover:text-primary">
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
