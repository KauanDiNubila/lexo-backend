package app.lexo.common.web;

import app.lexo.common.erro.DadoInvalidoException;
import app.lexo.common.erro.RecursoNaoEncontradoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler (excecoes puras)")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest requisicao = new MockHttpServletRequest("GET", "/api/clientes/1");

    @Test
    @DisplayName("recurso nao encontrado vira 404 com o codigo NAO_ENCONTRADO e a mesma mensagem")
    void naoEncontrado() {
        ResponseEntity<ErrorResponse> resposta =
                handler.handleNaoEncontrado(new RecursoNaoEncontradoException("Cliente não encontrado"), requisicao);

        assertThat(resposta.getStatusCode().value()).isEqualTo(404);
        assertThat(resposta.getBody().code()).isEqualTo(ErrorCode.NAO_ENCONTRADO);
        assertThat(resposta.getBody().error()).isEqualTo("Cliente não encontrado");
        assertThat(resposta.getBody().path()).isEqualTo("/api/clientes/1");
    }

    @Test
    @DisplayName("dado invalido vira 400 com o codigo REQUISICAO_INVALIDA e a mesma mensagem")
    void dadoInvalido() {
        ResponseEntity<ErrorResponse> resposta =
                handler.handleDadoInvalido(new DadoInvalidoException("CPF ou CNPJ inválido"), requisicao);

        assertThat(resposta.getStatusCode().value()).isEqualTo(400);
        assertThat(resposta.getBody().code()).isEqualTo(ErrorCode.REQUISICAO_INVALIDA);
        assertThat(resposta.getBody().error()).isEqualTo("CPF ou CNPJ inválido");
    }

    @Test
    @DisplayName("ApiException continua com o comportamento de antes")
    void apiExceptionInalterada() {
        ResponseEntity<ErrorResponse> resposta =
                handler.handleApi(ApiException.notFound("Processo não encontrado"), requisicao);

        assertThat(resposta.getStatusCode().value()).isEqualTo(404);
        assertThat(resposta.getBody().code()).isEqualTo(ErrorCode.NAO_ENCONTRADO);
    }
}
