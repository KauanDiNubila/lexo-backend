package app.lexo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InvoiceStatus")
class InvoiceStatusTest {

    @Test
    @DisplayName("keeps the codes stored in the database and sent in the JSON")
    void codes() {
        assertThat(InvoiceStatus.PENDING.code()).isEqualTo("PENDENTE");
        assertThat(InvoiceStatus.PAID.code()).isEqualTo("PAGO");
        assertThat(InvoiceStatus.OVERDUE.code()).isEqualTo("ATRASADO");
        assertThat(InvoiceStatus.CANCELLED.code()).isEqualTo("CANCELADO");
    }

    @Test
    @DisplayName("every code maps back to its status; an unknown code is rejected")
    void fromCode() {
        for (InvoiceStatus status : InvoiceStatus.values()) {
            assertThat(InvoiceStatus.fromCode(status.code())).isEqualTo(status);
        }
        assertThatThrownBy(() -> InvoiceStatus.fromCode("INEXISTENTE")).isInstanceOf(IllegalArgumentException.class);
    }
}
