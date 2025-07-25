package com._4point.aem.aem_utils.aem_cntrl.domain;

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
import org.junit.jupiter.api.io.TempDir;

import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.LogFile;
import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.LogFile.FromOption;
import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.SlingProperties;
import com._4point.aem.aem_utils.aem_cntrl.domain.Mocks.TailerMocker;

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
}
