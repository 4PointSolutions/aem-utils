package com._4point.aem.aem_utils.aem_cntrl.domain;

import static com.tngtech.archunit.library.Architectures.*;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * This class contains ArchUnit (https://www.archunit.org/) tests to verify that the code adheres to the hexagonal architecture 
 * as outlined in the architeture.md document at the root of this project.
 */
@AnalyzeClasses(packages = "com._4point.aem.aem_utils.aem_cntrl", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchUnitTest {
	private static final String MAIN_PACKAGE = "com._4point.aem.aem_utils.aem_cntrl";
	
	@ArchTest
	public static final ArchRule hexagonlArchRule = layeredArchitecture()
    											.consideringAllDependencies()
    											// Define the layers of the application architecture
    											.layer("Domain").definedBy(MAIN_PACKAGE + ".domain")
    											.layer("Domain.ports.api").definedBy(MAIN_PACKAGE + ".domain.ports.api")	// API ports (inbound requests)
    											.layer("Domain.ports.ipi").definedBy(MAIN_PACKAGE + ".domain.ports.ipi")	// Internal ports (internal implementations)
    											.layer("Domain.ports.spi").definedBy(MAIN_PACKAGE + ".domain.ports.spi")	// Service provider interfaces (outbound requests)
    											.layer("Adapters.spi").definedBy(MAIN_PACKAGE + ".adapters.spi")	// Adapters for outbound requests (service provider interfaces)
      											.layer("Adapters.ipi").definedBy(MAIN_PACKAGE + ".adapters.ipi")	// Adapters for internal requests (internal implementations)
    											.layer("Adapters.spi.ports").definedBy(MAIN_PACKAGE + ".adapters.spi.ports")		// Service provider interfaces for outbound SPI requests (service provider interfaces)
      											.layer("Adapters.spi.adapters").definedBy(MAIN_PACKAGE + ".adapters.spi.adapters")	// Adapters SPI service provider interfaces (adapters for outbound requests)
      											.layer("Commands").definedBy(MAIN_PACKAGE + ".commands")
											    .layer("Spring").definedBy(MAIN_PACKAGE)
											    // Define the layers that are external to the application that can be used in the architecture
											    .optionalLayer("JDK").definedBy("java..", "javax..")
											    .optionalLayer("SLF4J").definedBy("org.slf4j..")
											    .optionalLayer("DomainOther").definedBy("org.apache.commons.lang3..")
											    .optionalLayer("AdaptersSpiOther").definedBy("org.apache.commons.io.input..")
											    .optionalLayer("AdaptersSpiAdaptersOther").definedBy("com.fasterxml.jackson..", "org.springframework..")
											    .optionalLayer("CommandsOther").definedBy("picocli..", "org.springframework..")
											
											    // Spring can access all layers because it provides implementations for all layers.
											    // It needs access to the ports (interfaces) and adapters (implementations) so that it can tie everything together.
											    .whereLayer("Domain").mayOnlyBeAccessedByLayers("Spring")
											    .whereLayer("Domain").mayOnlyAccessLayers("Domain.ports.api", "Domain.ports.ipi", "Domain.ports.spi", "JDK", "SLF4J", "DomainOther")
											    .whereLayer("Domain.ports.api").mayOnlyBeAccessedByLayers("Spring", "Domain", "Commands")
											    .whereLayer("Domain.ports.api").mayOnlyAccessLayers("JDK", "SLF4J")
											    .whereLayer("Domain.ports.ipi").mayOnlyBeAccessedByLayers("Spring", "Domain", "Adapters.ipi")
											    .whereLayer("Domain.ports.ipi").mayOnlyAccessLayers("JDK", "SLF4J")
											    .whereLayer("Domain.ports.spi").mayOnlyBeAccessedByLayers("Spring", "Domain", "Adapters.spi")
											    .whereLayer("Domain.ports.spi").mayOnlyAccessLayers("JDK", "SLF4J")
											    .whereLayer("Adapters.ipi").mayOnlyBeAccessedByLayers("Spring")
											    .whereLayer("Adapters.ipi").mayOnlyAccessLayers("Domain.ports.ipi", "JDK", "SLF4J")
											    .whereLayer("Adapters.spi").mayOnlyBeAccessedByLayers("Spring")
											    .whereLayer("Adapters.spi").mayOnlyAccessLayers("Domain.ports.spi", "Adapters.spi.ports", "JDK", "SLF4J", "AdaptersSpiOther")
											    .whereLayer("Adapters.spi.ports").mayOnlyBeAccessedByLayers("Spring", "Adapters.spi", "Adapters.spi.adapters")
											    .whereLayer("Adapters.spi.ports").mayOnlyAccessLayers("Adapters.spi", "JDK", "SLF4J")
											    .whereLayer("Adapters.spi.adapters").mayOnlyBeAccessedByLayers("Spring")
											    .whereLayer("Adapters.spi.adapters").mayOnlyAccessLayers("Adapters.spi.ports", "JDK", "SLF4J", "AdaptersSpiAdaptersOther")
											    .whereLayer("Commands").mayOnlyBeAccessedByLayers("Spring")
											    .whereLayer("Commands").mayOnlyAccessLayers("Domain.ports.api", "JDK", "SLF4J", "CommandsOther")
											    ;
}


