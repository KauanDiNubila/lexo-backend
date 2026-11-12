package app.lexo.application.port.out;

import app.lexo.application.port.in.Actor;
import app.lexo.common.event.EventType;
import app.lexo.domain.Case;
import app.lexo.domain.CaseId;
import app.lexo.domain.OrganizationId;

public record CaseEvent(String type, OrganizationId organizationId, CaseId caseId, String description,
                        Actor actor) {

    public static final String CREATED = EventType.CASE_CREATED.code();
    public static final String UPDATED = EventType.CASE_UPDATED.code();
    public static final String DELETED = EventType.CASE_DELETED.code();

    public static CaseEvent created(Case value, Actor actor) {
        return new CaseEvent(CREATED, value.organizationId(), value.id(),
                "Processo criado: " + value.data().number(), actor);
    }

    public static CaseEvent updated(Case value, Actor actor) {
        return new CaseEvent(UPDATED, value.organizationId(), value.id(),
                "Processo atualizado: " + value.data().number(), actor);
    }

    public static CaseEvent deleted(Case value, Actor actor) {
        return new CaseEvent(DELETED, value.organizationId(), value.id(),
                "Processo exclu\u00eddo: " + value.data().number(), actor);
    }
}
