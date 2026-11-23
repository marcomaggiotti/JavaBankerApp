package com.javabanker.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** Clean/Hexagonal Architecture rules as executable tests - the build fails if a layer leaks. */
@AnalyzeClasses(packages = "com.javabanker", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainIsFrameworkFree = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "org.apache.kafka..", "jakarta..", "com.fasterxml..",
                    "..application..", "..adapter..", "..streams..", "..config..");

    @ArchTest
    static final ArchRule applicationDependsOnlyOnDomain = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "org.apache.kafka..", "..adapter..", "..streams..", "..config..");

    @ArchTest
    static final ArchRule adaptersDoNotTalkToEachOther = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.out..");

    @ArchTest
    static final ArchRule productionCodeDoesNotDependOnStudyMaterial = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..", "..adapter..", "..streams..", "..config..")
            .should().dependOnClassesThat().resideInAnyPackage("..exercises..", "..interview..", "..challenges..");
}
