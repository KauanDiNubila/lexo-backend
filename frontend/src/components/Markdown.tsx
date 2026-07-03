import ReactMarkdown from "react-markdown";

/** Renderiza texto em markdown (respostas da IA) com o estilo do tema (classe .md). */
export function Markdown({ children }: { children: string }) {
  return (
    <div className="md">
      <ReactMarkdown>{children}</ReactMarkdown>
    </div>
  );
}
