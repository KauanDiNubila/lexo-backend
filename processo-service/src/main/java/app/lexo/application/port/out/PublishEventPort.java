package app.lexo.application.port.out;

public interface PublishEventPort {

    void publish(CaseEvent event);
}
