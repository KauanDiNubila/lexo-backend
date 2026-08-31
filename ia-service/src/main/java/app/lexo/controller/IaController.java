package app.lexo.controller;

import app.lexo.common.security.AuthUser;
import app.lexo.dto.ChatDtos.ChatRequest;
import app.lexo.dto.ChatDtos.ChatResponse;
import app.lexo.dto.IaDtos.ResumoProcessoRequest;
import app.lexo.dto.IaDtos.ResumoResponse;
import app.lexo.dto.PeticaoDtos.PeticaoRequest;
import app.lexo.dto.PeticaoDtos.PeticaoResponse;
import app.lexo.service.IaService;
import app.lexo.service.LimiteIaService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ia")
public class IaController {

    private final IaService service;
    private final LimiteIaService limite;

    public IaController(IaService service, LimiteIaService limite) {
        this.service = service;
        this.limite = limite;
    }

    @PostMapping("/resumir-processo")
    public ResumoResponse resumirProcesso(@AuthenticationPrincipal AuthUser me,
                                          @Valid @RequestBody ResumoProcessoRequest req) {
        limite.exigirDentroDoLimite(me.organizationId());
        return service.resumirProcesso(req);
    }

    @PostMapping("/chat")
    public ChatResponse chat(@AuthenticationPrincipal AuthUser me,
                             @Valid @RequestBody ChatRequest req) {
        limite.exigirDentroDoLimite(me.organizationId());
        return service.chat(req);
    }

    @PostMapping("/rascunhar-peticao")
    public PeticaoResponse rascunharPeticao(@AuthenticationPrincipal AuthUser me,
                                            @Valid @RequestBody PeticaoRequest req) {
        limite.exigirDentroDoLimite(me.organizationId());
        return service.rascunharPeticao(req);
    }
}
