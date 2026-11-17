import { cn } from "@/lib/utils";

export type TomStatus = "ok" | "neutro" | "alerta";

const ponto: Record<TomStatus, string> = {
  ok: "bg-foreground",
  neutro: "border border-foreground/50",
  alerta: "bg-destructive",
};

const texto: Record<TomStatus, string> = {
  ok: "text-foreground",
  neutro: "text-muted-foreground",
  alerta: "font-semibold text-destructive",
};

export function Status({ rotulo, tom = "neutro" }: { rotulo: string; tom?: TomStatus }) {
  return (
    <span className={cn("inline-flex items-center gap-2 text-[13px]", texto[tom])}>
      <span className={cn("size-2 shrink-0 rounded-full", ponto[tom])} />
      {rotulo}
    </span>
  );
}

export const tomProcesso = (status: string): TomStatus => (status === "ATIVO" ? "ok" : "neutro");

export const tomHonorario = (status: string): TomStatus =>
  status === "PAGO" ? "ok" : status === "ATRASADO" ? "alerta" : "neutro";

export const tomRisco = (risco: string): TomStatus =>
  risco === "CRITICO" || risco === "URGENTE" ? "alerta" : risco === "ALTO" ? "ok" : "neutro";
