package com._4point.aem.aem_utils.aem_cntrl.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.SlingProperties;

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

}
