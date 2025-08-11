package com._4point.aem.aem_utils.aem_cntrl.domain;

import static com._4point.testing.matchers.javalang.ExceptionMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.AemDir.AemDirException;
import com._4point.aem.aem_utils.aem_cntrl.domain.ShimFiles.ShimFilesException;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.Shim;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.Shim.Operation;

// Note: This test is sociable, meaning it interacts with the filesystem and other components.
// This means it takes a little longer to run than a typical unit test.
// Also, since it generates a (mostly) complete Spring Context, it generates a spurious
// error message, which is expected in this case:
// Missing required subcommand
// Usage: aem-cntrl [COMMAND]
//
// Just ignore it.  Despite the error, the Spring Context is created and then the test will run successfully and complete as expected.

@SpringBootTest()
class ShimImpl_SociableTest {

	@Nested
	@ParameterizedClass
	@ValueSource(strings = {"cq-quickstart-6.6.0.jar", "AEM_6.5_Quickstart.jar"})
	class ShimFilesSuccessTest {

		private final Path quickstartJarFilename;
		
		public ShimFilesSuccessTest(Path quickstartJarFilename) {
			this.quickstartJarFilename = quickstartJarFilename;
		}

		@ParameterizedTest
		@CsvSource({ 
			"ADD, false", 
			"UPDATE, true", 
			"ADD_UPDATE, false", 
			"ADD_UPDATE, true" 
			})
		void test_ShimImplSuccess(Operation shimOperation, boolean createFiles, @Autowired Shim shim, @TempDir Path tempDir) throws Exception{
			createMockAemDir(tempDir, quickstartJarFilename);

			if (createFiles) {
				// Create existing shim files
				Files.writeString(tempDir.resolve(OperatingSystem.getOs().runStart()), "Old Start File");
				Files.writeString(tempDir.resolve(OperatingSystem.getOs().runStop()), "Old Stop File");
			}
			
			shim.shim(shimOperation, tempDir);
			
			assertAll(
					()->assertThat(Files.readString(tempDir.resolve(OperatingSystem.getOs().runStart())), containsString("Copy & paste the above commands in your CMD window")),
					()->assertThat(Files.readString(tempDir.resolve(OperatingSystem.getOs().runStop())), containsString("Copy & paste the above commands in your CMD window"))
					);
		}

	}

	private static void createMockAemDir(Path tempDir) throws IOException {
		createMockAemDir(tempDir, Path.of("cq-quickstart-6.6.0.jar"));
	}
	
	private static void createMockAemDir(Path tempDir, Path quickstartJarFilename) throws IOException {
		// Create faux AEM 6.5 LTS Quickstart Jar file and directory
		Files.createDirectory(tempDir.resolve("crx-quickstart"));
		Files.createFile(tempDir.resolve(quickstartJarFilename));
	}
	
	@Test
	void test_ShimImplFailure_NoAEMDir(@Autowired Shim shim, @TempDir Path tempDir) {
		// Given
		// No AEM directory structure created in tempDir
		// When
		AemDirException e = assertThrows(AemDirException.class, ()->shim.shim(Operation.ADD_UPDATE, tempDir));

		// Then
		assertThat(e, exceptionMsgContainsAll("No AEM directory found", tempDir.toString()));
	}
	
	@Test
	void test_ShimImplFailure_ShimFilesAlreadyExist(@Autowired Shim shim, @TempDir Path tempDir) throws Exception {
		// Given
		createMockAemDir(tempDir);

		// Create existing shim files
		Files.writeString(tempDir.resolve(OperatingSystem.getOs().runStart()), "Old Start File");
		Files.writeString(tempDir.resolve(OperatingSystem.getOs().runStop()), "Old Stop File");

		// When
		ShimFilesException e = assertThrows(ShimFilesException.class, ()->shim.shim(Operation.ADD, tempDir));

		e.printStackTrace();
		// Then
		assertThat(e, allOf(exceptionMsgContainsAll("Error while writing start/stop bat files", tempDir.toString()),
				hasCauseMatching(instanceOf(FileAlreadyExistsException.class), exceptionMsgContainsAll("Cannot create start/stop bat files", "both files already exist", tempDir.toString()))));
	}

}
