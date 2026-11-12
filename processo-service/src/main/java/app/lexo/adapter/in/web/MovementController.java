package app.lexo.adapter.in.web;

import app.lexo.application.port.in.DeleteMovementCommand;
import app.lexo.application.port.in.DeleteMovementUseCase;
import app.lexo.application.port.in.ListMovementsQuery;
import app.lexo.application.port.in.MovementResponse;
import app.lexo.application.port.in.RecordMovementCommand;
import app.lexo.application.port.in.RecordMovementUseCase;
import app.lexo.common.security.AuthUser;
import app.lexo.domain.CaseId;
import app.lexo.domain.MovementId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/processos/{caseId}/andamentos")
class MovementController {

    private final ListMovementsQuery listMovements;
    private final RecordMovementUseCase recordMovement;
    private final DeleteMovementUseCase deleteMovement;

    MovementController(ListMovementsQuery listMovements, RecordMovementUseCase recordMovement,
                       DeleteMovementUseCase deleteMovement) {
        this.listMovements = listMovements;
        this.recordMovement = recordMovement;
        this.deleteMovement = deleteMovement;
    }

    @GetMapping
    List<MovementResponse> list(@AuthenticationPrincipal AuthUser me, @PathVariable String caseId) {
        return listMovements.list(Identity.organization(me), new CaseId(caseId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    MovementResponse create(@AuthenticationPrincipal AuthUser me, @PathVariable String caseId,
                            @Valid @RequestBody MovementRequest req) {
        return recordMovement.record(new RecordMovementCommand(Identity.organization(me), new CaseId(caseId),
                req.toData()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AuthUser me, @PathVariable String caseId, @PathVariable String id) {
        deleteMovement.delete(new DeleteMovementCommand(Identity.organization(me), new CaseId(caseId),
                new MovementId(id)));
    }
}
