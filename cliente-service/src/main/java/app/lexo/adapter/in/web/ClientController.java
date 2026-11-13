package app.lexo.adapter.in.web;

import app.lexo.application.port.in.UpdateClientCommand;
import app.lexo.application.port.in.UpdateClientUseCase;
import app.lexo.application.port.in.GetClientQuery;
import app.lexo.application.port.in.ClientOption;
import app.lexo.application.port.in.ClientResponse;
import app.lexo.application.port.in.CreateClientCommand;
import app.lexo.application.port.in.CreateClientUseCase;
import app.lexo.application.port.in.DeleteClientCommand;
import app.lexo.application.port.in.DeleteClientUseCase;
import app.lexo.application.port.in.ListClientsQuery;
import app.lexo.application.port.in.ListClientOptionsQuery;
import app.lexo.application.port.in.ClientSummary;
import app.lexo.application.port.in.ClientSummaryQuery;
import app.lexo.common.security.AuthUser;
import app.lexo.common.web.PageParamsMapper;
import app.lexo.common.web.PageResult;
import app.lexo.domain.ClientId;
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
@RequestMapping("/api/clientes")
class ClientController {

    private final CreateClientUseCase createClient;
    private final UpdateClientUseCase updateClient;
    private final DeleteClientUseCase deleteClient;
    private final GetClientQuery getClient;
    private final ListClientsQuery listClients;
    private final ListClientOptionsQuery listOptions;
    private final ClientSummaryQuery clientSummary;

    ClientController(CreateClientUseCase createClient, UpdateClientUseCase updateClient,
                      DeleteClientUseCase deleteClient, GetClientQuery getClient,
                      ListClientsQuery listClients, ListClientOptionsQuery listOptions,
                      ClientSummaryQuery clientSummary) {
        this.createClient = createClient;
        this.updateClient = updateClient;
        this.deleteClient = deleteClient;
        this.getClient = getClient;
        this.listClients = listClients;
        this.listOptions = listOptions;
        this.clientSummary = clientSummary;
    }

    @GetMapping
    PageResult<ClientResponse> list(
            @AuthenticationPrincipal AuthUser me,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return listClients.list(Identity.organization(me), q, PageParamsMapper.from(pageable));
    }

    @GetMapping("/opcoes")
    List<ClientOption> options(@AuthenticationPrincipal AuthUser me) {
        return listOptions.listOptions(Identity.organization(me));
    }

    @GetMapping("/resumo")
    ClientSummary summary(@AuthenticationPrincipal AuthUser me) {
        return clientSummary.summarize(Identity.organization(me));
    }

    @GetMapping("/{id}")
    ClientResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return getClient.get(Identity.organization(me), new ClientId(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ClientResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ClientRequest req) {
        return createClient.create(new CreateClientCommand(Identity.organization(me), Identity.actor(me), req.toData()));
    }

    @PutMapping("/{id}")
    ClientResponse update(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                           @Valid @RequestBody ClientRequest req) {
        return updateClient.update(
                new UpdateClientCommand(Identity.organization(me), new ClientId(id), Identity.actor(me), req.toData()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADVOGADO')")
    void delete(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        deleteClient.delete(new DeleteClientCommand(Identity.organization(me), new ClientId(id), Identity.actor(me)));
    }
}
