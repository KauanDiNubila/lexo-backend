import { Button } from "@/components/ui/button";

type Props = {
  page: number;
  totalPages: number;
  totalElements: number;
  onChange: (page: number) => void;
};

export function Paginacao({ page, totalPages, totalElements, onChange }: Props) {
  if (totalPages <= 1) return null;

  return (
    <div className="mt-3.5 flex items-center justify-between gap-3 text-[13px] text-muted-foreground">
      <span>
        Página {page + 1} de {totalPages} · {totalElements} registro(s)
      </span>
      <div className="flex gap-2">
        <Button variant="outline" size="sm" onClick={() => onChange(page - 1)} disabled={page <= 0}>
          Anterior
        </Button>
        <Button variant="outline" size="sm" onClick={() => onChange(page + 1)} disabled={page >= totalPages - 1}>
          Próxima
        </Button>
      </div>
    </div>
  );
}
