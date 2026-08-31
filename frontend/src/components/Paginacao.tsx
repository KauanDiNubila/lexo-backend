type Props = {
  page: number;
  totalPages: number;
  totalElements: number;
  onChange: (page: number) => void;
};

export function Paginacao({ page, totalPages, totalElements, onChange }: Props) {
  if (totalPages <= 1) return null;

  const botao = (rotulo: string, destino: number, desabilitado: boolean) => (
    <button
      onClick={() => onChange(destino)}
      disabled={desabilitado}
      style={{
        background: "none",
        border: "1px solid var(--color-border)",
        borderRadius: 8,
        padding: "0.35rem 0.7rem",
        color: desabilitado ? "var(--color-text-muted)" : "var(--color-text)",
        cursor: desabilitado ? "default" : "pointer",
        fontSize: 13,
      }}
    >
      {rotulo}
    </button>
  );

  return (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        gap: 12,
        marginTop: 14,
        fontSize: 13,
        color: "var(--color-text-muted)",
      }}
    >
      <span>
        Página {page + 1} de {totalPages} · {totalElements} registro(s)
      </span>
      <div style={{ display: "flex", gap: 8 }}>
        {botao("Anterior", page - 1, page <= 0)}
        {botao("Próxima", page + 1, page >= totalPages - 1)}
      </div>
    </div>
  );
}
