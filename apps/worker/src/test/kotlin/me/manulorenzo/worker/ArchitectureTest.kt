package me.manulorenzo.worker

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes

@AnalyzeClasses(packages = ["me.manulorenzo.worker"], importOptions = [ImportOption.DoNotIncludeTests::class])
class ArchitectureTest {

    /**
     * Enforces the Layered Architecture defined in ADR 0001.
     *
     * Layers:
     * - API (Controllers)
     * - Service (Business Logic)
     * - Repository (Persistence)
     * - Config (Wiring)
     * - Observability (Logging/Metrics)
     */
    @ArchTest
    val `layered architecture is respected`: ArchRule = layeredArchitecture()
        .consideringOnlyDependenciesInAnyPackage("me.manulorenzo.worker..")
        .layer("Api").definedBy("..api..")
        .layer("Service").definedBy("..service..")
        .layer("Repository").definedBy("..repository..")
        .layer("Config").definedBy("..config..")
        .layer("Observability").definedBy("..observability..")

        .whereLayer("Api").mayNotBeAccessedByAnyLayer()
        .whereLayer("Service").mayOnlyBeAccessedByLayers("Api", "Config")
        .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service", "Config")
        .whereLayer("Observability").mayOnlyBeAccessedByLayers("Api", "Service", "Repository", "Config")

    /**
     * Explicit check mentioned in ADR 0001 Compliance section.
     */
    @ArchTest
    val `api should not access repository directly`: ArchRule = noClasses()
        .that().resideInAPackage("..api..")
        .should().dependOnClassesThat().resideInAPackage("..repository..")
        .because("Controllers should delegate to Services, not Repositories directly (ADR 0001)")

    @ArchTest
    val `services should be suffixed`: ArchRule = classes()
        .that().resideInAPackage("..service..")
        .should().haveSimpleNameEndingWith("Service")

    @ArchTest
    val `repositories should be suffixed`: ArchRule = classes()
        .that().resideInAPackage("..repository..")
        .should().haveSimpleNameEndingWith("Repository")

    @ArchTest
    val `controllers should be suffixed`: ArchRule = classes()
        .that().resideInAPackage("..api..")
        .should().haveSimpleNameEndingWith("Controller")
}