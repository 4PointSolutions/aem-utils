package com._4point.aem.aem_utils.aem_cntrl.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com._4point.aem.aem_utils.aem_cntrl.domain.WaitForLogImpl.AemDirType;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.WaitForLog.RegexArgument;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer.TailerFactory;

@ExtendWith(MockitoExtension.class)
class WaitForLogImplTest {
	
	// This mock AEN log has all the strings that are ever searched for
	private static final String MOCK_AEM_LOG = """
			18.06.2025 09:26:04.989 *INFO* [FelixDispatchQueue] com.adobe.granite.workflow.core.launcher.WorkflowLauncherListener StartupListener.startupFinished called
			18.06.2025 09:28:04.288 *INFO* [FelixFrameworkWiring] org.apache.sling.installer.core.impl.OsgiInstallerImpl Apache Sling OSGi Installer Service stopped.
			18.06.2025 09:32:42.198 *INFO* [Thread-2944] com.adobe.granite.installer.Updater Content Package AEM-6.5-Service-Pack-23 Installed successfully
			18.06.2025 09:38:01.557 *INFO* [OsgiInstallerImpl] org.apache.sling.audit.osgi.installer Installed BMC XMLFormService of type BMC_NATIVE
			""";

	@Mock TailerFactory tailerFactoryMock;
	@Mock Tailer tailerMock;

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
		WaitForLogImpl waitForLog = new WaitForLogImpl(() -> aemDir, tailerFactoryMock);
		when(tailerFactoryMock.fromEnd(any())).thenReturn(tailerMock);
		when(tailerMock.stream()).thenAnswer(i->MOCK_AEM_LOG.lines());

		// When
		assertThrows(UnsupportedOperationException.class, () -> {
			waitForLog.waitForLog(RegexArgument.startup(), Duration.ofMinutes(10), null, null);
		});
	}
	
	private Path createMockAemDir(Path tempDir) throws IOException {
		Path adobeDir = tempDir.resolve("adobe");
		Path crxQuickstartDir = adobeDir.resolve("aem").resolve("crx-quickstart");
		Files.createDirectories(crxQuickstartDir);
		return adobeDir;
	}
}
