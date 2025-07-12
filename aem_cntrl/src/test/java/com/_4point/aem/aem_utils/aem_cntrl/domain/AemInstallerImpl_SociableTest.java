package com._4point.aem.aem_utils.aem_cntrl.domain;

import static com._4point.aem.aem_utils.aem_cntrl.domain.MockInstallFiles.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.RestClient;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.RestClient.ContentType;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.AemInstaller;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.ipi.ProcessRunner;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer.TailerFactory;

@SpringBootTest()
class AemInstallerImpl_SociableTest {

	private static final String JBANG_ENV_RESPONSE = """
			set PATH=C:\\Users\\MyUser\\.jbang\\cache\\jdks\\11\\bin;%PATH%
			set JAVA_HOME=C:\\Users\\MyUser\\.jbang\\cache\\jdks\\11
			rem Copy & paste the above commands in your CMD window or add
			rem them to your Environment Variables in the System Settings.
			""";

	// This mock AEN log has all the strings that are ever searched for
	private static final String MOCK_AEM_LOG = """
			18.06.2025 09:26:04.989 *INFO* [FelixDispatchQueue] com.adobe.granite.workflow.core.launcher.WorkflowLauncherListener StartupListener.startupFinished called
			18.06.2025 09:28:04.288 *INFO* [FelixFrameworkWiring] org.apache.sling.installer.core.impl.OsgiInstallerImpl Apache Sling OSGi Installer Service stopped.
			18.06.2025 09:32:42.198 *INFO* [Thread-2944] com.adobe.granite.installer.Updater Content Package AEM-6.5-Service-Pack-23 Installed successfully
			18.06.2025 09:38:01.557 *INFO* [OsgiInstallerImpl] org.apache.sling.audit.osgi.installer Installed BMC XMLFormService of type BMC_NATIVE
			""";
	
	
	@MockitoBean ProcessRunner processRunnerMock;
	@MockitoBean(answers = Answers.RETURNS_DEEP_STUBS) RestClient restClientMock;
	@MockitoBean TailerFactory tailerFactoryMock;
	@Mock Tailer tailerMock;
	@Mock RestClient.Response restClientGetResponseMock;
	@Mock RestClient.Response restClientPostResponseMock;
	@Mock RestClient.MultipartPayload.Builder restClientPayloadBuilderMock;
	@Mock RestClient.MultipartPayload restClientPayloadMock;
	
	@Test
	void test(@Autowired AemInstaller underTest, @TempDir Path tempDir) throws Exception {
		AemInstallType aemInstallType = AemInstallType.AEM_ORIG;
		Path srcDir = createDirectory(tempDir, "srcDir");
		// create fake installation files
		aemInstallType.createMockFiles(srcDir);
		// Create DestDir
		Path destDir = createDirectory(tempDir, "destDir");
		// Mock other services
		when(processRunnerMock.runtoListResult(any(), any()))
							  .thenReturn(CompletableFuture.completedFuture(new ProcessRunner.ListResult(0, JBANG_ENV_RESPONSE.lines().toList(), null)));
		when(tailerFactoryMock.fromEnd(any()))
							  .thenReturn(tailerMock);
		when(tailerMock.stream())
		   			   .thenAnswer(i->mockAemInstallation(destDir.resolve(aemInstallType.aemDir()).resolve("crx-quickstart"))) // Mock the Aem installation on the first call
					   .thenAnswer(i->MOCK_AEM_LOG.lines());
		
		when(restClientMock.getRequestBuilder(anyString())
						   .queryParam("post", "true")
						   .queryParam("ts", "170")
						   .build()
						   .getFromServer(ContentType.APPLICATION_JSON)).thenReturn(Optional.of(restClientGetResponseMock));
		when(restClientGetResponseMock.data()).thenReturn(new ByteArrayInputStream(AemConfigSettingsTest.TEST_JSON1.getBytes()));

		when(restClientMock.multipartPayloadBuilder(anyString())).thenReturn(restClientPayloadBuilderMock);
		when(restClientPayloadBuilderMock.add(anyString(), anyString())).thenReturn(restClientPayloadBuilderMock);
		when(restClientPayloadBuilderMock.build()).thenReturn(restClientPayloadMock);
		when(restClientPayloadMock.postToServer(ContentType.TEXT_HTML)).thenReturn(Optional.of(restClientPostResponseMock));				   
						   
		when(restClientPostResponseMock.data()).thenReturn(new ByteArrayInputStream("Mock HTML".getBytes()));
				
		
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
		verify(processRunnerMock).runUntilCompletes(
				eq(jbangCommand("run", "--java=11", "--java-options=-Xmx2g", "-Djava.awt.headless=true", "AEM_6.5_Quickstart.jar", "-unpack")),
				eq(aemDir),
				eq(Duration.ofMinutes(3))
				);
		//      Get the Java environment settings so that we can create the scaffolding files (runStart, runStop)
		verify(processRunnerMock).runtoListResult(eq(jbangCommand("jdk", "java-env", Integer.toString(aemInstallType.javaVersion()))), eq(aemDir));
		//      Start and stop AEM 4 times:
		//        1) Initial install of AEM
		//		  2) Install Service Pack
		//		  3) Install Forms Add-on
		//		  4) Install FluentForms and update AEM Configuration
		verify(processRunnerMock, times(4)).runtoListResult(eq(new String[] { aemDir.resolve(SCAFFOLD_START_PATH.filename()).toString() }), eq(aemDir)); // 4 times
		verify(processRunnerMock, times(4)).runtoListResult(eq(new String[] { aemDir.resolve(SCAFFOLD_STOP_PATH.filename()).toString() }), eq(aemDir)); // 4 times
		
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
	
	private Stream<String> mockAemInstallation(Path aemQuickstartDir) {
		// When we're invoked for the initial install, AEM creates a bunch of files 
		// The only one we update during the AEM installation is the sling.properties file.
		// So this creates that file so that the AEM installation can proceed. 
		try {
			MockAemFiles.SLING_PROPERTIES.createMockFile(aemQuickstartDir);
			return MOCK_AEM_LOG.lines();
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
