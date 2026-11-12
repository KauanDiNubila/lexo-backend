package app.lexo.application.port.in;

public interface RenameProfileUseCase {

    ProfileInfo rename(RenameProfileCommand command);
}
