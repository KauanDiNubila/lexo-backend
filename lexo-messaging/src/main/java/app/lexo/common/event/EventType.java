package app.lexo.common.event;

public enum EventType {

    CLIENT_CREATED("CLIENTE_CRIADO"),
    CLIENT_DELETED("CLIENTE_EXCLUIDO"),
    CASE_CREATED("PROCESSO_CRIADO"),
    CASE_UPDATED("PROCESSO_ATUALIZADO"),
    CASE_DELETED("PROCESSO_EXCLUIDO"),
    USER_INVITED("CONVIDOU_USUARIO"),
    INVITE_REVOKED("REVOGOU_CONVITE"),
    ROLE_CHANGED("ALTEROU_PAPEL"),
    USER_REMOVED("REMOVEU_USUARIO"),
    TWO_FACTOR_ENABLED("ATIVOU_2FA"),
    TWO_FACTOR_DISABLED("DESATIVOU_2FA");

    private final String code;

    EventType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
