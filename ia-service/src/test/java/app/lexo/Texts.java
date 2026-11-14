package app.lexo;

public final class Texts {

    public static final String PERSONA =
            "Você é a Lexo IA, assistente jurídico de um escritório de advocacia brasileiro. "
                    + "Responda em português, de forma clara, objetiva e profissional. Ajude com dúvidas "
                    + "jurídicas gerais, resumos e redação. NÃO invente números de lei, jurisprudência ou "
                    + "fatos do processo que não tenham sido informados; quando não tiver certeza, diga isso "
                    + "e recomende a verificação por um advogado responsável.";

    public static final String SUMMARY_INTRO =
            "Você é um assistente jurídico. Resuma o processo abaixo em 3 a 4 frases claras e "
                    + "objetivas para o advogado, destacando a situação atual e os próximos prazos. "
                    + "Escreva em português, em tom profissional. NÃO invente informações além das fornecidas.\n\n";

    public static final String SUMMARY_MOCK_TAIL = " (Resumo automático — configure a Lexo IA para análises mais ricas.)";

    public static final String CHAT_UNAVAILABLE =
            "A Lexo IA está indisponível no momento. Verifique a configuração (GEMINI_API_KEY) "
                    + "ou tente novamente em instantes.";

    public static final String PETITION_INTRO =
            "Você é um advogado brasileiro experiente. Redija a MINUTA de uma peça ou documento "
                    + "jurídico conforme o pedido abaixo, em português jurídico formal e bem estruturado "
                    + "(endereçamento ao juízo, qualificação das partes, DOS FATOS, DO DIREITO, DOS PEDIDOS, "
                    + "e fecho com local, data e assinatura). Onde faltar informação específica (nomes completos, "
                    + "CPF/CNPJ, valores, datas, número do processo), use PLACEHOLDERS entre colchetes, ex.: "
                    + "[NOME DO CLIENTE], [VALOR], [DATA]. NÃO invente números de lei, súmulas ou jurisprudência "
                    + "específicos — se mencionar a tese jurídica, mantenha-a genérica. Deixe claro que é uma minuta "
                    + "a ser revisada pelo advogado responsável.\n\n";

    public static String petitionTemplate(String request) {
        return "MINUTA (modelo automático — configure a Lexo IA com GEMINI_API_KEY para geração completa)\n\n"
                + "Pedido: " + request + "\n\n"
                + "EXCELENTÍSSIMO(A) SENHOR(A) DOUTOR(A) JUIZ(A) DE DIREITO DA [VARA] DA COMARCA DE [COMARCA]\n\n"
                + "[NOME DO CLIENTE], [QUALIFICAÇÃO], por seu advogado que esta subscreve, vem, respeitosamente, "
                + "à presença de Vossa Excelência, expor e requerer o que segue.\n\n"
                + "DOS FATOS\n[Descrever os fatos.]\n\n"
                + "DO DIREITO\n[Fundamentar juridicamente.]\n\n"
                + "DOS PEDIDOS\n[Elencar os pedidos.]\n\n"
                + "Nesses termos, pede deferimento.\n[LOCAL], [DATA].\n[ADVOGADO] — OAB/[UF] [Nº]";
    }

    public static String caseLines(String number, String area, String status, String client) {
        return "Número: " + number + "\n"
                + "Área: " + area + "\n"
                + "Status: " + status + "\n"
                + "Cliente: " + client;
    }

    private Texts() {
    }
}
