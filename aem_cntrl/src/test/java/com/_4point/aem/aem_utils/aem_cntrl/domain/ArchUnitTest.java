package com._4point.aem.aem_utils.aem_cntrl.domain;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.*;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com._4point.aem.aem_utils.aem_cntrl", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchUnitTest {
	private static final String MAIN_PACKAGE = "com._4point.aem.aem_utils.aem_cntrl.";
//	@ArchTest
//    public static final ArchRule myRule = classes()
//        .that().resideInAPackage("..service..")
//        .should().onlyBeAccessed().byAnyPackage("..controller..", "..service..");

	@ArchTest
	public static final ArchRule hexagonlArchRule = layeredArchitecture()
    											.consideringAllDependencies()
    											.layer("Domain").definedBy(MAIN_PACKAGE + "domain..")
    											.layer("Adapters").definedBy(MAIN_PACKAGE + "adapters..")
											    .layer("Commands").definedBy(MAIN_PACKAGE + "commands..")
											    .optionalLayer("JDK").definedBy("java..", "javax..")
											    .optionalLayer("Other").definedBy("org.slf4j..", "org.apache.commons.lang3..")
											
											    .whereLayer("Domain").mayOnlyAccessLayers("Domain", "JDK", "Other")
//											    .whereLayer("Adapters").mayNotBeAccessedByAnyLayer()
//											    .whereLayer("Adapters").mayOnlyAccessLayers("Domain")
//											    .whereLayer("Commands").mayOnlyAccessLayers("Domain")
											    ;

}


