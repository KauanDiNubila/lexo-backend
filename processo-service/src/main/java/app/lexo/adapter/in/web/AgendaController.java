package app.lexo.adapter.in.web;

import app.lexo.application.port.in.AgendaSummary;
import app.lexo.application.port.in.ChangeDeadlineStatusCommand;
import app.lexo.application.port.in.ChangeDeadlineStatusUseCase;
import app.lexo.application.port.in.DeadlineResponse;
import app.lexo.application.port.in.DeleteDeadlineCommand;
import app.lexo.application.port.in.DeleteDeadlineUseCase;
import app.lexo.application.port.in.GetAgendaSummaryQuery;
import app.lexo.application.port.in.ListDeadlinesQuery;
import app.lexo.application.port.in.ScheduleDeadlineCommand;
import app.lexo.application.port.in.ScheduleDeadlineUseCase;
import app.lexo.application.port.in.UpdateDeadlineCommand;
import app.lexo.application.port.in.UpdateDeadlineUseCase;
import app.lexo.common.security.AuthUser;
import app.lexo.common.web.PageParamsMapper;
import app.lexo.common.web.PageResult;
import app.lexo.domain.CaseId;
import app.lexo.domain.DeadlineId;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agenda")
class AgendaController {

    private final ListDeadlinesQuery listDeadlines;
    private final GetAgendaSummaryQuery agendaSummary;
    private final ScheduleDeadlineUseCase scheduleDeadline;
    private final UpdateDeadlineUseCase updateDeadline;
    private final ChangeDeadlineStatusUseCase changeDeadlineStatus;
    private final DeleteDeadlineUseCase deleteDeadline;

    AgendaController(ListDeadlinesQuery listDeadlines, GetAgendaSummaryQuery agendaSummary,
                     ScheduleDeadlineUseCase scheduleDeadline, UpdateDeadlineUseCase updateDeadline,
                     ChangeDeadlineStatusUseCase changeDeadlineStatus, DeleteDeadlineUseCase deleteDeadline) {
        this.listDeadlines = listDeadlines;
        this.agendaSummary = agendaSummary;
        this.scheduleDeadline = scheduleDeadline;
        this.updateDeadline = updateDeadline;
        this.changeDeadlineStatus = changeDeadlineStatus;
        this.deleteDeadline = deleteDeadline;
    }

    @GetMapping
    PageResult<DeadlineResponse> list(
            @AuthenticationPrincipal AuthUser me,
            @RequestParam(required = false) String caseId,
            @PageableDefault(size = 20, sort = "date", direction = Sort.Direction.ASC) Pageable pageable) {
        return listDeadlines.list(Identity.organization(me), CaseId.optional(caseId),
                PageParamsMapper.from(pageable));
    }

    @GetMapping("/resumo")
    AgendaSummary summary(@AuthenticationPrincipal AuthUser me) {
        return agendaSummary.summarize(Identity.organization(me));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    DeadlineResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody DeadlineRequest req) {
        return scheduleDeadline.schedule(new ScheduleDeadlineCommand(Identity.organization(me), req.toData()));
    }

    @PutMapping("/{id}")
    DeadlineResponse update(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                            @Valid @RequestBody DeadlineRequest req) {
        return updateDeadline.update(new UpdateDeadlineCommand(Identity.organization(me), new DeadlineId(id),
                req.toData()));
    }

    @PatchMapping("/{id}/status")
    DeadlineResponse changeStatus(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                  @RequestBody StatusRequest req) {
        return changeDeadlineStatus.changeStatus(new ChangeDeadlineStatusCommand(Identity.organization(me),
                new DeadlineId(id), req.completed()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        deleteDeadline.delete(new DeleteDeadlineCommand(Identity.organization(me), new DeadlineId(id)));
    }
}
