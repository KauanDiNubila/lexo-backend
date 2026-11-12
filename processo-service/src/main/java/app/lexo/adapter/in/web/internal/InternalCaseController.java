package app.lexo.adapter.in.web.internal;

import app.lexo.application.port.in.CaseExistsQuery;
import app.lexo.common.reference.ExistsResponse;
import app.lexo.domain.CaseId;
import app.lexo.domain.OrganizationId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class InternalCaseController {

    private final CaseExistsQuery caseExists;

    InternalCaseController(CaseExistsQuery caseExists) {
        this.caseExists = caseExists;
    }

    @GetMapping("/internal/processos/{id}/existe")
    ExistsResponse exists(@PathVariable String id, @RequestParam String orgId) {
        if (id.isBlank() || orgId.isBlank()) {
            return new ExistsResponse(false);
        }
        return new ExistsResponse(caseExists.exists(new OrganizationId(orgId), new CaseId(id)));
    }
}
