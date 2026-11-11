package app.lexo.adapter.in.web;

import app.lexo.application.port.in.Autor;
import app.lexo.application.port.in.Ordem;
import app.lexo.application.port.in.Paginacao;
import app.lexo.common.security.AuthUser;
import app.lexo.domain.EscritorioId;
import org.springframework.data.domain.Pageable;

final class Identidade {

    private Identidade() {
    }

    static EscritorioId escritorio(AuthUser usuario) {
        return new EscritorioId(usuario.organizationId());
    }

    static Autor autor(AuthUser usuario) {
        return new Autor(usuario.id(), nomeDe(usuario));
    }

    static Paginacao paginacao(Pageable pageable) {
        return new Paginacao(pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().stream().map(o -> new Ordem(o.getProperty(), o.isAscending())).toList());
    }

    private static String nomeDe(AuthUser usuario) {
        if (usuario.name() != null) {
            return usuario.name();
        }
        return usuario.email() != null ? usuario.email() : "Sistema";
    }
}
