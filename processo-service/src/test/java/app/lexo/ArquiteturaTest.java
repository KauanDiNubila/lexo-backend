package app.lexo;

import app.lexo.controller.InternalCaseController;
import app.lexo.controller.PortalInternalController;
import app.lexo.repository.AndamentoRepository;
import app.lexo.repository.CaseRepository;
import app.lexo.repository.DeadlineRepository;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "app.lexo", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

    @ArchTest
    static final ArchRule camadas = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Controller").definedBy("app.lexo.controller..")
            .layer("Service").definedBy("app.lexo.service..")
            .layer("Repository").definedBy("app.lexo.repository..")
            .layer("Domain").definedBy("app.lexo.domain..")
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
            .whereLayer("Domain").mayNotAccessAnyLayer()
            .ignoreDependency(InternalCaseController.class, CaseRepository.class)
            .ignoreDependency(PortalInternalController.class, CaseRepository.class)
            .ignoreDependency(PortalInternalController.class, DeadlineRepository.class)
            .ignoreDependency(PortalInternalController.class, AndamentoRepository.class);
}
