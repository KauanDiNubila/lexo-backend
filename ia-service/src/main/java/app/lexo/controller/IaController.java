package app.lexo.controller;

import app.lexo.dto.ChatDtos.ChatRequest;
import app.lexo.dto.ChatDtos.ChatResponse;
import app.lexo.dto.IaDtos.ResumoProcessoRequest;
import app.lexo.dto.IaDtos.ResumoResponse;
import app.lexo.dto.PeticaoDtos.PeticaoRequest;
import app.lexo.dto.PeticaoDtos.PeticaoResponse;
import app.lexo.service.IaService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest req) {
        return service.chat(req);
    }

    @PostMapping("/rascunhar-peticao")
    public PeticaoResponse rascunharPeticao(@RequestBody PeticaoRequest req) {
        return service.rascunharPeticao(req);
    }
}
