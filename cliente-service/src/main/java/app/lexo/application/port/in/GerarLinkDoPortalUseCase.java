package app.lexo.application.port.in;

public interface GerarLinkDoPortalUseCase {

    LinkDoPortalResponse gerar(GerarLinkDoPortalCommand command);
}
