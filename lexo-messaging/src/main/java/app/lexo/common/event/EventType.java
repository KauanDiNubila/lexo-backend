package app.lexo.common.event;

public enum EventType {

    CLIENT_CREATED("CLIENTE_CRIADO"),
    CLIENT_DELETED("CLIENTE_EXCLUIDO"),
    CASE_CREATED("PROCESSO_CRIADO"),
    CASE_UPDATED("PROCESSO_ATUALIZADO"),
    CASE_DELETED("PROCESSO_EXCLUIDO");

    private final String code;

    EventType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
