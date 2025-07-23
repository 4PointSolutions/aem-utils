package com._4point.aem.aem_utils.aem_cntrl.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com._4point.aem.aem_utils.aem_cntrl.domain.WaitForLogImpl.AemDirType;

class WaitForLogImplTest {

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

	@Test
	void testWaitForLog(@TempDir Path tempDir) throws Exception {
		// Given
		Path aemDir = createMockAemDir(tempDir);
		WaitForLogImpl waitForLog = new WaitForLogImpl(() -> aemDir);

		// When
		assertThrows(UnsupportedOperationException.class, () -> {
			waitForLog.waitForLog(null, null, null, null);
		});
	}
	
	private Path createMockAemDir(Path tempDir) throws IOException {
		Path adobeDir = tempDir.resolve("adobe");
		Path crxQuickstartDir = adobeDir.resolve("aem").resolve("crx-quickstart");
		Files.createDirectories(crxQuickstartDir);
		return adobeDir;
	}
}
