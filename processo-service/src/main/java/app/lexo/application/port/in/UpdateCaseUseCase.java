package app.lexo.application.port.in;

public interface UpdateCaseUseCase {

    CaseResponse update(UpdateCaseCommand command);
}
