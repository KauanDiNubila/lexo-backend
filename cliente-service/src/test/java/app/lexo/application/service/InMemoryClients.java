package app.lexo.application.service;

import app.lexo.application.port.out.LoadClientPort;
import app.lexo.application.port.out.DeleteClientPort;
import app.lexo.application.port.out.SaveClientPort;
import app.lexo.domain.Client;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class InMemoryClients implements SaveClientPort, LoadClientPort, DeleteClientPort {

    static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");

    private final Map<String, Client> clients = new LinkedHashMap<>();
    private int sequence;

    @Override
    public Client save(Client client) {
        Client saved = client.id() == null
                ? Client.restore(new ClientId("c" + (++sequence)), client.organizationId(), client.data(),
                        client.portalLink(), NOW)
                : client;
        clients.put(saved.id().value(), saved);
        return saved;
    }

    @Override
    public Optional<Client> byId(OrganizationId organizationId, ClientId clientId) {
        return Optional.ofNullable(clients.get(clientId.value()))
                .filter(c -> c.organizationId().equals(organizationId));
    }

    @Override
    public Optional<Client> byPortalLink(String token) {
        return clients.values().stream()
                .filter(c -> c.portalLink() != null && c.portalLink().token().equals(token))
                .findFirst();
    }

    @Override
    public void delete(Client client) {
        clients.remove(client.id().value());
    }

    List<Client> all() {
        return new ArrayList<>(clients.values());
    }
}
