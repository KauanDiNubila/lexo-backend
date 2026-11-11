package app.lexo.domain;

public record DadosDoCliente(String nome, CpfCnpj documento, String email, String telefone, String observacoes) {

    public DadosDoCliente {
        email = vazioParaNulo(email);
        telefone = vazioParaNulo(telefone);
        observacoes = vazioParaNulo(observacoes);
    }

    private static String vazioParaNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto;
    }
}
