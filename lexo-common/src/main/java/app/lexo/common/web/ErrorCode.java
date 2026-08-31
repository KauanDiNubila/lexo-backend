package app.lexo.common.web;

import org.springframework.http.HttpStatus;

public final class ErrorCode {

    public static final String VALIDACAO = "VALIDACAO";
    public static final String REQUISICAO_INVALIDA = "REQUISICAO_INVALIDA";
    public static final String NAO_AUTORIZADO = "NAO_AUTORIZADO";
    public static final String ACESSO_NEGADO = "ACESSO_NEGADO";
    public static final String NAO_ENCONTRADO = "NAO_ENCONTRADO";
    public static final String CONFLITO = "CONFLITO";
    public static final String REQUISICOES_EM_EXCESSO = "REQUISICOES_EM_EXCESSO";
    public static final String SERVICO_INDISPONIVEL = "SERVICO_INDISPONIVEL";
    public static final String ERRO_INTERNO = "ERRO_INTERNO";

    public static final String CREDENCIAIS_INVALIDAS = "CREDENCIAIS_INVALIDAS";
    public static final String TOTP_REQUIRED = "TOTP_REQUIRED";
    public static final String TOTP_INVALID = "TOTP_INVALID";
    public static final String EMAIL_EM_USO = "EMAIL_EM_USO";
    public static final String SENHAS_NAO_COINCIDEM = "SENHAS_NAO_COINCIDEM";

    private ErrorCode() {
    }

    public static String paraStatus(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> REQUISICAO_INVALIDA;
            case UNAUTHORIZED -> NAO_AUTORIZADO;
            case FORBIDDEN -> ACESSO_NEGADO;
            case NOT_FOUND -> NAO_ENCONTRADO;
            case CONFLICT -> CONFLITO;
            case TOO_MANY_REQUESTS -> REQUISICOES_EM_EXCESSO;
            case SERVICE_UNAVAILABLE -> SERVICO_INDISPONIVEL;
            default -> ERRO_INTERNO;
        };
    }
}
