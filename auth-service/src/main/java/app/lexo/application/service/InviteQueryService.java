package app.lexo.application.service;

import app.lexo.application.port.in.GetInviteInfoQuery;
import app.lexo.application.port.in.InviteInfo;
import app.lexo.application.port.in.InviteResponse;
import app.lexo.application.port.in.ListPendingInvitesQuery;
import app.lexo.application.port.out.ListPendingInvitesPort;
import app.lexo.application.port.out.LoadInvitePort;
import app.lexo.application.port.out.LoadOrganizationPort;
import app.lexo.domain.Organization;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.UserInvite;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
@Transactional(readOnly = true)
class InviteQueryService implements ListPendingInvitesQuery, GetInviteInfoQuery {

    private static final String DEFAULT_ORGANIZATION_NAME = "Lexo";

    private final ListPendingInvitesPort listPendingInvitesPort;
    private final LoadInvitePort loadInvitePort;
    private final LoadOrganizationPort loadOrganizationPort;
    private final Clock clock;

    InviteQueryService(ListPendingInvitesPort listPendingInvitesPort, LoadInvitePort loadInvitePort,
                       LoadOrganizationPort loadOrganizationPort, Clock clock) {
        this.listPendingInvitesPort = listPendingInvitesPort;
        this.loadInvitePort = loadInvitePort;
        this.loadOrganizationPort = loadOrganizationPort;
        this.clock = clock;
    }

    @Override
    public List<InviteResponse> list(OrganizationId organizationId) {
        return listPendingInvitesPort.of(organizationId).stream().map(InviteResponse::of).toList();
    }

    @Override
    public InviteInfo get(String token) {
        UserInvite invite = loadInvitePort.byToken(token).orElse(null);
        if (invite == null) {
            return new InviteInfo(null, null, null, false, "Convite inválido");
        }
        return switch (invite.statusAt(clock.instant())) {
            case ACCEPTED -> unusable(invite, "Este convite já foi utilizado");
            case EXPIRED -> unusable(invite, "Este convite expirou");
            case PENDING -> new InviteInfo(organizationNameOf(invite), invite.name(), invite.email().value(),
                    true, null);
        };
    }

    private InviteInfo unusable(UserInvite invite, String reason) {
        return new InviteInfo(null, invite.name(), invite.email().value(), false, reason);
    }

    private String organizationNameOf(UserInvite invite) {
        return loadOrganizationPort.byId(invite.organizationId()).map(Organization::name)
                .orElse(DEFAULT_ORGANIZATION_NAME);
    }
}
