package app.lexo.application.port.in;

import java.util.List;

public record PageParams(int page, int size, List<SortOrder> orders) {
}
