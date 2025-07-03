package com._4point.aem.aem_utils.aem_cntrl.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.assertThat; 
import static org.hamcrest.Matchers.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.FieldSource;

import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.AemFileset;
import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.AemQuickstart.AemBaseRelease;
import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.AemVersion;
import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.SlingProperties;
import com._4point.testing.matchers.javalang.ExceptionMatchers;

class AemFilesTest {
	private static final Path ExPECTED_AEM_INSTALL_LOC = Path.of("AEM_65_SP19");
	private static final Path SAMPLE_AEM_ORIG_QUICKSTART_PATH = Path.of("AEM_6.5_Quickstart.jar");
	private static final Path SAMPLE_AEM_LTS_QUICKSTART_PATH = Path.of("cq-quickstart-6.6.0.jar");
	private static final Path SAMPLE_AEM_SERVICE_PACK_PATH = Path.of("aem-service-pkg-6.5.19.0.zip");
	private static final Path SAMPLE_AEM_FORMS_ADDON_PATH = Path.of("adobe-aemfd-" + getAbbrev() + "-pkg-6.0.1120.zip");
	private static final Path SAMPLE_LICENSE_PROPERTIES_PATH = Path.of("license.properties");
	private static final AemVersion EXPECTED_AEM_VERSION = new AemVersion(6, 5, 19, AemBaseRelease.AEM65_ORIG.aemJavaVersion);
	@SuppressWarnings("unused")
	private static final List<Path> AEM_BASE_VERSION_PATHS = List.of(SAMPLE_AEM_ORIG_QUICKSTART_PATH, SAMPLE_AEM_LTS_QUICKSTART_PATH);

	@Test
	void testCreateAemDir(@TempDir Path rootDir) {
		Path expectedResult = rootDir.resolve(ExPECTED_AEM_INSTALL_LOC);
		Path result = AemFiles.aemDir(rootDir, EXPECTED_AEM_VERSION);
		assertAll(
				()->assertEquals(expectedResult, result)
				);
	}

	@Test
	void testAemVersion() {
		assertAll(
				()->assertEquals("6.5", EXPECTED_AEM_VERSION.aemVersion()),
				()->assertEquals("6.5_SP19", EXPECTED_AEM_VERSION.aemVersionWithSp())
				);
	}

	@Test
	void testFindAemServicePacks(@TempDir Path rootDir) throws Exception {
		List<Path> result1 = AemFiles.AemServicePack.findFiles(rootDir);
		
		createTestFile(rootDir, SAMPLE_AEM_ORIG_QUICKSTART_PATH);
		Path testFilePath = createTestFile(rootDir, SAMPLE_AEM_SERVICE_PACK_PATH);
		createTestFile(rootDir, SAMPLE_AEM_FORMS_ADDON_PATH);
		
		List<Path> result2 = AemFiles.AemServicePack.findFiles(rootDir);
		
		assertAll(
				()->assertThat("First result should be empty", result1, is(empty())),
				()->assertThat("Should find one file", result2, hasSize(1)),
				()->assertThat("Should match expected result", result2.getFirst(), equalTo(testFilePath))
				);
	}

	private Path createTestFile(Path rootDir, Path filePath) throws IOException {
		Path testFilePath = rootDir.resolve(filePath);
		Files.createFile(testFilePath);
		return testFilePath;
	}

	@ParameterizedTest
	@FieldSource("AEM_BASE_VERSION_PATHS")
	void testFindAemQuickstarts(Path quickstartPath, @TempDir Path rootDir) throws Exception {
		List<Path> result1 = AemFiles.AemQuickstart.findFiles(rootDir);
		
		Path testFilePath = createTestFile(rootDir, quickstartPath);
		createTestFile(rootDir, SAMPLE_AEM_SERVICE_PACK_PATH);
		createTestFile(rootDir, SAMPLE_AEM_FORMS_ADDON_PATH);
		
		List<Path> result2 = AemFiles.AemQuickstart.findFiles(rootDir);
		
		assertAll(
				()->assertThat("First result should be empty", result1, is(empty())),
				()->assertThat("Should find one file", result2, hasSize(1)),
				()->assertThat("Should match expected result", result2.getFirst(), equalTo(testFilePath))
				);
	}

	@Test
	void testFindAemFormsAddOns(@TempDir Path rootDir) throws Exception {
		List<Path> result1 = AemFiles.AemFormsAddOn.findFiles(rootDir);
		
		createTestFile(rootDir, SAMPLE_AEM_ORIG_QUICKSTART_PATH);
		createTestFile(rootDir, SAMPLE_AEM_SERVICE_PACK_PATH);
		Path testFilePath = createTestFile(rootDir, SAMPLE_AEM_FORMS_ADDON_PATH);
		
		List<Path> result2 = AemFiles.AemFormsAddOn.findFiles(rootDir);
		
		assertAll(
				()->assertThat("First result should be empty", result1, is(empty())),
				()->assertThat("Should find one file", result2, hasSize(1)),
				()->assertThat("Should match expected result", result2.getFirst(), equalTo(testFilePath))
				);
	}

	@Test
	void testFindAemLicenseProperties(@TempDir Path rootDir) throws Exception {
		Optional<Path> result1 = AemFiles.LicenseProperties.findFile(rootDir);
		
		createTestFile(rootDir, SAMPLE_AEM_ORIG_QUICKSTART_PATH);
		createTestFile(rootDir, SAMPLE_AEM_SERVICE_PACK_PATH);
		createTestFile(rootDir, SAMPLE_AEM_FORMS_ADDON_PATH);
		Path testFilePath = createTestFile(rootDir, SAMPLE_LICENSE_PROPERTIES_PATH);
		
		Optional<Path> result2 = AemFiles.LicenseProperties.findFile(rootDir);
		
		assertAll(
				()->assertThat("First result should be empty", result1.isEmpty(), is(true)),
				()->assertThat("Should find one file", result2.isPresent(), is(true)),
				()->assertThat("Should match expected result", result2.orElseThrow(), equalTo(testFilePath))
				);
	}

	@Test
	void testThereCanBeOnlyOne_Pass_OnlyOne(@TempDir Path rootDir) throws Exception {
		String description = "Forms Add On";
		createTestFile(rootDir, SAMPLE_AEM_ORIG_QUICKSTART_PATH);
		createTestFile(rootDir, SAMPLE_AEM_SERVICE_PACK_PATH);
		Path testFilePath = createTestFile(rootDir, SAMPLE_AEM_FORMS_ADDON_PATH);

		assertEquals(testFilePath, AemFiles.thereCanBeOnlyOne(AemFiles.AemFormsAddOn.findFiles(rootDir), description));
	}

	@Test
	void testThereCanBeOnlyOne_Fail_Zero(@TempDir Path rootDir) throws Exception {
		String description = "Forms Add On";
		IllegalStateException ex = assertThrows(IllegalStateException.class, ()->AemFiles.thereCanBeOnlyOne(AemFiles.AemFormsAddOn.findFiles(rootDir), description));
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll(description, "Found no", "should be one"));
	}

	@Test
	void testThereCanBeOnlyOne_Fail_MoreThanOne(@TempDir Path rootDir) throws Exception {
		String description = "Forms Add On";
		createTestFile(rootDir, Path.of("adobe-aemfd-" + getAbbrev() + "-pkg-6.0.9999.zip"));
		createTestFile(rootDir, SAMPLE_AEM_FORMS_ADDON_PATH);
		
		IllegalStateException ex = assertThrows(IllegalStateException.class, ()->AemFiles.thereCanBeOnlyOne(AemFiles.AemFormsAddOn.findFiles(rootDir), description));
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll(description, "Found multiple", "should only be one"));
	}

	@Test
	void testLocateAemFiles(@TempDir Path rootDir) throws Exception {
		Path testQuickstartFilePath = createTestFile(rootDir, SAMPLE_AEM_ORIG_QUICKSTART_PATH);
		Path testServicePackFilePath = createTestFile(rootDir, SAMPLE_AEM_SERVICE_PACK_PATH);
		Path testFormsAddonFilePath = createTestFile(rootDir, SAMPLE_AEM_FORMS_ADDON_PATH);
		
		AemFileset result = AemFiles.locateAemFiles(rootDir);
		AemVersion aemVersion = result.aemVersion();
		
		assertAll(
				()->assertEquals(testQuickstartFilePath, result.quickstart()),
				()->assertEquals(testServicePackFilePath, result.servicePack().get()),
				()->assertEquals(testFormsAddonFilePath, result.formsAddOn()),
				()->assertEquals("6.5_SP19", aemVersion.aemVersionWithSp())
				);
	}
	
	@Test
	void testLocateAemFiles_NoServicePack(@TempDir Path rootDir) throws Exception {
		Path testQuickstartFilePath = createTestFile(rootDir, SAMPLE_AEM_ORIG_QUICKSTART_PATH);
		// Omitted: Path testServicePackFilePath = createTestFile(rootDir, SAMPLE_AEM_SERVICE_PACK_PATH);
		Path testFormsAddonFilePath = createTestFile(rootDir, SAMPLE_AEM_FORMS_ADDON_PATH);
		
		AemFileset result = AemFiles.locateAemFiles(rootDir);
		AemVersion aemVersion = result.aemVersion();
		
		assertAll(
				()->assertEquals(testQuickstartFilePath, result.quickstart()),
				()->assertTrue(result.servicePack().isEmpty(), "Expected no service pack to be present, but found one."),
				()->assertEquals(testFormsAddonFilePath, result.formsAddOn()),
				()->assertEquals("6.5_SP0", aemVersion.aemVersionWithSp())
				);
	}
	
	@ParameterizedTest
	@CsvSource(textBlock = 
			"""
			AEM_6.5_Quickstart.jar,6,5,AEM65_ORIG
			AEM_7.4_Quickstart.jar,7,4,AEM65_ORIG
			cq-quickstart-6.6.0.jar,6,6,AEM65_LTS
			""")
	void testAemQuickstartVersionInfo(Path filename, int expectedMajorVersion, int expectedMinorVersion, AemBaseRelease expectedAemBaseRelease) {
		AemFiles.AemQuickstart.VersionInfo versionInfo = AemFiles.AemQuickstart.versionInfo(filename);
		assertAll(
				()->assertEquals(expectedMajorVersion,versionInfo.majorVersion()),
				()->assertEquals(expectedMinorVersion,versionInfo.minorVersion()),
				()->assertEquals(expectedAemBaseRelease, versionInfo.aemRelease())
				);
	}

	@ParameterizedTest
	@CsvSource(textBlock = 
			"""
			aem-service-pkg-6.5.19.0.zip,6,5,19,0
			aem-service-pkg-7.4.1.57.zip,7,4,1,57
			""")
	void testAemServicePackVersionInfo(Path filename, int expectedMajorVersion, int expectedMinorVersion, int expectedServicePack, int expectedPatch) {
		AemFiles.AemServicePack.VersionInfo versionInfo = AemFiles.AemServicePack.versionInfo(filename);
		
		assertAll(
				()->assertEquals(expectedMajorVersion,versionInfo.majorVersion()),
				()->assertEquals(expectedMinorVersion,versionInfo.minorVersion()),
				()->assertEquals(expectedServicePack,versionInfo.servicePack()),
				()->assertEquals(expectedPatch,versionInfo.patch())
				);
	}

	@EnabledOnOs(OS.WINDOWS)
	@ParameterizedTest
	@CsvSource(textBlock = 
			"""
			adobe-aemfd-win-pkg-6.0.1120.zip,6,0,1120
			adobe-aemfd-win-pkg-7.4.12345.zip,7,4,12345
			""")
	void testAemFormsAddonVersionInfo_Windows(Path filename, int expectedMajorVersion, int expectedMinorVersion, int expectedBuildNum) {
		AemFiles.AemFormsAddOn.VersionInfo versionInfo = AemFiles.AemFormsAddOn.versionInfo(filename);
		
		assertAll(
				()->assertEquals(expectedMajorVersion,versionInfo.majorVersion()),
				()->assertEquals(expectedMinorVersion,versionInfo.minorVersion()),
				()->assertEquals(expectedBuildNum,versionInfo.buildNum())
				);
	}
	
	@EnabledOnOs(OS.LINUX)
	@ParameterizedTest
	@CsvSource(textBlock = 
			"""
			adobe-aemfd-linux-pkg-6.0.1120.zip,6,0,1120
			adobe-aemfd-linux-pkg-7.4.12345.zip,7,4,12345
			""")
	void testAemFormsAddonVersionInfo_Linux(Path filename, int expectedMajorVersion, int expectedMinorVersion, int expectedBuildNum) {
		AemFiles.AemFormsAddOn.VersionInfo versionInfo = AemFiles.AemFormsAddOn.versionInfo(filename);
		
		assertAll(
				()->assertEquals(expectedMajorVersion,versionInfo.majorVersion()),
				()->assertEquals(expectedMinorVersion,versionInfo.minorVersion()),
				()->assertEquals(expectedBuildNum,versionInfo.buildNum())
				);
	}
	
	private static String getAbbrev() {
		if (SystemUtils.IS_OS_WINDOWS) {
			return "win";
		} else if (SystemUtils.IS_OS_LINUX) {
			return "linux";
		} else if (SystemUtils.IS_OS_MAC) {
			return "macos";
		} else {
			throw new IllegalStateException("Unsupported Operating System (%s)".formatted(SystemUtils.OS_NAME));
		}

	}
	
	@Test
	void testUpdateSlingProperties(@TempDir Path tempDir) throws Exception {
		// Given
		String sampleSlingProperties = 
				"""
				#Overlay properties for configuration
				#Wed Apr 02 09:40:35 EDT 2025
				sling.bootdelegation.sun=sun.*,com.sun.*
				org.osgi.framework.system.capabilities.extra=${org.apache.sling.launcher.system.capabilities.extra}
				sling.framework.install.startlevel=1
				""";
		Path configDir = tempDir.resolve("conf");
		Files.createDirectory(configDir);
		Path slingPropertiesPath = configDir.resolve("sling.properties");
		Files.writeString(slingPropertiesPath, sampleSlingProperties, StandardOpenOption.CREATE_NEW);

		String expectedAdditionalProperties = 
				"""
				sling.bootdelegation.class.com.rsa.jsafe.provider.JsafeJCE=com.rsa.*
				""";
		
		// When 
		SlingProperties.under(tempDir).orElseThrow().updateSlingProperties();;

		// Then 
		List<String> expectedResult = stringToListOfLines(sampleSlingProperties + expectedAdditionalProperties);
		List<String> actualResult = Files.readAllLines(slingPropertiesPath);
		assertEquals(expectedResult, actualResult);
	}

	@Test
	void testUpdateSlingProperties_SlingPropertiesDoesNotExist(@TempDir Path tempDir) throws Exception {
		// When 
		assertTrue(SlingProperties.under(tempDir).isEmpty());
	}

	private static List<String> stringToListOfLines(String s) throws IOException {
		try(StringReader sr = new StringReader(s); BufferedReader br = new BufferedReader(sr)) {
			return br.lines().toList();
		}
	}
}
