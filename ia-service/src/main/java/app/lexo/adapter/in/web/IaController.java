package app.lexo.adapter.in.web;

import app.lexo.application.port.in.ChatResponse;
import app.lexo.application.port.in.ChatUseCase;
import app.lexo.application.port.in.DraftPetitionUseCase;
import app.lexo.application.port.in.PetitionResponse;
import app.lexo.application.port.in.SummarizeCaseUseCase;
import app.lexo.application.port.in.SummaryResponse;
import app.lexo.common.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ia")
class IaController {

    private final SummarizeCaseUseCase summarizeCase;
    private final ChatUseCase chat;
    private final DraftPetitionUseCase draftPetition;

    IaController(SummarizeCaseUseCase summarizeCase, ChatUseCase chat, DraftPetitionUseCase draftPetition) {
        this.summarizeCase = summarizeCase;
        this.chat = chat;
        this.draftPetition = draftPetition;
    }

    @PostMapping("/resumir-processo")
    SummaryResponse summarize(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody SummarizeRequest req) {
        return summarizeCase.summarize(req.toCommand(Identity.organization(me)));
    }

    @PostMapping("/chat")
    ChatResponse chat(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ChatRequest req) {
        return chat.chat(req.toCommand(Identity.organization(me)));
    }

    @PostMapping("/rascunhar-peticao")
    PetitionResponse draft(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody DraftRequest req) {
        return draftPetition.draft(req.toCommand(Identity.organization(me)));
    }
}
