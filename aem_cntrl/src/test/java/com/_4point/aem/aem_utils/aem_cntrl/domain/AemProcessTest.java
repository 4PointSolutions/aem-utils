package com._4point.aem.aem_utils.aem_cntrl.domain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.CommonsIoTailerTailer;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer.TailerFactory;

@Disabled("The AemProcessTest tests are designed to be run manually, not as part of the automated test suite.  Comment out this line to run the individual tests manually.")
class AemProcessTest {
	private static final TailerFactory TAILER_FACTORY = new CommonsIoTailerTailer.TailerFactory();
	
	// Assumes a directory structure containing install files has already been created. 
//	private static final Path AEM_FILES_LOC = Path.of("/opt/adobe/AEM_65_SP21");		// Linux Location
	private static final Path AEM_FILES_LOC = Path.of("/Adobe", "AEM_65_SP23");		// Windows location
	private static final Path SAMPLE_AEM_QUICKSTART_PATH = Path.of("AEM_6.5_Quickstart.jar"); // AEM 6.5 SP19-23
	private static final JavaVersion AEM_JAVA_VERSION = JavaVersion.VERSION_11; // AEM 6.5 ORIG requires Java 11
//	private static final Path AEM_FILES_LOC = Path.of("/Adobe", "AEM_65_LTS");		// Windows location
//	private static final Path SAMPLE_AEM_QUICKSTART_PATH = Path.of("cq-quickstart-6.6.0.jar"); // AEM 6.5 LTS
//	private static final JavaVersion AEM_JAVA_VERSION = JavaVersion.VERSION_21; // AEM 6.5 LTS requires Java 21
	
	
	
	private final AemProcess underTest = new AemProcess(AEM_FILES_LOC.resolve(AEM_FILES_LOC), TAILER_FACTORY);

	@Disabled("Disabled because it's already been unpacked")
	@Test
	void testUnpackQuickstart() throws Exception {
		assertFalse(Files.exists(AEM_FILES_LOC.resolve("crx-quickstart")));
		AemProcess aemProcess = new AemProcess.UninitializedAemInstance(AEM_FILES_LOC.resolve(SAMPLE_AEM_QUICKSTART_PATH), AEM_JAVA_VERSION).unpackQuickstart(TAILER_FACTORY);
		assertTrue(Files.exists(AEM_FILES_LOC.resolve("crx-quickstart")));
		assertTrue(Files.exists(AEM_FILES_LOC.resolve(SystemUtils.IS_OS_WINDOWS ? "runStart.bat" : "runStart")));
		assertTrue(Files.exists(AEM_FILES_LOC.resolve(SystemUtils.IS_OS_WINDOWS ? "runStop.bat" : "runStop")));
		assertNotNull(aemProcess);
		//
	}

	@Disabled("Disabled because it's already been initialized")
	@Test
	void testStartQuickstartUnitializeAem() throws Exception {
		String result = underTest.startQuickstartInitializeAem();
		assertThat(result, containsString("StartupListener.startupFinished called"));
	}
	
	@Disabled("Disabled because service pack has already been installed")
	@Test
	// Copy the service pack file into the /install directory before running this test.
	void testStartQuickstartInstallServicePacks() throws Exception {
		String result = underTest.startQuickstartInstallServicePack();
		assertThat(result, containsString("Installed successfully"));
	}

	@Disabled("Disabled because forms add-on has already been installed")
	@Test
	// Copy the forms add-on file into the /install directory before running this test.
	void testStartQuickstartInstallFormsAddOn() throws Exception {
		String result = underTest.startQuickstartInstallFormsAddOn();
		assertThat(result, containsString("Installed BMC XMLFormService of type BMC_NATIVE"));
	}

	@Disabled("Disabled because it's already been initialized")
	@Test
	void testStartQuickstartRunAction() throws Exception {
		String result = underTest.startQuickstartPerformAction(()->{});
		assertThat(result, containsString("StartupListener.startupFinished called"));
	}
	// This class tests the RegEx patterns used to match the AEM log entries using samples of actual log entries.
	// Currently the regxes are relatively simple, but if they grow in complexity, this will ensure that they are backwards compatible.
	public static class RegExTests {
		
		@ParameterizedTest
		@ValueSource(strings = { 
				"15.06.2025 09:37:45.478 *INFO* [FelixDispatchQueue] com.adobe.granite.workflow.core.launcher.WorkflowLauncherListener StartupListener.startupFinished called" // AEM 6.5 SP19-23 and 6.5 LTS
				})
		void testAemStartTargetPattern(String testString) {
			assertThat(testString, matchesRegex(AemProcess.AEM_START_TARGET_REGEX));
		}

		@ParameterizedTest
		@ValueSource(strings = {
				"15.06.2025 09:41:34.731 *INFO* [FelixStartLevel] org.apache.sling.installer.core.impl.OsgiInstallerImpl Apache Sling OSGi Installer Service stopped." // AEM 6.5 SP19-23 and 6.5 LTS
				})
		void testAemStopTargetPattern(String testString) {
			assertThat(testString, matchesRegex(AemProcess.AEM_STOP_TARGET_REGEX));
		}

		@ParameterizedTest
		@ValueSource(strings = { 
				"15.06.2025 09:32:49.896 *INFO* [Thread-5112] com.adobe.granite.installer.Updater Content Package AEM-6.5-Service-Pack-23 Installed successfully" // AEM 6.5 SP19-23
				})
		void testAemServicePackStartTargetPattern(String testString) {
			assertThat(testString, matchesRegex(AemProcess.AEM_SP_START_TARGET_REGEX));
		}

		@ParameterizedTest
		@ValueSource(strings = { 
				"15.06.2025 09:40:52.777 *INFO* [OsgiInstallerImpl] org.apache.sling.audit.osgi.installer Installed BMC XMLFormService of type BMC_NATIVE" // AEM 6.5 SP19-23
				})
		void testAemFormsAddOnStartTargetPattern(String testString) {
			assertThat(testString, matchesRegex(AemProcess.AEM_FORMS_ADD_ON_START_TARGET_REGEX));
		}


	}
}
