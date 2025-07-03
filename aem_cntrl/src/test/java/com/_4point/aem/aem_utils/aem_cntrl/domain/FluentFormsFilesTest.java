package com._4point.aem.aem_utils.aem_cntrl.domain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com._4point.aem.aem_utils.aem_cntrl.domain.FluentFormsFiles.FluentFormsFileset;
import com._4point.testing.matchers.javalang.ExceptionMatchers;

class FluentFormsFilesTest {
	private static final Path SAMPLE_FLUENTFORMS_CORE_PATH = Path.of("fluentforms.core-0.0.3-SNAPSHOT.jar");
	private static final Path SAMPLE_FLUENTFORMS_RESTSERVICES_PATH = Path.of("rest-services.server-0.0.3-SNAPSHOT.jar");

	@Test
	void testLocateFluentFormsCoreFiles(@TempDir Path rootDir) throws Exception {
		List<Path> result1 = FluentFormsFiles.FluentFormsCore.findFiles(rootDir);
		
		Path testFilePath = createTestFile(rootDir, SAMPLE_FLUENTFORMS_CORE_PATH);
		createTestFile(rootDir, SAMPLE_FLUENTFORMS_RESTSERVICES_PATH);
		
		List<Path> result2 = FluentFormsFiles.FluentFormsCore.findFiles(rootDir);
		
		assertAll(
				()->assertThat("First result should be empty", result1, is(empty())),
				()->assertThat("Should find one file", result2, hasSize(1)),
				()->assertThat("Should match expected result", result2.getFirst(), equalTo(testFilePath))
				);
	}

	@Test
	void testLocateFluentFormsRestServicesFiles(@TempDir Path rootDir) throws Exception {
		List<Path> result1 = FluentFormsFiles.RestServices.findFiles(rootDir);
		
		createTestFile(rootDir, SAMPLE_FLUENTFORMS_CORE_PATH);
		Path testFilePath = createTestFile(rootDir, SAMPLE_FLUENTFORMS_RESTSERVICES_PATH);
		
		List<Path> result2 = FluentFormsFiles.RestServices.findFiles(rootDir);
		
		assertAll(
				()->assertThat("First result should be empty", result1, is(empty())),
				()->assertThat("Should find one file", result2, hasSize(1)),
				()->assertThat("Should match expected result", result2.getFirst(), equalTo(testFilePath))
				);
	}

	@Test
	void testLocateFluentFormsFiles(@TempDir Path rootDir) throws Exception {
		Path testCoreFilePath = createTestFile(rootDir, SAMPLE_FLUENTFORMS_CORE_PATH);
		Path testRestServicesFilePath = createTestFile(rootDir, SAMPLE_FLUENTFORMS_RESTSERVICES_PATH);
		
		FluentFormsFileset result = FluentFormsFiles.locateFluentFormsFiles(rootDir);
		
		assertAll(
				()->assertEquals(testCoreFilePath, result.fluentFormsCore().orElseThrow()),
				()->assertEquals(testRestServicesFilePath, result.restServices().orElseThrow())
				);
	}

	@Test
	void testLocateFluentFormsFiles_Fail_NoCore(@TempDir Path rootDir) throws Exception {
		// OMITTED Path testCoreFilePath = createTestFile(rootDir, SAMPLE_FLUENTFORMS_CORE_PATH);
		Path testRestServicesFilePath = createTestFile(rootDir, SAMPLE_FLUENTFORMS_RESTSERVICES_PATH);

		FluentFormsFileset result = FluentFormsFiles.locateFluentFormsFiles(rootDir);
		
		assertAll(
				()->assertTrue(result.fluentFormsCore().isEmpty(), "FluentForms Core should be empty"),
				()->assertEquals(testRestServicesFilePath, result.restServices().orElseThrow())
				);
	}

	@Test
	void testLocateFluentFormsFiles_Fail_NoRestServices(@TempDir Path rootDir) throws Exception {
		Path testCoreFilePath = createTestFile(rootDir, SAMPLE_FLUENTFORMS_CORE_PATH);
		// OMITTED Path testRestServicesFilePath = createTestFile(rootDir, SAMPLE_FLUENTFORMS_RESTSERVICES_PATH);
		
		FluentFormsFileset result = FluentFormsFiles.locateFluentFormsFiles(rootDir);
		
		assertAll(
				()->assertEquals(testCoreFilePath, result.fluentFormsCore().orElseThrow()),
				()->assertTrue(result.restServices().isEmpty(), "FluentForms REST Services should be empty")
				);
	}

	@Test
	void testThereCanBeOnlyOne_Fail_MoreThanOneCore(@TempDir Path rootDir) throws Exception {
		createTestFile(rootDir, Path.of("fluentforms.core-1.2.3-SNAPSHOT.jar"));
		createTestFile(rootDir, SAMPLE_FLUENTFORMS_CORE_PATH);
		createTestFile(rootDir, SAMPLE_FLUENTFORMS_RESTSERVICES_PATH);
		
		IllegalStateException ex = assertThrows(IllegalStateException.class, ()->FluentFormsFiles.locateFluentFormsFiles(rootDir));
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("FluentForms Core Library", "Found multiple", "should at most be one"));
	}

	@Test
	void testThereCanBeOnlyOne_Fail_MoreThanOneRestServices(@TempDir Path rootDir) throws Exception {
		createTestFile(rootDir, Path.of("rest-services.server-1.2.3-SNAPSHOT.jar"));
		createTestFile(rootDir, SAMPLE_FLUENTFORMS_CORE_PATH);
		createTestFile(rootDir, SAMPLE_FLUENTFORMS_RESTSERVICES_PATH);
		
		IllegalStateException ex = assertThrows(IllegalStateException.class, ()->FluentFormsFiles.locateFluentFormsFiles(rootDir));
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("FluentForms REST Services Library", "Found multiple", "should at most be one"));
	}

	private Path createTestFile(Path rootDir, Path filePath) throws IOException {
		Path testFilePath = rootDir.resolve(filePath);
		Files.createFile(testFilePath);
		return testFilePath;
	}
}
