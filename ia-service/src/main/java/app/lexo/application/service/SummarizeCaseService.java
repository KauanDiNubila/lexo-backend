package app.lexo.application.service;

import app.lexo.application.port.in.SummarizeCaseCommand;
import app.lexo.application.port.in.SummarizeCaseUseCase;
import app.lexo.application.port.in.SummaryResponse;
import app.lexo.application.port.out.TextGenerationPort;
import app.lexo.domain.CaseContext;
import app.lexo.domain.Deadline;
import app.lexo.domain.Source;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

import static app.lexo.application.service.CaseText.orDefault;

@Service
class SummarizeCaseService implements SummarizeCaseUseCase {

    private static final String INSTRUCTION =
            "Você é um assistente jurídico. Resuma o processo abaixo em 3 a 4 frases claras e "
                    + "objetivas para o advogado, destacando a situação atual e os próximos prazos. "
                    + "Escreva em português, em tom profissional. NÃO invente informações além das fornecidas.\n\n";

    private final UsageQuota quota;
    private final TextGenerationPort textPort;

    SummarizeCaseService(UsageQuota quota, TextGenerationPort textPort) {
        this.quota = quota;
        this.textPort = textPort;
    }

    @Override
    public SummaryResponse summarize(SummarizeCaseCommand command) {
        quota.spend(command.organizationId());
        return textPort.generate(prompt(command))
                .map(text -> new SummaryResponse(text.trim(), Source.MODEL.code()))
                .orElseGet(() -> new SummaryResponse(automaticSummary(command), Source.FALLBACK.code()));
    }

    private String prompt(SummarizeCaseCommand command) {
        StringBuilder prompt = new StringBuilder(INSTRUCTION)
                .append(CaseText.lines(command.caseContext())).append("\n");
        if (command.deadlines().isEmpty()) {
            return prompt.append("Prazos: nenhum cadastrado.\n").toString();
        }
        prompt.append("Prazos:\n");
        for (Deadline deadline : command.deadlines()) {
            prompt.append("  - ").append(orDefault(deadline.title(), "prazo"))
                    .append(" (").append(orDefault(deadline.date(), "sem data")).append(", ")
                    .append(orDefault(deadline.status(), "?")).append(")\n");
        }
        return prompt.toString();
    }

    private String automaticSummary(SummarizeCaseCommand command) {
        CaseContext context = command.caseContext();
        StringBuilder summary = new StringBuilder("O processo ").append(orDefault(context.number(), "sem número"));
        if (context.area() != null && !context.area().isBlank()) {
            summary.append(" (").append(context.area()).append(")");
        }
        summary.append(" do cliente ").append(orDefault(context.client(), "não informado"))
                .append(" encontra-se ")
                .append(orDefault(context.status(), "em andamento").toLowerCase(Locale.ROOT)).append(". ");
        return summary.append(deadlinesSentence(command.deadlines()))
                .append(" (Resumo automático — configure a Lexo IA para análises mais ricas.)").toString();
    }

    private String deadlinesSentence(List<Deadline> deadlines) {
        if (deadlines.isEmpty()) {
            return "Não há prazos cadastrados no momento.";
        }
        List<Deadline> open = deadlines.stream().filter(Deadline::isOpen).toList();
        if (open.isEmpty()) {
            return "Não há prazos em aberto no momento.";
        }
        Deadline next = open.get(0);
        StringBuilder sentence = new StringBuilder("Há ").append(open.size())
                .append(" prazo(s) em aberto; o próximo é \"").append(orDefault(next.title(), "prazo")).append("\"");
        if (next.date() != null && !next.date().isBlank()) {
            sentence.append(" em ").append(next.date());
        }
        return sentence.append(".").toString();
    }
}
