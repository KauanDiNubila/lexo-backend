package app.lexo.application.port.in;

import app.lexo.common.erro.DadoInvalidoException;

final class NomeDoCliente {

    private static final int TAMANHO_MINIMO = 2;

    private NomeDoCliente() {
    }

    static void exigirValido(String nome) {
        if (nome == null || nome.isBlank() || nome.length() < TAMANHO_MINIMO) {
            throw new DadoInvalidoException("Nome muito curto");
        }
    }
}
