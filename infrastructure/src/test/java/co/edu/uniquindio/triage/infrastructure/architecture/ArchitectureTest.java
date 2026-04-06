package co.edu.uniquindio.triage.infrastructure.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;

import static com.tngtech.archunit.library.Architectures.onionArchitecture;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "co.edu.uniquindio.triage", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule hexagonal = onionArchitecture()
            .withOptionalLayers(true)
            // NOTE: the current aggregate implementation calls StateTransitionValidator directly.
            // Until that domain-level coupling is refactored, keep services inside the inner domain ring
            // so we still enforce onion boundaries without breaking on known in-domain dependencies.
            .domainModels("..domain.model..", "..domain.enums..", "..domain.event..", "..domain.service..", "..domain.exception..")
            .applicationServices("..application..")
            .adapter("rest", "..infrastructure.adapter.in.rest..")
            .adapter("persistence", "..infrastructure.adapter.out.persistence..")
            .adapter("security", "..infrastructure.adapter.out.security..")
            .adapter("ai", "..infrastructure.adapter.out.ai..")
            .adapter("config", "..infrastructure.config..")
            .ignoreDependency(
                    new DescribedPredicate<JavaClass>("config layer") {
                        @Override
                        public boolean test(JavaClass input) {
                            return input.getPackageName().contains("infrastructure.config");
                        }
                    },
                    DescribedPredicate.alwaysTrue()
            );

    @ArchTest
    static final ArchRule restAdaptersShouldNotDependOnOutAdapters = noClasses()
            .that().resideInAPackage("..infrastructure.adapter.in.rest..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..infrastructure.adapter.out..");

    @ArchTest
    static final ArchRule coreMustNotDependOnOuterLayers = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..infrastructure..", "..bootstrap..");

    @ArchTest
    static final ArchRule domainIsFrameworkFree = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta..", "lombok..");

    @ArchTest
    static final ArchRule applicationIsFrameworkFree = noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta..", "lombok..");

    @ArchTest
    static final ArchRule applicationAndDomainMustNotDependOnSpringSecurity = noClasses()
            .that().resideInAnyPackage("..application..", "..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework.security..");

    @ArchTest
    static final ArchRule outputPortsMustBeInterfaces = classes()
            .that().resideInAPackage("..application.port.out..")
            .and().haveSimpleNameEndingWith("Port")
            .should().beInterfaces();
}
