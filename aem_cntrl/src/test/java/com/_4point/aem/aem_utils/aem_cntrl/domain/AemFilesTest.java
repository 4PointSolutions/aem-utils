package com._4point.aem.aem_utils.aem_cntrl.domain;

import static org.hamcrest.MatcherAssert.assertThat; 
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.AemDir;
import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.AemDir.AemDirType;
import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.LogFile;
import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.LogFile.FromOption;
import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.SlingProperties;
import com._4point.aem.aem_utils.aem_cntrl.domain.Mocks.TailerMocker;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.WaitForLog.WaitForLogException;
import com._4point.testing.matchers.javalang.ExceptionMatchers;

class AemFilesTest {

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

	@Test
	void testMonitorLogFile_FindsLine() {
		// Given
		TailerMocker tailerMocker = new TailerMocker();
		tailerMocker.programMocksToEmulateAem();
		LogFile underTest = LogFile.under(Path.of("aemDir"), tailerMocker.tailerFactoryMock());
		// When
		Optional<String> result = underTest.monitorLogFile(Pattern.compile(".*"), Duration.ofSeconds(1), FromOption.END);
		assertTrue(result.isPresent(), "Expected a result, but got none");
	}

	@Test
	void testMonitorLogFile_DoesNotFindLine() {
		// Given
		TailerMocker tailerMocker = new TailerMocker();
		tailerMocker.programMocksToEmulateAem();
		LogFile underTest = LogFile.under(Path.of("aemDir"), tailerMocker.tailerFactoryMock());
		// When
		Optional<String> result = underTest.monitorLogFile(Pattern.compile("Some Text that is not in the log file."), Duration.ofSeconds(1), FromOption.END);
		assertFalse(result.isPresent(), "Expected no result, but got one");
	}

	@Test
	void testMonitorLogFile_TimesOut() {
		// Given
		TailerMocker tailerMocker = new TailerMocker();
		tailerMocker.programMocksToEmulateAemAfterWait(Duration.ofSeconds(2)); // Wait is longer than the timeput below
		LogFile underTest = LogFile.under(Path.of("aemDir"), tailerMocker.tailerFactoryMock());
		// When
		Optional<String> result = underTest.monitorLogFile(Pattern.compile(".*"), Duration.ofSeconds(1), FromOption.END);
		assertFalse(result.isPresent(), "Expected no result, but got one");
	}
	
	@ParameterizedTest
	@CsvSource({ "/Adobe, ABSOLUTE", "./Adobe, RELATIVE", "../Adobe, RELATIVE", "Adobe, DEFAULT", ",NULL" })
	void testAemDirType_of(Path aemDir, AemDirType expectedType) {
		assertEquals(expectedType, AemDirType.of(aemDir));
	}

	@EnabledOnOs(OS.WINDOWS)
	@Test
	void testAemDirType_of_windows_withDriveLetter() {
		Path aemDir = Path.of("C:\\Adobe");
		assertEquals(AemDirType.ABSOLUTE, AemDirType.of(aemDir));
	}

	@ParameterizedTest
	@ValueSource(strings = "aem")
	@NullSource
	void testToQualifiedPath_NullOrDefault(Path aemPath, @TempDir Path tempDir) throws Exception {
		Path adobeDir = createMockAemDir(tempDir);
		
		AemDir underTest = new AemDir(()->adobeDir);
		// Given
		Path result = underTest.toQualified(aemPath);
		
		assertEquals(tempDir.resolve("adobe").resolve("aem"), result, "Expected the path to be qualified with the AEM directory");
	}

	@Test
	void testToQualifiedPath_AbsoluteParent(@TempDir Path tempDir) throws Exception {
		Path adobeDir = createMockAemDir(tempDir);
		
		AemDir underTest = new AemDir(()->adobeDir);
		// Given
		Path result = underTest.toQualified(adobeDir.toAbsolutePath());
		
		assertEquals(tempDir.resolve("adobe").resolve("aem"), result, "Expected the path to be qualified with the AEM directory");
	}

	@Test
	void testToQualifiedPath_AbsoluteChild(@TempDir Path tempDir) throws Exception {
		Path adobeDir = createMockAemDir(tempDir);
		
		AemDir underTest = new AemDir(()->adobeDir);
		// Given
		Path result = underTest.toQualified(adobeDir.resolve("aem").toAbsolutePath());
		
		assertEquals(tempDir.resolve("adobe").resolve("aem"), result, "Expected the path to be qualified with the AEM directory");
	}
	
	@Test
	void testToQualifiedPath_TooManyDirs(@TempDir Path tempDir) throws Exception {
		Path adobeDir = createMockAemDir(tempDir);
		Files.createDirectories(tempDir.resolve("adobe").resolve("aem_extra").resolve("crx-quickstart"));
		
		AemDir underTest = new AemDir(()->adobeDir);
		// Given
		WaitForLogException ex = assertThrows(WaitForLogException.class, () ->underTest.toQualified(null));

		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("Too many AEM directories found" , adobeDir.toString()));
	}
	
	@Test
	void testToQualifiedPath_NoDirs(@TempDir Path tempDir) throws Exception {
		Path adobeDir = tempDir.resolve("adobe");
		Files.createDirectories(adobeDir.resolve("aem"));
		
		AemDir underTest = new AemDir(()->adobeDir);
		// Given
		WaitForLogException ex = assertThrows(WaitForLogException.class, () ->underTest.toQualified(adobeDir.resolve("aem").toAbsolutePath()));

		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("No AEM directory found" , adobeDir.toString()));
	}
	
	private Path createMockAemDir(Path rootDir) throws IOException {
		Path adobeDir = rootDir.resolve("adobe");
		Path crxQuickstartDir = adobeDir.resolve("aem").resolve("crx-quickstart");
		Files.createDirectories(crxQuickstartDir);
		return adobeDir;
	}

}
