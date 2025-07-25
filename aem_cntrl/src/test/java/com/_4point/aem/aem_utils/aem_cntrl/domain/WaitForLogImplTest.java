package com._4point.aem.aem_utils.aem_cntrl.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com._4point.aem.aem_utils.aem_cntrl.domain.Mocks.TailerMocker;
import com._4point.aem.aem_utils.aem_cntrl.domain.WaitForLogImpl.AemDirType;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.WaitForLog.FromOption;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.WaitForLog.RegexArgument;

@ExtendWith(MockitoExtension.class)
class WaitForLogImplTest {
	
    TailerMocker tailerMocker = new TailerMocker();
    
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
		tailerMocker.programMocksToEmulateAem();;
		Path aemDir = createMockAemDir(tempDir);
		WaitForLogImpl waitForLog = new WaitForLogImpl(() -> aemDir, tailerMocker.tailerFactoryMock());
		// When
		waitForLog.waitForLog(RegexArgument.startup(), Duration.ofMinutes(10), FromOption.START, null);
	}

	
	// Test the three RegexArgument types (startup, shutdown, custom)
	// Test for a timeout
	// Test for FormStart and FromEnd
	// Test for null aemDir, or provided aemDir
	
	private Path createMockAemDir(Path tempDir) throws IOException {
		Path adobeDir = tempDir.resolve("adobe");
		Path crxQuickstartDir = adobeDir.resolve("aem").resolve("crx-quickstart");
		Files.createDirectories(crxQuickstartDir);
		return adobeDir;
	}
}
