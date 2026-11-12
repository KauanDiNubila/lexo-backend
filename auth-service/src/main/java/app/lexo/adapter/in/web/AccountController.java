package app.lexo.adapter.in.web;

import app.lexo.application.port.in.AccountResponse;
import app.lexo.application.port.in.GetAccountQuery;
import app.lexo.application.port.in.OrganizationInfo;
import app.lexo.application.port.in.ProfileInfo;
import app.lexo.application.port.in.RenameOrganizationCommand;
import app.lexo.application.port.in.RenameOrganizationUseCase;
import app.lexo.application.port.in.RenameProfileCommand;
import app.lexo.application.port.in.RenameProfileUseCase;
import app.lexo.common.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AccountController {

    private final GetAccountQuery getAccountQuery;
    private final RenameOrganizationUseCase renameOrganizationUseCase;
    private final RenameProfileUseCase renameProfileUseCase;

    AccountController(GetAccountQuery getAccountQuery, RenameOrganizationUseCase renameOrganizationUseCase,
                      RenameProfileUseCase renameProfileUseCase) {
        this.getAccountQuery = getAccountQuery;
        this.renameOrganizationUseCase = renameOrganizationUseCase;
        this.renameProfileUseCase = renameProfileUseCase;
    }

    @GetMapping("/api/conta")
    AccountResponse account(@AuthenticationPrincipal AuthUser me) {
        return getAccountQuery.get(Identity.organization(me), Identity.user(me));
    }

    @PutMapping("/api/organizacao")
    @PreAuthorize("hasRole('ADMIN')")
    OrganizationInfo renameOrganization(@AuthenticationPrincipal AuthUser me,
                                        @Valid @RequestBody RenameOrganizationRequest request) {
        return renameOrganizationUseCase.rename(
                new RenameOrganizationCommand(Identity.organization(me), request.name()));
    }

    @PutMapping("/api/perfil")
    ProfileInfo renameProfile(@AuthenticationPrincipal AuthUser me,
                              @Valid @RequestBody RenameProfileRequest request) {
        return renameProfileUseCase.rename(
                new RenameProfileCommand(Identity.organization(me), Identity.user(me), request.name()));
    }
}
