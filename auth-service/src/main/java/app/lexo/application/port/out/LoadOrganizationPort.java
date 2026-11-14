package app.lexo.application.port.out;

import app.lexo.domain.Organization;
import app.lexo.domain.OrganizationId;

import java.util.Optional;

public interface LoadOrganizationPort {

    Optional<Organization> byId(OrganizationId id);
}
