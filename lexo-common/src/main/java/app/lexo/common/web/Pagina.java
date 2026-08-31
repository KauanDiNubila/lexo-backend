package app.lexo.common.web;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record Pagina<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> Pagina<T> de(Page<T> pagina) {
        return new Pagina<>(pagina.getContent(), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }

    public static <E, T> Pagina<T> de(Page<E> pagina, Function<E, T> mapeador) {
        return de(pagina.map(mapeador));
    }
}
