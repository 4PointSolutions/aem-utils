package com._4point.aem.aem_utils.aem_cntrl.domain;

import static com._4point.aem.aem_utils.aem_cntrl.domain.MockInstallFiles.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com._4point.aem.aem_utils.aem_cntrl.domain.ports.ipi.ProcessRunner;

@ExtendWith(MockitoExtension.class)
class ShimFilesTest {
	private static final String JBANG_ENV_RESPONSE = """
			set PATH=C:\\Users\\MyUser\\.jbang\\cache\\jdks\\11\\bin;%PATH%
			set JAVA_HOME=C:\\Users\\MyUser\\.jbang\\cache\\jdks\\11
			rem Copy & paste the above commands in your CMD window or add
			rem them to your Environment Variables in the System Settings.
			""";

	@Mock ProcessRunner processRunnerMock;
	
	@Test
	void testCreateBatFiles(@TempDir Path tempDir) {
		ShimFiles underTest = new ShimFiles(JavaVersion.VERSION_11, tempDir, processRunnerMock);
		when(processRunnerMock.runtoListResult(any(), any()))
		  		.thenReturn(CompletableFuture.completedFuture(new ProcessRunner.ListResult(0, JBANG_ENV_RESPONSE.lines().toList(), null)));
		
		underTest.createBatFiles();
		
		verify(processRunnerMock).runtoListResult(eq(jbangCommand("jdk", "java-env", "11")), eq(tempDir));
		assertTrue(Files.exists(tempDir.resolve(OperatingSystem.getOs().runStart())), "runStart file should exist");
		assertTrue(Files.exists(tempDir.resolve(OperatingSystem.getOs().runStop())), "runStop file should exist");
	}

	@Test
	void testStartAem(@TempDir Path tempDir) throws Exception {
		ShimFiles underTest = new ShimFiles(JavaVersion.VERSION_17, tempDir, processRunnerMock);

		underTest.startAem();

		verify(processRunnerMock).runtoListResult(eq(new String[] { tempDir.resolve(SCAFFOLD_START_PATH.filename()).toString() }), eq(tempDir));
	}

	@Test
	void testStopAem(@TempDir Path tempDir) throws Exception {
		ShimFiles underTest = new ShimFiles(JavaVersion.VERSION_17, tempDir, processRunnerMock);

		underTest.stopAem();

		verify(processRunnerMock).runtoListResult(eq(new String[] { tempDir.resolve(SCAFFOLD_STOP_PATH.filename()).toString() }), eq(tempDir));
	}

	private static String[] jbangCommand(String... args) {
		return Stream.concat((OS.WINDOWS.isCurrentOs() ? Stream.of("CMD.exe", "/C", "jbang") : Stream.of("jbang")), Stream.of(args))
					 .map(arg->arg.contains(" ") ? "\"" + arg + "\"" : arg)
					 .toArray(String[]::new);
	}	
}
