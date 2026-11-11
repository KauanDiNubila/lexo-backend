package app.lexo.application.port.in;

import java.util.List;

public record Paginacao(int pagina, int tamanho, List<Ordem> ordens) {
}
