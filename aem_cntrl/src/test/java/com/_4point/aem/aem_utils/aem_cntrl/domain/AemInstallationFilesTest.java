package com._4point.aem.aem_utils.aem_cntrl.domain;

import static org.junit.jupiter.api.Assertions.*;
import static com._4point.aem.aem_utils.aem_cntrl.domain.MockInstallFiles.*;
import static org.hamcrest.MatcherAssert.assertThat; 
import static org.hamcrest.Matchers.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.FieldSource;

import com._4point.aem.aem_utils.aem_cntrl.domain.AemInstallationFiles.AemFileset;
import com._4point.aem.aem_utils.aem_cntrl.domain.AemInstallationFiles.AemQuickstart.AemBaseRelease;
import com._4point.aem.aem_utils.aem_cntrl.domain.AemInstallationFiles.AemVersion;
import com._4point.aem.aem_utils.aem_cntrl.domain.AemInstallationFiles.SlingProperties;
import com._4point.testing.matchers.javalang.ExceptionMatchers;

class AemInstallationFilesTest {
	private static final Path EXPECTED_AEM_INSTALL_LOC = Path.of("AEM_65_SP19");
	private static final AemVersion EXPECTED_AEM_VERSION = new AemVersion(6, 5, 19, AemBaseRelease.AEM65_ORIG.aemJavaVersion);

	@SuppressWarnings("unused")
	private static final List<MockInstallFiles> AEM_BASE_VERSION_PATHS = List.of(SAMPLE_AEM_ORIG_QUICKSTART_PATH, SAMPLE_AEM_LTS_QUICKSTART_PATH);

	@Test
	void testCreateAemDir(@TempDir Path rootDir) {
		Path expectedResult = rootDir.resolve(EXPECTED_AEM_INSTALL_LOC);
		Path result = AemInstallationFiles.aemDir(rootDir, EXPECTED_AEM_VERSION);
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
		List<Path> result1 = AemInstallationFiles.AemServicePack.findFiles(rootDir);
		
		SAMPLE_AEM_ORIG_QUICKSTART_PATH.createMockFile(rootDir);
		Path testFilePath = SAMPLE_AEM_SERVICE_PACK_PATH.createMockFile(rootDir);
		SAMPLE_AEM_FORMS_ADDON_PATH.createMockFile(rootDir);
		
		List<Path> result2 = AemInstallationFiles.AemServicePack.findFiles(rootDir);
		
		assertAll(
				()->assertThat("First result should be empty", result1, is(empty())),
				()->assertThat("Should find one file", result2, hasSize(1)),
				()->assertThat("Should match expected result", result2.getFirst(), equalTo(testFilePath))
				);
	}

	@ParameterizedTest
	@FieldSource("AEM_BASE_VERSION_PATHS")
	void testFindAemQuickstarts(MockInstallFiles quickstart, @TempDir Path rootDir) throws Exception {
		List<Path> result1 = AemInstallationFiles.AemQuickstart.findFiles(rootDir);
		
		Path testFilePath = quickstart.createMockFile(rootDir);
		SAMPLE_AEM_SERVICE_PACK_PATH.createMockFile(rootDir);
		SAMPLE_AEM_FORMS_ADDON_PATH.createMockFile(rootDir);
		
		List<Path> result2 = AemInstallationFiles.AemQuickstart.findFiles(rootDir);
		
		assertAll(
				()->assertThat("First result should be empty", result1, is(empty())),
				()->assertThat("Should find one file", result2, hasSize(1)),
				()->assertThat("Should match expected result", result2.getFirst(), equalTo(testFilePath))
				);
	}

	@Test
	void testFindAemFormsAddOns(@TempDir Path rootDir) throws Exception {
		List<Path> result1 = AemInstallationFiles.AemFormsAddOn.findFiles(rootDir);
		
		SAMPLE_AEM_ORIG_QUICKSTART_PATH.createMockFile(rootDir);
		SAMPLE_AEM_SERVICE_PACK_PATH.createMockFile(rootDir);
		Path testFilePath = SAMPLE_AEM_FORMS_ADDON_PATH.createMockFile(rootDir);
		
		List<Path> result2 = AemInstallationFiles.AemFormsAddOn.findFiles(rootDir);
		
		assertAll(
				()->assertThat("First result should be empty", result1, is(empty())),
				()->assertThat("Should find one file", result2, hasSize(1)),
				()->assertThat("Should match expected result", result2.getFirst(), equalTo(testFilePath))
				);
	}

	@Test
	void testFindAemLicenseProperties(@TempDir Path rootDir) throws Exception {
		Optional<Path> result1 = AemInstallationFiles.LicenseProperties.findFile(rootDir);
		
		SAMPLE_AEM_ORIG_QUICKSTART_PATH.createMockFile(rootDir);
		SAMPLE_AEM_SERVICE_PACK_PATH.createMockFile(rootDir);
		SAMPLE_AEM_FORMS_ADDON_PATH.createMockFile(rootDir);
		Path testFilePath = SAMPLE_LICENSE_PROPERTIES_PATH.createMockFile(rootDir);
		
		Optional<Path> result2 = AemInstallationFiles.LicenseProperties.findFile(rootDir);
		
		assertAll(
				()->assertThat("First result should be empty", result1.isEmpty(), is(true)),
				()->assertThat("Should find one file", result2.isPresent(), is(true)),
				()->assertThat("Should match expected result", result2.orElseThrow(), equalTo(testFilePath))
				);
	}

	@Test
	void testThereCanBeOnlyOne_Pass_OnlyOne(@TempDir Path rootDir) throws Exception {
		String description = "Forms Add On";
		SAMPLE_AEM_ORIG_QUICKSTART_PATH.createMockFile(rootDir);
		SAMPLE_AEM_SERVICE_PACK_PATH.createMockFile(rootDir);
		Path testFilePath = SAMPLE_AEM_FORMS_ADDON_PATH.createMockFile(rootDir);

		assertEquals(testFilePath, AemInstallationFiles.thereCanBeOnlyOne(AemInstallationFiles.AemFormsAddOn.findFiles(rootDir), description));
	}

	@Test
	void testThereCanBeOnlyOne_Fail_Zero(@TempDir Path rootDir) throws Exception {
		String description = "Forms Add On";
		IllegalStateException ex = assertThrows(IllegalStateException.class, ()->AemInstallationFiles.thereCanBeOnlyOne(AemInstallationFiles.AemFormsAddOn.findFiles(rootDir), description));
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll(description, "Found no", "should be one"));
	}

	@Test
	void testThereCanBeOnlyOne_Fail_MoreThanOne(@TempDir Path rootDir) throws Exception {
		String description = "Forms Add On";
		SAMPLE_AEM_FORMS_ADDON_PATH.createMockFile(rootDir);
		SECOND_AEM_FOMRS_ADDON_PATH.createMockFile(rootDir);
		
		IllegalStateException ex = assertThrows(IllegalStateException.class, ()->AemInstallationFiles.thereCanBeOnlyOne(AemInstallationFiles.AemFormsAddOn.findFiles(rootDir), description));
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll(description, "Found multiple", "should only be one"));
	}

	@Test
	void testLocateAemFiles(@TempDir Path rootDir) throws Exception {
		Path testQuickstartFilePath = SAMPLE_AEM_ORIG_QUICKSTART_PATH.createMockFile(rootDir);
		Path testServicePackFilePath = SAMPLE_AEM_SERVICE_PACK_PATH.createMockFile(rootDir);
		Path testFormsAddonFilePath = SAMPLE_AEM_FORMS_ADDON_PATH.createMockFile(rootDir);
		
		AemFileset result = AemInstallationFiles.locateAemFiles(rootDir);
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
		Path testQuickstartFilePath = SAMPLE_AEM_ORIG_QUICKSTART_PATH.createMockFile(rootDir);
		// Omitted: Path testServicePackFilePath = SAMPLE_AEM_SERVICE_PACK_PATH.createTestFile(rootDir);
		Path testFormsAddonFilePath = SAMPLE_AEM_FORMS_ADDON_PATH.createMockFile(rootDir);
		
		AemFileset result = AemInstallationFiles.locateAemFiles(rootDir);
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
		AemInstallationFiles.AemQuickstart.VersionInfo versionInfo = AemInstallationFiles.AemQuickstart.versionInfo(filename);
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
		AemInstallationFiles.AemServicePack.VersionInfo versionInfo = AemInstallationFiles.AemServicePack.versionInfo(filename);
		
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
		AemInstallationFiles.AemFormsAddOn.VersionInfo versionInfo = AemInstallationFiles.AemFormsAddOn.versionInfo(filename);
		
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
		AemInstallationFiles.AemFormsAddOn.VersionInfo versionInfo = AemInstallationFiles.AemFormsAddOn.versionInfo(filename);
		
		assertAll(
				()->assertEquals(expectedMajorVersion,versionInfo.majorVersion()),
				()->assertEquals(expectedMinorVersion,versionInfo.minorVersion()),
				()->assertEquals(expectedBuildNum,versionInfo.buildNum())
				);
	}
	
	@Test
	void testUpdateSlingProperties(@TempDir Path tempDir) throws Exception {
		// Given
		MockAemFiles.SLING_PROPERTIES.createMockFile(tempDir);
		
		String expectedAdditionalProperties = 
				"""
				sling.bootdelegation.class.com.rsa.jsafe.provider.JsafeJCE=com.rsa.*
				""";
		
		// When 
		SlingProperties.under(tempDir).orElseThrow().updateSlingProperties();;

		// Then 
		List<String> expectedResult = stringToListOfLines(MockAemFiles.SLING_PROPERTIES.contents() + expectedAdditionalProperties);
		List<String> actualResult = Files.readAllLines(tempDir.resolve(MockAemFiles.SLING_PROPERTIES.filename()));
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
