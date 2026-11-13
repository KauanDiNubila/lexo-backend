package app.lexo.adapter.out.persistence;

import app.lexo.domain.Client;
import app.lexo.domain.ClientId;
import app.lexo.domain.TaxId;
import app.lexo.domain.ClientData;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.PortalLink;

final class ClientMapper {

    private ClientMapper() {
    }

    static Client toDomain(ClientJpaEntity entity) {
        ClientData data = new ClientData(entity.name, TaxId.stored(entity.document),
                entity.email, entity.phone, entity.notes);
        PortalLink link = entity.portalTokenHash == null
                ? null
                : new PortalLink(entity.portalTokenHash, entity.portalTokenExpiresAt);
        return Client.restore(new ClientId(entity.getId()), new OrganizationId(entity.organizationId),
                data, link, entity.getCreatedAt());
    }

    static void copyTo(Client client, ClientJpaEntity entity) {
        ClientData data = client.data();
        entity.organizationId = client.organizationId().value();
        entity.name = data.name();
        entity.document = data.taxId() == null ? null : data.taxId().text();
        entity.email = data.email();
        entity.phone = data.phone();
        entity.notes = data.notes();
        PortalLink link = client.portalLink();
        entity.portalTokenHash = link == null ? null : link.tokenHash();
        entity.portalTokenExpiresAt = link == null ? null : link.expiresAt();
    }
}
