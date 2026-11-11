package app.lexo.domain;

import app.lexo.common.erro.RecursoNaoEncontradoException;

public class ClienteNaoEncontradoException extends RecursoNaoEncontradoException {

    public ClienteNaoEncontradoException() {
        super("Cliente não encontrado");
    }
}
