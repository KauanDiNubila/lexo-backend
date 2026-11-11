package app.lexo.common.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public final class PageParamsMapper {

    private PageParamsMapper() {
    }

    public static PageParams from(Pageable pageable) {
        return new PageParams(pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().stream().map(o -> new SortOrder(o.getProperty(), o.isAscending())).toList());
    }

    public static PageRequest toPageRequest(PageParams params) {
        List<Sort.Order> orders = params.orders().stream()
                .map(o -> o.ascending() ? Sort.Order.asc(o.property()) : Sort.Order.desc(o.property()))
                .toList();
        return PageRequest.of(params.page(), params.size(), Sort.by(orders));
    }
}
