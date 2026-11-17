import { useEffect, useSyncExternalStore } from "react";

export type Tema = "claro" | "escuro";

const CHAVE = "lexo-tema";
const ouvintes = new Set<() => void>();

function lerSalvo(): Tema {
  try {
    const salvo = localStorage.getItem(CHAVE);
    if (salvo === "claro" || salvo === "escuro") return salvo;
  } catch {
    return "claro";
  }
  return "claro";
}

let atual: Tema = lerSalvo();

function definir(tema: Tema) {
  atual = tema;
  try {
    localStorage.setItem(CHAVE, tema);
  } catch {
    return;
  } finally {
    ouvintes.forEach((o) => o());
  }
}

function assinar(ouvinte: () => void) {
  ouvintes.add(ouvinte);
  return () => ouvintes.delete(ouvinte);
}

export function useTema() {
  const tema = useSyncExternalStore(assinar, () => atual);
  return { tema, alternar: () => definir(tema === "claro" ? "escuro" : "claro") };
}

export function useAplicarTema() {
  const { tema } = useTema();
  useEffect(() => {
    const raiz = document.documentElement;
    raiz.classList.toggle("dark", tema === "escuro");
    return () => raiz.classList.add("dark");
  }, [tema]);
}
