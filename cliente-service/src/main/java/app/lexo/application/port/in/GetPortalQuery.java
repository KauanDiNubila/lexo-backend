package app.lexo.application.port.in;

public interface GetPortalQuery {

    PortalResponse get(String token);
}
