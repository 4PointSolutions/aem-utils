package com._4point.aem.aem_utils.aem_cntrl.domain;

import static com.tngtech.archunit.library.Architectures.*;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com._4point.aem.aem_utils.aem_cntrl", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchUnitTest {
	private static final String MAIN_PACKAGE = "com._4point.aem.aem_utils.aem_cntrl";
//	@ArchTest
//    public static final ArchRule myRule = classes()
//        .that().resideInAPackage("..service..")
//        .should().onlyBeAccessed().byAnyPackage("..controller..", "..service..");

	@ArchTest
	public static final ArchRule hexagonlArchRule = layeredArchitecture()
    											.consideringAllDependencies()
    											.layer("Domain").definedBy(MAIN_PACKAGE + ".domain")
    											.layer("Domain.api").definedBy(MAIN_PACKAGE + ".domain.ports.api")	// API ports (inbound requests)
    											.layer("Domain.ipi").definedBy(MAIN_PACKAGE + ".domain.ports.ipi")	// Internal ports (internal implementations)
    											.layer("Domain.spi").definedBy(MAIN_PACKAGE + ".domain.ports.spi")	// Service provider interfaces (outbound requests)
    											.layer("Adapters.spi").definedBy(MAIN_PACKAGE + ".adapters.spi")	// Adapters for outbound requests (service provider interfaces)
      											.layer("Adapters.ipi").definedBy(MAIN_PACKAGE + ".adapters.ipi")	// Adapters for internal requests (internal implementations)
    											.layer("Adapters.spi.ports").definedBy(MAIN_PACKAGE + ".adapters.spi.ports")		// Service provider interfaces for outbound SPI requests (service provider interfaces)
      											.layer("Adapters.spi.adapters").definedBy(MAIN_PACKAGE + ".adapters.spi.adapters")	// Adapters SPI service provider interfaces (adapters for outbound requests)
      											.layer("Commands").definedBy(MAIN_PACKAGE + ".commands")
											    .layer("Spring").definedBy(MAIN_PACKAGE)
											    .optionalLayer("JDK").definedBy("java..", "javax..")
											    .optionalLayer("DomainOther").definedBy("org.slf4j..", "org.apache.commons.lang3..")
											
											    .whereLayer("Domain").mayOnlyAccessLayers("Domain", "Domain.api", "Domain.ipi", "Domain.spi", "JDK", "DomainOther")
											    .whereLayer("Domain.api").mayOnlyBeAccessedByLayers("Domain", "Spring", "Commands")
											    .whereLayer("Domain.ipi").mayOnlyAccessLayers("Domain", "JDK")
											    .whereLayer("Domain.ipi").mayOnlyBeAccessedByLayers("Domain", "Spring", "Adapters.ipi")
											    .whereLayer("Domain.spi").mayOnlyAccessLayers("Domain", "JDK")
											    .whereLayer("Domain.spi").mayOnlyBeAccessedByLayers("Domain", "Spring", "Adapters.spi")
											    .whereLayer("Adapters.ipi").mayOnlyBeAccessedByLayers("Spring")
											    .whereLayer("Adapters.spi").mayOnlyBeAccessedByLayers("Spring")
											    .whereLayer("Adapters.spi.ports").mayOnlyBeAccessedByLayers("Spring", "Adapters.spi", "Adapters.spi.adapters")
											    .whereLayer("Adapters.spi.adapters").mayOnlyBeAccessedByLayers("Spring")
//											    .whereLayer("Adapters").mayOnlyAccessLayers("Domain")
											    .whereLayer("Commands").mayOnlyBeAccessedByLayers("Spring")
//											    .whereLayer("Commands").mayOnlyAccessLayers("Domain")
											    ;

}


