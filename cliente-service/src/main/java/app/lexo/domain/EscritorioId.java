package app.lexo.domain;

public record EscritorioId(String valor) {

    public EscritorioId {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("EscritorioId vazio");
        }
    }
}
