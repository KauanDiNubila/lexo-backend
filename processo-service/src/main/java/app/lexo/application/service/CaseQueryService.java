package app.lexo.application.service;

import app.lexo.application.port.in.CaseExistsQuery;
import app.lexo.application.port.in.CaseOption;
import app.lexo.application.port.in.CaseResponse;
import app.lexo.application.port.in.CaseSummary;
import app.lexo.application.port.in.GetCaseQuery;
import app.lexo.application.port.in.GetCaseSummaryQuery;
import app.lexo.application.port.in.ListCaseOptionsQuery;
import app.lexo.application.port.in.ListCasesQuery;
import app.lexo.application.port.out.CaseExistsPort;
import app.lexo.application.port.out.CountCasesByStatusPort;
import app.lexo.application.port.out.ListCaseOptionsPort;
import app.lexo.application.port.out.ListCasesPort;
import app.lexo.application.port.out.LoadCasePort;
import app.lexo.common.web.PageParams;
import app.lexo.common.web.PageResult;
import app.lexo.domain.Case;
import app.lexo.domain.CaseFilter;
import app.lexo.domain.CaseId;
import app.lexo.domain.CaseNotFoundException;
import app.lexo.domain.CaseStatus;
import app.lexo.domain.OrganizationId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
class CaseQueryService implements ListCasesQuery, GetCaseQuery, ListCaseOptionsQuery, GetCaseSummaryQuery,
        CaseExistsQuery {

    private final ListCasesPort listCasesPort;
    private final LoadCasePort loadCasePort;
    private final ListCaseOptionsPort listOptionsPort;
    private final CountCasesByStatusPort countByStatusPort;
    private final CaseExistsPort caseExistsPort;

    CaseQueryService(ListCasesPort listCasesPort, LoadCasePort loadCasePort, ListCaseOptionsPort listOptionsPort,
                     CountCasesByStatusPort countByStatusPort, CaseExistsPort caseExistsPort) {
        this.listCasesPort = listCasesPort;
        this.loadCasePort = loadCasePort;
        this.listOptionsPort = listOptionsPort;
        this.countByStatusPort = countByStatusPort;
        this.caseExistsPort = caseExistsPort;
    }

    @Override
    public PageResult<CaseResponse> list(OrganizationId organizationId, CaseFilter filter, PageParams pageParams) {
        PageResult<Case> page = listCasesPort.list(organizationId, filter, pageParams);
        List<CaseResponse> content = page.content().stream().map(CaseResponse::of).toList();
        return new PageResult<>(content, page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    @Override
    public CaseResponse get(OrganizationId organizationId, CaseId caseId) {
        return loadCasePort.byId(organizationId, caseId).map(CaseResponse::of)
                .orElseThrow(CaseNotFoundException::new);
    }

    @Override
    public List<CaseOption> options(OrganizationId organizationId) {
        return listOptionsPort.options(organizationId);
    }

    @Override
    public CaseSummary summarize(OrganizationId organizationId) {
        Map<CaseStatus, Long> counts = countByStatusPort.countByStatus(organizationId);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        counts.forEach((status, count) -> byStatus.put(status.code(), count));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return new CaseSummary(total, counts.getOrDefault(CaseStatus.ACTIVE, 0L), byStatus);
    }

    @Override
    public boolean exists(OrganizationId organizationId, CaseId caseId) {
        return caseExistsPort.exists(organizationId, caseId);
    }
}
