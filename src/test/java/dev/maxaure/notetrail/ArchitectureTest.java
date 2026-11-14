package dev.maxaure.notetrail;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "dev.maxaure.notetrail", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainIsFreeOfFrameworks = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta..",
                    "com.fasterxml..",
                    "org.hibernate.."
            );

    @ArchTest
    static final ArchRule applicationIsFreeOfFrameworks = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta..",
                    "com.fasterxml..",
                    "org.hibernate.."
            );

    @ArchTest
    static final ArchRule applicationDoesNotDependOnAdaptersOrBootstrap = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..adapter..",
                    "..bootstrap.."
            );

    @ArchTest
    static final ArchRule inboundAdaptersDoNotDependOnOutboundAdapters = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter.out..");

    @ArchTest
    static final ArchRule domainDependsOnlyOnItself = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application..",
                    "..adapter..",
                    "..bootstrap.."
            );

    @ArchTest
    static final ArchRule outboundAdaptersDoNotDependOnInboundAdaptersOrBootstrap = noClasses()
            .that().resideInAPackage("..adapter.out..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..adapter.in..",
                    "..bootstrap.."
            );

    @ArchTest
    static final ArchRule coreErrorIsDependencyFree = noClasses()
            .that().resideInAPackage("..core.error..")
            .should().dependOnClassesThat().resideOutsideOfPackages(
                    "java..",
                    "..core.error.."
            );

    @ArchTest
    static final ArchRule globalWebAdapterKnowsNoFeatures = noClasses()
            .that().resideInAPackage("..adapter.web..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..user..",
                    "..item.."
            );

    @ArchTest
    static final ArchRule bootstrapIsNotDependedOn = noClasses()
            .that().resideOutsideOfPackage("..bootstrap..")
            .should().dependOnClassesThat().resideInAPackage("..bootstrap..");

    @ArchTest
    static final ArchRule adapterClassesArePackagePrivate = classes()
            .that().resideInAPackage("..adapter..")
            .and().areTopLevelClasses()
            .should().notBePublic();

    @ArchTest
    static final ArchRule domainDependsOnlyOnJdkAndCoreError = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideOutsideOfPackages(
                    "java..",
                    "..core.error..",
                    "..domain.."
            );

    @ArchTest
    static final ArchRule itemAndUserAreIndependent = noClasses()
            .that().resideInAPackage("..item..")
            .should().dependOnClassesThat().resideInAnyPackage("..user..");

    @ArchTest
    static final ArchRule userAndItemAreIndependent = noClasses()
            .that().resideInAPackage("..user..")
            .should().dependOnClassesThat().resideInAnyPackage("..item..");

    @ArchTest
    static final ArchRule securityDependsOnlyOnCore = noClasses()
            .that().resideInAPackage("dev.maxaure.notetrail.security..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..user..",
                    "..item..",
                    "..adapter..",
                    "..bootstrap.."
            );

    @ArchTest
    static final ArchRule applicationDependsOnlyOnCoreDomainAndItself = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideOutsideOfPackages(
                    "java..",
                    "..core.error..",
                    "..domain..",
                    "..application.."
            );
}
