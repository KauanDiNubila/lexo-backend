package app.lexo.application.service;

import app.lexo.application.port.in.DraftPetitionCommand;
import app.lexo.application.port.in.DraftPetitionUseCase;
import app.lexo.application.port.in.PetitionResponse;
import app.lexo.application.port.out.TextGenerationPort;
import app.lexo.domain.Source;
import org.springframework.stereotype.Service;

import static app.lexo.application.service.CaseText.orDefault;

@Service
class DraftPetitionService implements DraftPetitionUseCase {

    private static final String INSTRUCTION =
            "Você é um advogado brasileiro experiente. Redija a MINUTA de uma peça ou documento "
                    + "jurídico conforme o pedido abaixo, em português jurídico formal e bem estruturado "
                    + "(endereçamento ao juízo, qualificação das partes, DOS FATOS, DO DIREITO, DOS PEDIDOS, "
                    + "e fecho com local, data e assinatura). Onde faltar informação específica (nomes completos, "
                    + "CPF/CNPJ, valores, datas, número do processo), use PLACEHOLDERS entre colchetes, ex.: "
                    + "[NOME DO CLIENTE], [VALOR], [DATA]. NÃO invente números de lei, súmulas ou jurisprudência "
                    + "específicos — se mencionar a tese jurídica, mantenha-a genérica. Deixe claro que é uma minuta "
                    + "a ser revisada pelo advogado responsável.\n\n";

    private final UsageQuota quota;
    private final TextGenerationPort textPort;

    DraftPetitionService(UsageQuota quota, TextGenerationPort textPort) {
        this.quota = quota;
        this.textPort = textPort;
    }

    @Override
    public PetitionResponse draft(DraftPetitionCommand command) {
        quota.spend(command.organizationId());
        return textPort.generate(prompt(command))
                .map(text -> new PetitionResponse(text.trim(), Source.MODEL.code()))
                .orElseGet(() -> new PetitionResponse(template(command), Source.FALLBACK.code()));
    }

    private String prompt(DraftPetitionCommand command) {
        StringBuilder prompt = new StringBuilder(INSTRUCTION)
                .append("Pedido: ").append(orDefault(command.instruction(), "peça jurídica")).append("\n");
        command.caseContext().ifPresent(context -> prompt
                .append("\nContexto do processo:\n").append(CaseText.lines(context)).append("\n"));
        return prompt.toString();
    }

    private String template(DraftPetitionCommand command) {
        return "MINUTA (modelo automático — configure a Lexo IA com GEMINI_API_KEY para geração completa)\n\n"
                + "Pedido: " + orDefault(command.instruction(), "peça jurídica") + "\n\n"
                + "EXCELENTÍSSIMO(A) SENHOR(A) DOUTOR(A) JUIZ(A) DE DIREITO DA [VARA] DA COMARCA DE [COMARCA]\n\n"
                + "[NOME DO CLIENTE], [QUALIFICAÇÃO], por seu advogado que esta subscreve, vem, respeitosamente, "
                + "à presença de Vossa Excelência, expor e requerer o que segue.\n\n"
                + "DOS FATOS\n[Descrever os fatos.]\n\n"
                + "DO DIREITO\n[Fundamentar juridicamente.]\n\n"
                + "DOS PEDIDOS\n[Elencar os pedidos.]\n\n"
                + "Nesses termos, pede deferimento.\n[LOCAL], [DATA].\n[ADVOGADO] — OAB/[UF] [Nº]";
    }
}
