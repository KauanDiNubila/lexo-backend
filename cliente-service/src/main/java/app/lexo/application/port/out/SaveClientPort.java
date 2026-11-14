package app.lexo.application.port.out;

import app.lexo.domain.Client;

public interface SaveClientPort {

    Client save(Client client);
}
