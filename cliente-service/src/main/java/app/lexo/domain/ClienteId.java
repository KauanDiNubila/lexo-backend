package app.lexo.domain;

public record ClienteId(String valor) {

    public ClienteId {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("ClienteId vazio");
        }
    }
}
