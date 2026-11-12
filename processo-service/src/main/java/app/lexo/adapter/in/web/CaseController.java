package app.lexo.adapter.in.web;

import app.lexo.application.port.in.CaseOption;
import app.lexo.application.port.in.CaseResponse;
import app.lexo.application.port.in.CaseSummary;
import app.lexo.application.port.in.CreateCaseCommand;
import app.lexo.application.port.in.CreateCaseUseCase;
import app.lexo.application.port.in.DeleteCaseCommand;
import app.lexo.application.port.in.DeleteCaseUseCase;
import app.lexo.application.port.in.GetCaseQuery;
import app.lexo.application.port.in.GetCaseSummaryQuery;
import app.lexo.application.port.in.ListCaseOptionsQuery;
import app.lexo.application.port.in.ListCasesQuery;
import app.lexo.application.port.in.UpdateCaseCommand;
import app.lexo.application.port.in.UpdateCaseUseCase;
import app.lexo.common.security.AuthUser;
import app.lexo.common.web.PageParamsMapper;
import app.lexo.common.web.PageResult;
import app.lexo.domain.CaseFilter;
import app.lexo.domain.CaseId;
import app.lexo.domain.CaseStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/processos")
class CaseController {

    private final ListCasesQuery listCases;
    private final GetCaseQuery getCase;
    private final ListCaseOptionsQuery listOptions;
    private final GetCaseSummaryQuery caseSummary;
    private final CreateCaseUseCase createCase;
    private final UpdateCaseUseCase updateCase;
    private final DeleteCaseUseCase deleteCase;

    CaseController(ListCasesQuery listCases, GetCaseQuery getCase, ListCaseOptionsQuery listOptions,
                   GetCaseSummaryQuery caseSummary, CreateCaseUseCase createCase, UpdateCaseUseCase updateCase,
                   DeleteCaseUseCase deleteCase) {
        this.listCases = listCases;
        this.getCase = getCase;
        this.listOptions = listOptions;
        this.caseSummary = caseSummary;
        this.createCase = createCase;
        this.updateCase = updateCase;
        this.deleteCase = deleteCase;
    }

    @GetMapping
    PageResult<CaseResponse> list(
            @AuthenticationPrincipal AuthUser me,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) CaseStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return listCases.list(Identity.organization(me), new CaseFilter(q, status), PageParamsMapper.from(pageable));
    }

    @GetMapping("/opcoes")
    List<CaseOption> options(@AuthenticationPrincipal AuthUser me) {
        return listOptions.options(Identity.organization(me));
    }

    @GetMapping("/resumo")
    CaseSummary summary(@AuthenticationPrincipal AuthUser me) {
        return caseSummary.summarize(Identity.organization(me));
    }

    @GetMapping("/{id}")
    CaseResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return getCase.get(Identity.organization(me), new CaseId(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CaseResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CaseRequest req) {
        return createCase.create(new CreateCaseCommand(Identity.organization(me), Identity.actor(me), req.toData()));
    }

    @PutMapping("/{id}")
    CaseResponse update(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                        @Valid @RequestBody CaseRequest req) {
        return updateCase.update(new UpdateCaseCommand(Identity.organization(me), new CaseId(id),
                Identity.actor(me), req.toData()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADVOGADO')")
    void delete(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        deleteCase.delete(new DeleteCaseCommand(Identity.organization(me), new CaseId(id), Identity.actor(me)));
    }
}
