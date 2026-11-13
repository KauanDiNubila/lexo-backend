package app.lexo.application.service;

import app.lexo.application.PortalSettings;
import app.lexo.application.port.in.GeneratePortalLinkCommand;
import app.lexo.application.port.in.GeneratePortalLinkUseCase;
import app.lexo.application.port.in.PortalLinkResponse;
import app.lexo.application.port.out.LoadClientPort;
import app.lexo.application.port.out.SaveClientPort;
import app.lexo.domain.Client;
import app.lexo.domain.ClientNotFoundException;
import app.lexo.domain.PortalToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
class GeneratePortalLinkService implements GeneratePortalLinkUseCase {

    private final LoadClientPort loadClientPort;
    private final SaveClientPort saveClientPort;
    private final Clock clock;
    private final PortalSettings settings;

    GeneratePortalLinkService(LoadClientPort loadClientPort, SaveClientPort saveClientPort,
                             Clock clock, PortalSettings settings) {
        this.loadClientPort = loadClientPort;
        this.saveClientPort = saveClientPort;
        this.clock = clock;
        this.settings = settings;
    }

    @Override
    public PortalLinkResponse generate(GeneratePortalLinkCommand command) {
        Client client = loadClientPort.byId(command.organizationId(), command.clientId())
                .orElseThrow(ClientNotFoundException::new);
        String token = PortalToken.generate();
        client.generatePortalLink(PortalToken.hash(token), clock.instant(), settings.validityDays());
        saveClientPort.save(client);
        return new PortalLinkResponse(token);
    }
}
