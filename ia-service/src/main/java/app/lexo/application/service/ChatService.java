package app.lexo.application.service;

import app.lexo.application.port.in.ChatCommand;
import app.lexo.application.port.in.ChatResponse;
import app.lexo.application.port.in.ChatUseCase;
import app.lexo.application.port.out.TextGenerationPort;
import app.lexo.domain.Source;
import org.springframework.stereotype.Service;

@Service
class ChatService implements ChatUseCase {

    private static final String PERSONA =
            "Você é a Lexo IA, assistente jurídico de um escritório de advocacia brasileiro. "
                    + "Responda em português, de forma clara, objetiva e profissional. Ajude com dúvidas "
                    + "jurídicas gerais, resumos e redação. NÃO invente números de lei, jurisprudência ou "
                    + "fatos do processo que não tenham sido informados; quando não tiver certeza, diga isso "
                    + "e recomende a verificação por um advogado responsável.";

    private static final String UNAVAILABLE =
            "A Lexo IA está indisponível no momento. Verifique a configuração (GEMINI_API_KEY) "
                    + "ou tente novamente em instantes.";

    private final UsageQuota quota;
    private final TextGenerationPort textPort;

    ChatService(UsageQuota quota, TextGenerationPort textPort) {
        this.quota = quota;
        this.textPort = textPort;
    }

    @Override
    public ChatResponse chat(ChatCommand command) {
        quota.spend(command.organizationId());
        return textPort.converse(systemInstruction(command), command.messages())
                .map(text -> new ChatResponse(text.trim(), Source.MODEL.code()))
                .orElseGet(() -> new ChatResponse(UNAVAILABLE, Source.FALLBACK.code()));
    }

    private String systemInstruction(ChatCommand command) {
        return command.caseContext()
                .map(context -> PERSONA + "\n\nA conversa é sobre o seguinte processo (use como contexto):\n"
                        + CaseText.lines(context))
                .orElse(PERSONA);
    }
}
