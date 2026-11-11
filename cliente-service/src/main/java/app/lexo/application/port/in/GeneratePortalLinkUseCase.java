package app.lexo.application.port.in;

public interface GeneratePortalLinkUseCase {

    PortalLinkResponse generate(GeneratePortalLinkCommand command);
}
