package app.lexo.application.port.out;

import app.lexo.domain.Organization;

public interface SaveOrganizationPort {

    Organization save(Organization organization);
}
