package app.lexo.application.service;

import app.lexo.application.port.in.AgendaSummary;
import app.lexo.application.port.in.DeadlineResponse;
import app.lexo.application.port.in.GetAgendaSummaryQuery;
import app.lexo.application.port.in.ListDeadlinesQuery;
import app.lexo.application.port.out.ListDeadlinesPort;
import app.lexo.application.port.out.PendingDeadlinesPort;
import app.lexo.common.web.PageParams;
import app.lexo.common.web.PageResult;
import app.lexo.domain.CaseId;
import app.lexo.domain.Deadline;
import app.lexo.domain.OrganizationId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
class AgendaQueryService implements ListDeadlinesQuery, GetAgendaSummaryQuery {

    private static final Duration URGENT_WINDOW = Duration.ofDays(7);
    private static final int UPCOMING_LIMIT = 5;

    private final ListDeadlinesPort listDeadlinesPort;
    private final PendingDeadlinesPort pendingDeadlinesPort;
    private final Clock clock;

    AgendaQueryService(ListDeadlinesPort listDeadlinesPort, PendingDeadlinesPort pendingDeadlinesPort,
                       Clock clock) {
        this.listDeadlinesPort = listDeadlinesPort;
        this.pendingDeadlinesPort = pendingDeadlinesPort;
        this.clock = clock;
    }

    @Override
    public PageResult<DeadlineResponse> list(OrganizationId organizationId, CaseId caseId, PageParams pageParams) {
        Instant now = clock.instant();
        PageResult<Deadline> page = listDeadlinesPort.list(organizationId, caseId, pageParams);
        List<DeadlineResponse> content = page.content().stream()
                .map(deadline -> DeadlineResponse.of(deadline, now)).toList();
        return new PageResult<>(content, page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    @Override
    public AgendaSummary summarize(OrganizationId organizationId) {
        Instant now = clock.instant();
        return new AgendaSummary(
                pendingDeadlinesPort.countBetween(organizationId, now, now.plus(URGENT_WINDOW)),
                pendingDeadlinesPort.countBefore(organizationId, now),
                pendingDeadlinesPort.next(organizationId, UPCOMING_LIMIT).stream()
                        .map(deadline -> DeadlineResponse.of(deadline, now)).toList());
    }
}
