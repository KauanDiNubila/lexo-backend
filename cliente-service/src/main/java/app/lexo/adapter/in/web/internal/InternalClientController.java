package app.lexo.adapter.in.web.internal;

import app.lexo.application.port.in.ClientExistsQuery;
import app.lexo.common.referencia.ExisteResponse;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class InternalClientController {

    private final ClientExistsQuery clientExists;

    InternalClientController(ClientExistsQuery clientExists) {
        this.clientExists = clientExists;
    }

    @GetMapping("/internal/clientes/{id}/existe")
    ExisteResponse exists(@PathVariable String id, @RequestParam String orgId) {
        if (orgId.isBlank()) {
            return new ExisteResponse(false);
        }
        return new ExisteResponse(clientExists.exists(new OrganizationId(orgId), new ClientId(id)));
    }
}
