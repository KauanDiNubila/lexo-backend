package app.lexo.service;

import app.lexo.dto.ChatDtos.ChatRequest;
import app.lexo.dto.ChatDtos.ChatResponse;
import app.lexo.dto.ChatDtos.Mensagem;
import app.lexo.dto.ChatDtos.ProcessoContexto;
import app.lexo.dto.IaDtos.PrazoCtx;
import app.lexo.dto.IaDtos.ResumoProcessoRequest;
import app.lexo.dto.IaDtos.ResumoResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Recursos de IA: Gemini quando ha chave, senao fallback heuristico (mock). */
@Service
public class IaService {

    private static final String PERSONA =
            "Você é a Lexo IA, assistente jurídico de um escritório de advocacia brasileiro. "
            + "Responda em português, de forma clara, objetiva e profissional. Ajude com dúvidas "
            + "jurídicas gerais, resumos e redação. NÃO invente números de lei, jurisprudência ou "
            + "fatos do processo que não tenham sido informados; quando não tiver certeza, diga isso "
            + "e recomende a verificação por um advogado responsável.";

    private final GeminiClient gemini;

    public IaService(GeminiClient gemini) {
        this.gemini = gemini;
    }

    public ChatResponse chat(ChatRequest req) {
        List<Map<String, Object>> contents = new ArrayList<>();
        if (req.mensagens() != null) {
            for (Mensagem m : req.mensagens()) {
                if (m.texto() == null || m.texto().isBlank()) {
                    continue;
                }
                String role = "assistant".equals(m.papel()) ? "model" : "user";
                contents.add(Map.of("role", role, "parts", List.of(Map.of("text", m.texto()))));
            }
        }
        return gemini.conversar(systemDoChat(req.contexto()), contents)
                .map(t -> new ChatResponse(t.trim(), "gemini"))
                .orElseGet(() -> new ChatResponse(
                        "A Lexo IA está indisponível no momento. Verifique a configuração (GEMINI_API_KEY) "
                        + "ou tente novamente em instantes.", "mock"));
    }

    private String systemDoChat(ProcessoContexto ctx) {
        if (ctx == null) {
            return PERSONA;
        }
        return PERSONA + "\n\nA conversa é sobre o seguinte processo (use como contexto):\n"
                + "Número: " + nvl(ctx.numero(), "não informado") + "\n"
                + "Área: " + nvl(ctx.area(), "não informada") + "\n"
                + "Status: " + nvl(ctx.status(), "não informado") + "\n"
                + "Cliente: " + nvl(ctx.cliente(), "não informado");
    }

    public ResumoResponse resumirProcesso(ResumoProcessoRequest req) {
        return gemini.gerar(montarPrompt(req))
                .map(texto -> new ResumoResponse(texto.trim(), "gemini"))
                .orElseGet(() -> new ResumoResponse(mock(req), "mock"));
    }

    private String montarPrompt(ResumoProcessoRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("Você é um assistente jurídico. Resuma o processo abaixo em 3 a 4 frases claras e ")
          .append("objetivas para o advogado, destacando a situação atual e os próximos prazos. ")
          .append("Escreva em português, em tom profissional. NÃO invente informações além das fornecidas.\n\n");
        sb.append("Número: ").append(nvl(req.numero(), "não informado")).append("\n");
        sb.append("Área: ").append(nvl(req.area(), "não informada")).append("\n");
        sb.append("Status: ").append(nvl(req.status(), "não informado")).append("\n");
        sb.append("Cliente: ").append(nvl(req.cliente(), "não informado")).append("\n");
        List<PrazoCtx> prazos = req.prazos();
        if (prazos == null || prazos.isEmpty()) {
            sb.append("Prazos: nenhum cadastrado.\n");
        } else {
            sb.append("Prazos:\n");
            for (PrazoCtx p : prazos) {
                sb.append("  - ").append(nvl(p.titulo(), "prazo"))
                  .append(" (").append(nvl(p.data(), "sem data")).append(", ")
                  .append(nvl(p.status(), "?")).append(")\n");
            }
        }
        return sb.toString();
    }

    private String mock(ResumoProcessoRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("O processo ").append(nvl(req.numero(), "sem número"));
        if (req.area() != null && !req.area().isBlank()) {
            sb.append(" (").append(req.area()).append(")");
        }
        sb.append(" do cliente ").append(nvl(req.cliente(), "não informado"))
          .append(" encontra-se ").append(nvl(req.status(), "em andamento").toLowerCase()).append(". ");
        List<PrazoCtx> prazos = req.prazos();
        if (prazos == null || prazos.isEmpty()) {
            sb.append("Não há prazos cadastrados no momento.");
        } else {
            PrazoCtx prox = prazos.get(0);
            sb.append("Há ").append(prazos.size()).append(" prazo(s) em aberto; o próximo é \"")
              .append(nvl(prox.titulo(), "prazo")).append("\"");
            if (prox.data() != null && !prox.data().isBlank()) {
                sb.append(" em ").append(prox.data());
            }
            sb.append(".");
        }
        sb.append(" (Resumo automático — configure a Lexo IA para análises mais ricas.)");
        return sb.toString();
    }

    private static String nvl(String v, String alt) {
        return (v == null || v.isBlank()) ? alt : v;
    }
}
