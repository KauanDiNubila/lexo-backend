package app.lexo.common.web;

import app.lexo.common.error.ConflictException;
import app.lexo.common.error.InvalidDataException;
import app.lexo.common.error.ServiceUnavailableException;
import app.lexo.common.error.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler (pure exceptions)")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/clientes/1");

    @Test
    @DisplayName("resource not found becomes 404 with code NAO_ENCONTRADO and the same message")
    void notFound() {
        ResponseEntity<ErrorResponse> response =
                handler.handleNotFound(new ResourceNotFoundException("Cliente não encontrado"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.NAO_ENCONTRADO);
        assertThat(response.getBody().error()).isEqualTo("Cliente não encontrado");
        assertThat(response.getBody().path()).isEqualTo("/api/clientes/1");
    }

    @Test
    @DisplayName("invalid data becomes 400 with code REQUISICAO_INVALIDA and the same message")
    void invalidData() {
        ResponseEntity<ErrorResponse> response =
                handler.handleInvalidData(new InvalidDataException("CPF ou CNPJ inválido"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.REQUISICAO_INVALIDA);
        assertThat(response.getBody().error()).isEqualTo("CPF ou CNPJ inválido");
    }

    @Test
    @DisplayName("service unavailable becomes 503 with the code SERVICO_INDISPONIVEL and the same message")
    void serviceUnavailable() {
        ResponseEntity<ErrorResponse> response =
                handler.handleServiceUnavailable(new ServiceUnavailableException("try again"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.SERVICO_INDISPONIVEL);
        assertThat(response.getBody().error()).isEqualTo("try again");
    }

    @Test
    @DisplayName("a conflict becomes 409 with the code CONFLITO and the same message")
    void conflict() {
        ResponseEntity<ErrorResponse> response =
                handler.handleConflict(new ConflictException("already exists"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.CONFLITO);
        assertThat(response.getBody().error()).isEqualTo("already exists");
    }

    @Test
    @DisplayName("a parameter that cannot be converted becomes 400 with the code REQUISICAO_INVALIDA and its name")
    void typeMismatch() {
        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(
                new org.springframework.web.method.annotation.MethodArgumentTypeMismatchException(
                        "BOGUS", Integer.class, "status", null, null), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.REQUISICAO_INVALIDA);
        assertThat(response.getBody().error()).isEqualTo("Par\u00e2metro inv\u00e1lido: status");
    }

    @Test
    @DisplayName("ApiException keeps the previous behavior")
    void apiExceptionUnchanged() {
        ResponseEntity<ErrorResponse> response =
                handler.handleApi(ApiException.notFound("Processo não encontrado"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.NAO_ENCONTRADO);
    }
}
