package app.lexo;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(
        packages = {"app.lexo.adapter", "app.lexo.application", "app.lexo.domain", "app.lexo.config"},
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainHasNoPersistenceOrWebFramework = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "..application..", "..adapter..");

    @ArchTest
    static final ArchRule applicationHasNoWebOrPersistence = noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..adapter..", "org.springframework.web..", "org.springframework.data..",
                    "org.springframework.security..", "jakarta.persistence..", "jakarta.servlet..",
                    "app.lexo.common.security..");

    @ArchTest
    static final ArchRule inboundSeesOnlyInboundPorts = noClasses().that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..adapter.out..", "..application.service..", "..application.port.out..");

    @ArchTest
    static final ArchRule outboundDoesNotSeeInboundOrServices = noClasses().that().resideInAPackage("..adapter.out..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter.in..", "..application.service..");

    @ArchTest
    static final ArchRule nothingOutsideAdaptersDependsOnThem = noClasses().that().resideOutsideOfPackage("..adapter..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..");

    @ArchTest
    static final ArchRule transactionOnlyInUseCases = noClasses().that().resideOutsideOfPackage("..application.service..")
            .should().beAnnotatedWith(Transactional.class);

    @ArchTest
    static final ArchRule transactionalMethodOnlyInUseCases = noMethods().that().areDeclaredInClassesThat()
            .resideOutsideOfPackage("..application.service..").should().beAnnotatedWith(Transactional.class);

    @ArchTest
    static final ArchRule servicesAndAdaptersArePackagePrivate = classes().that().areTopLevelClasses()
            .and().areNotInterfaces()
            .and().resideInAnyPackage("..application.service..", "..adapter..")
            .should().notBePublic();

    @ArchTest
    static final ArchRule noCycles = slices().matching("app.lexo.(*)..").should().beFreeOfCycles();
}
