package app.lexo.application.port.out;

import app.lexo.domain.Client;

public interface DeleteClientPort {

    void delete(Client client);
}
