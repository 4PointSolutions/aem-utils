package com._4point.aem.aem_utils.aem_cntrl.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com._4point.aem.aem_utils.aem_cntrl.domain.Mocks.TailerMocker;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.WaitForLog;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer.TailerFactory;

// Note: This test is sociable, meaning it interacts with the filesystem and other components.
// This means it takes a little longer to run than a typical unit test.
// Also, since it generates a (mostly) complete Spring Context, it generates a spurious
// error message, which is expected in this case:
//   Missing required subcommand
//   Usage: aem-cntrl [COMMAND]
// 
// Just ignore it.  Despite the error, the Spring Context is created and then the test will run successfully and complete as expected.

@SpringBootTest()
class WaitForLogImpl_SociableTest {

	@MockitoBean TailerFactory tailerFactoryMock;

	@SuppressWarnings("unused")
	private static final List<WaitForLog.RegexArgument> regexArgs = 
			List.of(WaitForLog.RegexArgument.startup(),
					WaitForLog.RegexArgument.shutdown(), 
					WaitForLog.RegexArgument.custom(Pattern.compile(".*Installed BMC XMLFormService of type BMC_NATIVE.*")));
	
	@ParameterizedTest
	@FieldSource("regexArgs")
	void testWaitForLog(WaitForLog.RegexArgument regexArg, @Autowired WaitForLog waitForLog, @TempDir Path tempDir) throws Exception {
		TailerMocker tailerMocker = new TailerMocker(tailerFactoryMock);
		// Given
		// Setup tailer mock response
		tailerMocker.programMocksToEmulateAem(); // Change to `tailerMocker.programMocks(Stream.empty(), Duration.ZERO);` to cause test to fail.
		// create the AEM directory structure in the temp directory
		Path adobeDir = AemFilesTest.createMockAemDir(tempDir);
		// When
		waitForLog.waitForLog(regexArg, Duration.ofMinutes(10), WaitForLog.FromOption.START, adobeDir);
	}

}
