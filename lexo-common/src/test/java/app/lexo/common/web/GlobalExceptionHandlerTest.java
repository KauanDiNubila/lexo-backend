package app.lexo.common.web;

import app.lexo.common.error.InvalidDataException;
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
    @DisplayName("ApiException keeps the previous behavior")
    void apiExceptionUnchanged() {
        ResponseEntity<ErrorResponse> response =
                handler.handleApi(ApiException.notFound("Processo não encontrado"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.NAO_ENCONTRADO);
    }
}
