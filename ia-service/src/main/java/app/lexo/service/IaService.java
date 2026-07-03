package app.lexo.service;

import app.lexo.dto.IaDtos.PrazoCtx;
import app.lexo.dto.IaDtos.ResumoProcessoRequest;
import app.lexo.dto.IaDtos.ResumoResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/** Gera o resumo do processo: Gemini quando ha chave, senao um resumo heuristico (mock). */
@Service
public class IaService {

    private final GeminiClient gemini;

    public IaService(GeminiClient gemini) {
        this.gemini = gemini;
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
