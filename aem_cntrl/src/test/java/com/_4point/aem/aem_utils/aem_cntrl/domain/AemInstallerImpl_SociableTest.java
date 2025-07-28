package com._4point.aem.aem_utils.aem_cntrl.domain;

import static com._4point.aem.aem_utils.aem_cntrl.domain.MockInstallFiles.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com._4point.aem.aem_utils.aem_cntrl.domain.Mocks.TailerMocker;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.AemInstaller;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.ipi.ProcessRunner;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.AemConfigManager;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.MobileFormsSettings;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer.TailerFactory;

// Note: This test is sociable, meaning it interacts with the filesystem and other components.
// This means it takes a little longer to run than a typical unit test.
// Also, since it generates a (mostly) complete Spring Context, it generates a spurious
// error message, which is expected in this case:
//    Missing required subcommand
//    Usage: aem-cntrl [COMMAND]
//
// Just ignore it.  Despite the error, the Spring Context is created and then the test will run successfully and complete as expected.

@SpringBootTest()
class AemInstallerImpl_SociableTest {

	private static final String JBANG_ENV_RESPONSE = """
			set PATH=C:\\Users\\MyUser\\.jbang\\cache\\jdks\\11\\bin;%PATH%
			set JAVA_HOME=C:\\Users\\MyUser\\.jbang\\cache\\jdks\\11
			rem Copy & paste the above commands in your CMD window or add
			rem them to your Environment Variables in the System Settings.
			""";
	
	@MockitoBean ProcessRunner processRunnerMock;
	@MockitoBean AemConfigManager aemConfigManagerMock;
	@MockitoBean TailerFactory tailerFactoryMock;
	@Mock Tailer tailerMock;
	@Mock MobileFormsSettings mobileFormsSettingsMock;
	
	@ParameterizedTest
	@EnumSource(AemInstallType.class)
	void test(AemInstallType aemInstallType, @Autowired AemInstaller underTest, @TempDir Path tempDir) throws Exception {
		Path srcDir = createDirectory(tempDir, "srcDir");
		// create fake installation files
		aemInstallType.createMockFiles(srcDir);
		// Create DestDir
		Path destDir = createDirectory(tempDir, "destDir");
		// Mock other services
		when(processRunnerMock.runtoListResult(any(), any()))
							  .thenReturn(CompletableFuture.completedFuture(new ProcessRunner.ListResult(0, JBANG_ENV_RESPONSE.lines().toList(), null)));
		when(tailerFactoryMock.from(any(), any()))
							  .thenReturn(tailerMock);
		when(tailerMock.stream())
		   			   .thenAnswer(i->mockAemInstallation(destDir.resolve(aemInstallType.aemDir()))) // Mock the Aem installation on the first call
					   .thenAnswer(i->TailerMocker.MOCK_AEM_LOG.lines());
		
		// Mock the setting of protected mode for HTML5 forms
		when(aemConfigManagerMock.mobileFormsSettings()).thenReturn(mobileFormsSettingsMock);
		doNothing().when(mobileFormsSettingsMock).protectedMode(true); 
		doNothing().when(aemConfigManagerMock).mobileFormsSettings(mobileFormsSettingsMock); 
		
		// Run test
		underTest.installAem(destDir, srcDir);
		// Verify results
		Path aemDir = destDir.resolve(aemInstallType.aemDir());
		//   Gather all the install files into a stream and then check that they were copied
		var scaffoldFiles = Stream.of(SCAFFOLD_START_PATH, SCAFFOLD_STOP_PATH);
		var fileAssertions = Stream.concat(aemInstallType.files().stream(), scaffoldFiles)
												   .map(f->verifyExists(aemDir, f))
												   .toList();
		assertAll(fileAssertions);

		//   Check that AEM has been run the requisite number of times
		//     Unpack the quickstart jar
		String quickstartFilename = aemInstallType.files().getFirst().filename().toString();
		verify(processRunnerMock).runUntilCompletes(
				eq(jbangCommand("run", "--java=" + aemInstallType.javaVersion(), "--java-options=-Xmx2g", "-Djava.awt.headless=true", quickstartFilename, "-unpack")),
				eq(aemDir),
				eq(Duration.ofMinutes(3))
				);
		//      Get the Java environment settings so that we can create the scaffolding files (runStart, runStop)
		verify(processRunnerMock).runtoListResult(eq(jbangCommand("jdk", "java-env", Integer.toString(aemInstallType.javaVersion()))), eq(aemDir));
		//      Start and stop AEM 3 or 4 times (once for each file in install set):
		//        1) Initial install of AEM
		//		  2) Install Service Pack (maybe)
		//		  3) Install Forms Add-on
		//		  4) Install FluentForms and update AEM Configuration
		int numInstallFiles = aemInstallType.files().size();
		verify(processRunnerMock, times(numInstallFiles)).runtoListResult(eq(new String[] { aemDir.resolve(SCAFFOLD_START_PATH.filename()).toString() }), eq(aemDir)); // 4 times
		verify(processRunnerMock, times(numInstallFiles)).runtoListResult(eq(new String[] { aemDir.resolve(SCAFFOLD_STOP_PATH.filename()).toString() }), eq(aemDir)); // 4 times
		
		//   Verify Sling.properties was modified
		//   Verify that the POST was made to update protected mode
	}

	private static String[] jbangCommand(String... args) {
		return Stream.concat((OS.WINDOWS.isCurrentOs() ? Stream.of("CMD.exe", "/C", "jbang") : Stream.of("jbang")), Stream.of(args))
					 .map(arg->arg.contains(" ") ? "\"" + arg + "\"" : arg)
					 .toArray(String[]::new);
	}
	
	private Executable verifyExists(Path aemDir, MockInstallFiles installFile) {
		Path expectedLocation = aemDir.resolve(installFile.expectedLocation());
		return ()->assertTrue(Files.exists(expectedLocation), "File expected at '" + expectedLocation.toString() + "' but it does not exist.");
	}
	
	private static Stream<String> mockAemInstallation(Path aemQuickstartDir) {
		// When we're invoked for the initial install, AEM creates a bunch of files 
		// The only one we update during the AEM installation is the sling.properties file.
		// So this creates that file so that the AEM installation can proceed. 
		try {
			MockAemFiles.SLING_PROPERTIES.createMockFile(aemQuickstartDir);
			return TailerMocker.MOCK_AEM_LOG.lines();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static Path createDirectory(Path parentDir, String newDirName) throws IOException {
		Path newDir = parentDir.resolve(newDirName);
		Files.createDirectory(newDir);
		return newDir;
	}
}
