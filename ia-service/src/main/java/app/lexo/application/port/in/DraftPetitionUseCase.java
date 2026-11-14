package app.lexo.application.port.in;

public interface DraftPetitionUseCase {

    PetitionResponse draft(DraftPetitionCommand command);
}
