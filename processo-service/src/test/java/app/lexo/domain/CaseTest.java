package app.lexo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Case")
class CaseTest {

    private static final OrganizationId ORGANIZATION = new OrganizationId("org-1");

    private static CaseData data(String number, String area, String description, UserId responsible) {
        return new CaseData(new ClientId("client-1"), number, area, CaseStatus.ACTIVE, description, responsible);
    }

    @Test
    @DisplayName("opening a case keeps the data and has no id yet")
    void open() {
        Case opened = Case.open(ORGANIZATION, data("0001", "Civel", "text", new UserId("u-1")));

        assertThat(opened.id()).isNull();
        assertThat(opened.createdAt()).isNull();
        assertThat(opened.organizationId()).isEqualTo(ORGANIZATION);
        assertThat(opened.data().number()).isEqualTo("0001");
        assertThat(opened.data().responsibleId()).isEqualTo(new UserId("u-1"));
    }

    @Test
    @DisplayName("a blank area and a blank description become absent")
    void blankBecomesAbsent() {
        CaseData data = data("0002", "  ", "", null);

        assertThat(data.area()).isNull();
        assertThat(data.description()).isNull();
    }

    @Test
    @DisplayName("a blank responsible becomes absent, a filled one is kept")
    void optionalResponsible() {
        assertThat(UserId.optional(" ")).isNull();
        assertThat(UserId.optional(null)).isNull();
        assertThat(UserId.optional("u-2")).isEqualTo(new UserId("u-2"));
    }

    @Test
    @DisplayName("updating replaces the data and keeps identity and creation time")
    void update() {
        Instant created = Instant.parse("2026-01-01T00:00:00Z");
        Case restored = Case.restore(new CaseId("c-1"), ORGANIZATION, data("0003", null, null, null), created);

        restored.update(data("0003-B", "Trabalhista", "changed", null));

        assertThat(restored.id()).isEqualTo(new CaseId("c-1"));
        assertThat(restored.createdAt()).isEqualTo(created);
        assertThat(restored.data().number()).isEqualTo("0003-B");
        assertThat(restored.data().area()).isEqualTo("Trabalhista");
    }

    @Test
    @DisplayName("ids reject empty values")
    void idsRejectEmpty() {
        assertThatThrownBy(() -> new CaseId(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrganizationId(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
