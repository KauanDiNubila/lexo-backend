package app.lexo.domain;

import app.lexo.common.erro.DadoInvalidoException;

public record DadosDoCliente(String nome, CpfCnpj documento, String email, String telefone, String observacoes) {

    private static final int TAMANHO_MINIMO_DO_NOME = 2;

    public DadosDoCliente {
        if (nome == null || nome.isBlank() || nome.length() < TAMANHO_MINIMO_DO_NOME) {
            throw new DadoInvalidoException("Nome muito curto");
        }
        email = vazioParaNulo(email);
        telefone = vazioParaNulo(telefone);
        observacoes = vazioParaNulo(observacoes);
    }

    private static String vazioParaNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto;
    }
}
