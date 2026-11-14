package app.lexo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Enum codes stored in the database and sent in the JSON")
class EnumCodesTest {

    @Test
    @DisplayName("case status keeps its codes and its order")
    void caseStatus() {
        assertThat(CaseStatus.values()).extracting(CaseStatus::code)
                .containsExactly("ATIVO", "SUSPENSO", "ARQUIVADO", "ENCERRADO");
    }

    @Test
    @DisplayName("deadline status keeps its codes")
    void deadlineStatus() {
        assertThat(DeadlineStatus.values()).extracting(DeadlineStatus::code)
                .containsExactly("PENDENTE", "CONCLUIDO", "PERDIDO");
    }

    @Test
    @DisplayName("deadline type keeps its codes and risk factors")
    void deadlineType() {
        assertThat(DeadlineType.values()).extracting(DeadlineType::code)
                .containsExactly("PRAZO", "AUDIENCIA", "REUNIAO", "OUTRO");
        assertThat(DeadlineType.DEADLINE.riskFactor()).isEqualTo(1.5);
        assertThat(DeadlineType.HEARING.riskFactor()).isEqualTo(1.2);
        assertThat(DeadlineType.OTHER.riskFactor()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("every code maps back; an unknown code is rejected")
    void fromCode() {
        for (CaseStatus status : CaseStatus.values()) {
            assertThat(CaseStatus.fromCode(status.code())).isEqualTo(status);
        }
        for (DeadlineStatus status : DeadlineStatus.values()) {
            assertThat(DeadlineStatus.fromCode(status.code())).isEqualTo(status);
        }
        for (DeadlineType type : DeadlineType.values()) {
            assertThat(DeadlineType.fromCode(type.code())).isEqualTo(type);
        }
        assertThatThrownBy(() -> CaseStatus.fromCode("INEXISTENTE")).isInstanceOf(IllegalArgumentException.class);
    }
}
