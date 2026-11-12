package app.lexo.application.port.in;

public interface CreateCaseUseCase {

    CaseResponse create(CreateCaseCommand command);
}
