import ReactMarkdown from "react-markdown";

export function Markdown({ children }: { children: string }) {
  return (
    <div className="md">
      <ReactMarkdown>{children}</ReactMarkdown>
    </div>
  );
}
