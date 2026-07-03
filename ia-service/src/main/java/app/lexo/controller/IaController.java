package app.lexo.controller;

import app.lexo.dto.IaDtos.ResumoProcessoRequest;
import app.lexo.dto.IaDtos.ResumoResponse;
import app.lexo.service.IaService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recursos de IA. Fica atras do gateway (so requisicoes autenticadas chegam aqui),
 * por isso o servico nao precisa de seguranca propria.
 */
@RestController
@RequestMapping("/api/ia")
public class IaController {

    private final IaService service;

    public IaController(IaService service) {
        this.service = service;
    }

    @PostMapping("/resumir-processo")
    public ResumoResponse resumirProcesso(@RequestBody ResumoProcessoRequest req) {
        return service.resumirProcesso(req);
    }
}
