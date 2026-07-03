import { useEffect, useRef, useState, type FormEvent } from "react";
import { Sparkles, Send } from "lucide-react";
import { api } from "../lib/api";

type Msg = { papel: "user" | "assistant"; texto: string };

const SUGESTOES = [
  "Como funciona a contestação no processo civil?",
  "Explique, em linguagem simples, o que é jurimetria.",
  "Quais os prazos prescricionais trabalhistas mais comuns?",
];

export function Assistente() {
  const [mensagens, setMensagens] = useState<Msg[]>([]);
  const [input, setInput] = useState("");
  const [enviando, setEnviando] = useState(false);
  const fimRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    fimRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [mensagens, enviando]);

  async function perguntar(texto: string) {
    if (!texto.trim() || enviando) return;
    const novas: Msg[] = [...mensagens, { papel: "user", texto: texto.trim() }];
    setMensagens(novas);
    setInput("");
    setEnviando(true);
    try {
      const r = await api.post<{ resposta: string; fonte: string }>("/api/ia/chat", { mensagens: novas });
      setMensagens([...novas, { papel: "assistant", texto: r.resposta }]);
    } catch {
      setMensagens([...novas, { papel: "assistant", texto: "Não consegui responder agora. Tente novamente em instantes." }]);
    } finally {
      setEnviando(false);
    }
  }

  function enviar(e: FormEvent) {
    e.preventDefault();
    perguntar(input);
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", height: "calc(100vh - 140px)", maxWidth: 820, margin: "0 auto" }}>
      <div style={{ marginBottom: 16 }}>
        <h1 style={{ fontSize: 24, fontWeight: 700, margin: 0, display: "flex", alignItems: "center", gap: 10 }}>
          <span className="grid h-8 w-8 place-items-center rounded-lg text-white" style={{ background: "linear-gradient(135deg,#6366f1,#8b5cf6)" }}>
            <Sparkles size={17} />
          </span>
          Assistente
        </h1>
        <p style={{ color: "var(--color-text-muted)", margin: "4px 0 0", fontSize: 14 }}>
          Tire dúvidas jurídicas com a Lexo IA · via <code>ia-service</code>
        </p>
      </div>

      {/* Área das mensagens */}
      <div className="card" style={{ flex: 1, overflowY: "auto", padding: "1.25rem", display: "flex", flexDirection: "column", gap: 14 }}>
        {mensagens.length === 0 ? (
          <div style={{ margin: "auto", textAlign: "center", maxWidth: 460 }}>
            <div className="grid place-items-center" style={{ width: 52, height: 52, borderRadius: 14, margin: "0 auto 14px", background: "var(--color-primary-soft)", color: "var(--color-primary)" }}>
              <Sparkles size={26} />
            </div>
            <div style={{ fontWeight: 700, fontSize: 16 }}>Como posso ajudar?</div>
            <p style={{ color: "var(--color-text-muted)", fontSize: 14, margin: "6px 0 18px" }}>
              Pergunte sobre prazos, procedimentos, ou peça para explicar um termo jurídico.
            </p>
            <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
              {SUGESTOES.map((s) => (
                <button
                  key={s}
                  onClick={() => perguntar(s)}
                  className="card"
                  style={{ padding: "0.7rem 0.9rem", textAlign: "left", fontSize: 13.5, cursor: "pointer", background: "var(--color-surface-2)" }}
                >
                  {s}
                </button>
              ))}
            </div>
          </div>
        ) : (
          mensagens.map((m, i) => (
            <div key={i} style={{ display: "flex", justifyContent: m.papel === "user" ? "flex-end" : "flex-start" }}>
              <div
                style={{
                  maxWidth: "78%",
                  padding: "0.7rem 1rem",
                  borderRadius: 14,
                  fontSize: 14.5,
                  lineHeight: 1.55,
                  whiteSpace: "pre-wrap",
                  background: m.papel === "user" ? "var(--color-primary)" : "var(--color-surface-2)",
                  color: m.papel === "user" ? "#fff" : "var(--color-text)",
                  borderTopRightRadius: m.papel === "user" ? 4 : 14,
                  borderTopLeftRadius: m.papel === "user" ? 14 : 4,
                }}
              >
                {m.texto}
              </div>
            </div>
          ))
        )}
        {enviando && (
          <div style={{ display: "flex", justifyContent: "flex-start" }}>
            <div style={{ padding: "0.7rem 1rem", borderRadius: 14, background: "var(--color-surface-2)", color: "var(--color-text-muted)", fontSize: 14 }}>
              Lexo IA está pensando...
            </div>
          </div>
        )}
        <div ref={fimRef} />
      </div>

      {/* Campo de entrada */}
      <form onSubmit={enviar} style={{ display: "flex", gap: 10, marginTop: 14 }}>
        <input
          className="input"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Escreva sua pergunta..."
          disabled={enviando}
        />
        <button type="submit" className="btn btn-primary" disabled={enviando || !input.trim()} style={{ padding: "0 1.1rem" }}>
          <Send size={16} />
        </button>
      </form>
      <p style={{ textAlign: "center", fontSize: 11, color: "var(--color-text-muted)", marginTop: 8 }}>
        A Lexo IA pode cometer erros. Confira informações importantes.
      </p>
    </div>
  );
}
