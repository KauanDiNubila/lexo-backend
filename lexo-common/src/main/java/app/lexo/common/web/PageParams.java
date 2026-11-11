package app.lexo.common.web;

import java.util.List;

public record PageParams(int page, int size, List<SortOrder> orders) {
}
