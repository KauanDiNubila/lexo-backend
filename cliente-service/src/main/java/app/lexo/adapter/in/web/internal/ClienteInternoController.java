package app.lexo.adapter.in.web.internal;

import app.lexo.application.port.in.ExisteClienteQuery;
import app.lexo.common.referencia.ExisteResponse;
import app.lexo.domain.ClienteId;
import app.lexo.domain.EscritorioId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ClienteInternoController {

    private final ExisteClienteQuery existeCliente;

    ClienteInternoController(ExisteClienteQuery existeCliente) {
        this.existeCliente = existeCliente;
    }

    @GetMapping("/internal/clientes/{id}/existe")
    ExisteResponse existe(@PathVariable String id, @RequestParam String orgId) {
        if (orgId.isBlank()) {
            return new ExisteResponse(false);
        }
        return new ExisteResponse(existeCliente.existe(new EscritorioId(orgId), new ClienteId(id)));
    }
}
