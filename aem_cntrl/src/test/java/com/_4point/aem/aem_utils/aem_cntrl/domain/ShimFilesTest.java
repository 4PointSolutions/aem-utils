package com._4point.aem.aem_utils.aem_cntrl.domain;

import static com._4point.aem.aem_utils.aem_cntrl.domain.MockInstallFiles.*;
import static com._4point.testing.matchers.javalang.ExceptionMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat; 
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com._4point.aem.aem_utils.aem_cntrl.domain.AemProcess.AemProcessException;
import com._4point.aem.aem_utils.aem_cntrl.domain.ShimFiles.CreateType;
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
	
	enum ExistingFilesScenario {
		BOTH_EXIST {
			@Override
			void setup(Path tempDir) throws IOException {
				Files.createFile(tempDir.resolve(OperatingSystem.getOs().runStart()));
				Files.createFile(tempDir.resolve(OperatingSystem.getOs().runStop()));
			}

			@Override
			void testOriginalFilesUntouched(Path tempDir) throws IOException {
				assertAll(
						()->assertTrue(Files.exists(tempDir.resolve(OperatingSystem.getOs().runStart())), "runStart file should exist"),
						()->assertEquals(Files.size(tempDir.resolve(OperatingSystem.getOs().runStart())), 0L, "runStart file should still be 0 bytes"),
						()->assertTrue(Files.exists(tempDir.resolve(OperatingSystem.getOs().runStop())), "runStop file should exist"),
						()->assertEquals(Files.size(tempDir.resolve(OperatingSystem.getOs().runStop())), 0L, "runStop file should still be 0 bytes")
						);
			}

			@Override
			String expectedExceptionMessage(Path tempDir) {
				return "both files already exist";
			}
		}, 
		NEITHER_EXIST {
			@Override
			void setup(Path tempDir) throws IOException {
				assertAll(
						()->assertFalse(Files.exists(tempDir.resolve(OperatingSystem.getOs().runStart())), "runStart file should not exist"),
						()->assertFalse(Files.exists(tempDir.resolve(OperatingSystem.getOs().runStop())), "runStop file should not exist")
						);
			}

			@Override
			void testOriginalFilesUntouched(Path tempDir) throws IOException {
				assertAll(
						()->assertFalse(Files.exists(tempDir.resolve(OperatingSystem.getOs().runStart())), "runStart file should not exist"),
						()->assertFalse(Files.exists(tempDir.resolve(OperatingSystem.getOs().runStop())), "runStop file should not exist")
						);
			}

			@Override
			String expectedExceptionMessage(Path tempDir) {
				return "neither file exists";
			}
		}, 
		ONLY_START_EXISTS {
			@Override
			void setup(Path tempDir) throws IOException {
				Files.createFile(tempDir.resolve(OperatingSystem.getOs().runStart()));
				assertFalse(Files.exists(tempDir.resolve(OperatingSystem.getOs().runStop())), "runStop file should not exist");
			}

			@Override
			void testOriginalFilesUntouched(Path tempDir) throws IOException {
				assertAll(
						()->assertTrue(Files.exists(tempDir.resolve(OperatingSystem.getOs().runStart())), "runStart file should exist"),
						()->assertEquals(Files.size(tempDir.resolve(OperatingSystem.getOs().runStart())), 0L, "runStart file should still be 0 bytes"),
						()->assertFalse(Files.exists(tempDir.resolve(OperatingSystem.getOs().runStop())), "runStop file should not exist")
						);
			}

			@Override
			String expectedExceptionMessage(Path tempDir) {
				return tempDir.resolve(OperatingSystem.getOs().runStart()).toString();
			}
		}, 
		ONLY_STOP_EXISTS {
			@Override
			void setup(Path tempDir) throws IOException {
				assertFalse(Files.exists(tempDir.resolve(OperatingSystem.getOs().runStart())), "runStart file should not exist");
				Files.createFile(tempDir.resolve(OperatingSystem.getOs().runStop()));
			}

			@Override
			void testOriginalFilesUntouched(Path tempDir) throws IOException {
				assertAll(
						()->assertFalse(Files.exists(tempDir.resolve(OperatingSystem.getOs().runStart())), "runStart file should not exist"),
						()->assertTrue(Files.exists(tempDir.resolve(OperatingSystem.getOs().runStop())), "runStop file should exist"),
						()->assertEquals(Files.size(tempDir.resolve(OperatingSystem.getOs().runStop())), 0L, "runStop file should still be 0 bytes")
						);
					}

			@Override
			String expectedExceptionMessage(Path tempDir) {
				return tempDir.resolve(OperatingSystem.getOs().runStart()).toString();
			}
		};
		
		abstract void setup(Path tempDir) throws IOException;
		abstract void testOriginalFilesUntouched(Path tempDir) throws IOException;
		abstract String expectedExceptionMessage(Path tempDir);
	}
	
	@ParameterizedTest
	@CsvSource(textBlock = """
			NEW, NEITHER_EXIST, true
			NEW, BOTH_EXIST, false
			NEW, ONLY_START_EXISTS, false
			NEW, ONLY_STOP_EXISTS, false
			EXISTING, BOTH_EXIST, true
			EXISTING, NEITHER_EXIST, false
			EXISTING, ONLY_START_EXISTS, false
			EXISTING, ONLY_STOP_EXISTS, false
			NEW_OR_EXISTING, NEITHER_EXIST, true
			NEW_OR_EXISTING, BOTH_EXIST, true
			NEW_OR_EXISTING, ONLY_START_EXISTS, true
			NEW_OR_EXISTING, ONLY_STOP_EXISTS, true
			""")
	void testCreateBatFiles_ExpectSuccess(CreateType createType, ExistingFilesScenario existingFilesScenario, boolean expectSuccess, @TempDir Path tempDir) throws Exception {
		existingFilesScenario.setup(tempDir);
		ShimFiles underTest = new ShimFiles(JavaVersion.VERSION_11, tempDir, processRunnerMock);
		if (expectSuccess) {
			when(processRunnerMock.runtoListResult(any(), any())).thenReturn(CompletableFuture
					.completedFuture(new ProcessRunner.ListResult(0, JBANG_ENV_RESPONSE.lines().toList(), null)));
			underTest.createBatFiles(createType);
			assertAll(
					()->verify(processRunnerMock).runtoListResult(eq(jbangCommand("jdk", "java-env", "11")), eq(tempDir)),
					()->assertThat(Files.readString(tempDir.resolve(OperatingSystem.getOs().runStart())), containsString("Copy & paste the above commands in your CMD window")),
					()->assertThat(Files.readString(tempDir.resolve(OperatingSystem.getOs().runStop())), containsString("Copy & paste the above commands in your CMD window"))
					);
		} else {
			var e = assertThrows(AemProcessException.class, () -> underTest.createBatFiles(createType));
			switch (createType) {
				case NEW -> assertThat(e, hasCauseMatching(
						             		allOf(
						             				instanceOf(FileAlreadyExistsException.class), 
													exceptionMsgContainsAll(existingFilesScenario.expectedExceptionMessage(tempDir))
													)
						             		));
				case EXISTING -> assertThat(e, hasCauseMatching(
													allOf(
															instanceOf(FileNotFoundException.class),
															exceptionMsgContainsAll(existingFilesScenario.expectedExceptionMessage(tempDir))
															)
												));
				case NEW_OR_EXISTING -> fail("This case should never happen because expectSuccess should be true for this case.");
			}
			existingFilesScenario.testOriginalFilesUntouched(tempDir);
		}
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
