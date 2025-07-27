package com._4point.aem.aem_utils.aem_cntrl.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.AemDir;
import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.LogFile;
import com._4point.aem.aem_utils.aem_cntrl.domain.Mocks.TailerMocker;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.WaitForLog.FromOption;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.WaitForLog.RegexArgument;

@ExtendWith(MockitoExtension.class)
class WaitForLogImplTest {
	private static final Pattern CUSTOM_PATTERN = Pattern.compile(".*");
	
	@Mock AemDir aemDirMock;
	@Mock LogFile logFileMock;
	
    private final TailerMocker tailerMocker = new TailerMocker();
    
	// This is more of a Sociable test than a unit test
//	@Test
//	void testWaitForLog(@TempDir Path tempDir) throws Exception {
//		// Given
//		tailerMocker.programMocksToEmulateAem();;
//		Path aemDir = createMockAemDir(tempDir);
//		WaitForLogImpl waitForLog = new WaitForLogImpl(() -> aemDir, tailerMocker.tailerFactoryMock());
//		// When
//		waitForLog.waitForLog(RegexArgument.startup(), Duration.ofMinutes(10), FromOption.START, null);
//	}

	
	// Test the three RegexArgument types (startup, shutdown, custom)
	enum WaitForLogRegExArgumentTestScenario {
		startup(RegexArgument.startup(), AemProcess.AEM_START_TARGET_PATTERN), 
		shutdown(RegexArgument.shutdown(), AemProcess.AEM_STOP_TARGET_PATTERN), 
		custom(RegexArgument.custom(CUSTOM_PATTERN), CUSTOM_PATTERN),
		;
		
		
		private final RegexArgument regexArgument;
		private final Pattern expectedPattern;
		private WaitForLogRegExArgumentTestScenario(RegexArgument regexArgument, Pattern expectedPattern) {
			this.regexArgument = regexArgument;
			this.expectedPattern = expectedPattern;
		}
		public RegexArgument regexArgument() {
			return regexArgument;
		}
		public Pattern expectedPattern() {
			return expectedPattern;
		}
	}
	
	@ParameterizedTest
	@EnumSource
	void testWaitForLogWithRegexArgument(WaitForLogRegExArgumentTestScenario scenario) throws Exception {
		// Given
		Path aemDir = Path.of("aemTest");
		WaitForLogImpl waitForLog = new WaitForLogImpl(aemDirMock, tailerMocker.tailerFactoryMock(), (dir, tailerFactory) -> logFileMock);
		when(logFileMock.monitorLogFile(any(Pattern.class), any(Duration.class), any(LogFile.FromOption.class)))
				.thenReturn(java.util.Optional.of("Mock log line matching: " + scenario.expectedPattern().pattern()));

		// When
		waitForLog.waitForLog(scenario.regexArgument(), Duration.ofMinutes(1), FromOption.START, null);

		// Then
		verify(logFileMock).monitorLogFile(scenario.expectedPattern(), Duration.ofMinutes(1), LogFile.FromOption.START);
	}
	
	// Test for FormStart and FromEnd
	// Test for null aemDir, or provided aemDir
	// Test for log line not found
	// Test for a timeout
}
