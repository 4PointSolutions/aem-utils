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

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

import com._4point.aem.aem_utils.aem_cntrl.domain.FluentFormsFiles.FluentFormsFileset;
import com._4point.testing.matchers.javalang.ExceptionMatchers;

class FluentFormsFilesTest {
	private static final List<Path> SAMPLE_FLUENTFORMS_CORE_PATHS = List.of(Path.of("fluentforms.core-0.0.3-SNAPSHOT.jar"), Path.of("fluentforms.core-0.0.3.jar"));
	private static final List<Path> SAMPLE_FLUENTFORMS_RESTSERVICES_PATHS = List.of(Path.of("rest-services.server-0.0.3-SNAPSHOT.jar"), Path.of("rest-services.server-0.0.3.jar"));
	private static final List<Path> SAMPLE_EXTRA_CORE_PATHS = List.of(Path.of("fluentforms.core-1.2.3-SNAPSHOT.jar"), Path.of("fluentforms.core-1.2.3.jar"));
	private static final List<Path> SAMPLE_EXTRA_RESTSERVICES_PATHS = List.of(Path.of("rest-services.server-1.2.3-SNAPSHOT.jar"),Path.of("rest-services.server-1.2.3.jar"));
	@SuppressWarnings("unused")
	private static final List<SamplePaths> SAMPLE_FLUENTFORMS_PATHS_PRODUCT = productOf(SAMPLE_FLUENTFORMS_CORE_PATHS, SAMPLE_FLUENTFORMS_RESTSERVICES_PATHS); 
	@SuppressWarnings("unused")
	private static final List<SamplePathsWithExtra> SAMPLE_FLUENTFORMS_PATHS_PRODUCT_WITH_EXTRA_CORE_PATHS = productWithExtra(SAMPLE_FLUENTFORMS_CORE_PATHS, SAMPLE_FLUENTFORMS_RESTSERVICES_PATHS, SAMPLE_EXTRA_CORE_PATHS);
	@SuppressWarnings("unused")
	private static final List<SamplePathsWithExtra> SAMPLE_FLUENTFORMS_PATHS_PRODUCT_WITH_EXTRA_RESTSERVICES_PATHS = productWithExtra(SAMPLE_FLUENTFORMS_CORE_PATHS, SAMPLE_FLUENTFORMS_RESTSERVICES_PATHS, SAMPLE_EXTRA_RESTSERVICES_PATHS);
	
	@ParameterizedTest
	@FieldSource("SAMPLE_FLUENTFORMS_CORE_PATHS")
	void testLocateFluentFormsCoreFiles(Path sampleFluentFormsCorePath, @TempDir Path rootDir) throws Exception {
		List<Path> result1 = FluentFormsFiles.FluentFormsCore.findFiles(rootDir);
		
		Path testFilePath = createTestFile(rootDir, sampleFluentFormsCorePath);
		createTestFile(rootDir, SAMPLE_FLUENTFORMS_RESTSERVICES_PATHS.getFirst());
		
		List<Path> result2 = FluentFormsFiles.FluentFormsCore.findFiles(rootDir);
		
		assertAll(
				()->assertThat("First result should be empty", result1, is(empty())),
				()->assertThat("Should find one file", result2, hasSize(1)),
				()->assertThat("Should match expected result", result2.getFirst(), equalTo(testFilePath))
				);
	}

	@ParameterizedTest
	@FieldSource("SAMPLE_FLUENTFORMS_RESTSERVICES_PATHS")
	void testLocateFluentFormsRestServicesFiles(Path sampleFluentFormsRestServicesPath, @TempDir Path rootDir) throws Exception {
		List<Path> result1 = FluentFormsFiles.RestServices.findFiles(rootDir);
		
		createTestFile(rootDir, SAMPLE_FLUENTFORMS_CORE_PATHS.getFirst());
		Path testFilePath = createTestFile(rootDir, sampleFluentFormsRestServicesPath);
		
		List<Path> result2 = FluentFormsFiles.RestServices.findFiles(rootDir);
		
		assertAll(
				()->assertThat("First result should be empty", result1, is(empty())),
				()->assertThat("Should find one file", result2, hasSize(1)),
				()->assertThat("Should match expected result", result2.getFirst(), equalTo(testFilePath))
				);
	}

	
	@ParameterizedTest
	@FieldSource("SAMPLE_FLUENTFORMS_PATHS_PRODUCT")
	void testLocateFluentFormsFiles(SamplePaths samplePaths, @TempDir Path rootDir) throws Exception {
		Path testCoreFilePath = createTestFile(rootDir, samplePaths.sampleFluentFormsCorePath());
		Path testRestServicesFilePath = createTestFile(rootDir, samplePaths.sampleFluentFormsRestServicesPath());
		
		FluentFormsFileset result = FluentFormsFiles.locateFluentFormsFiles(rootDir);
		
		assertAll(
				()->assertEquals(testCoreFilePath, result.fluentFormsCore().orElseThrow()),
				()->assertEquals(testRestServicesFilePath, result.restServices().orElseThrow())
				);
	}

	@ParameterizedTest
	@FieldSource("SAMPLE_FLUENTFORMS_RESTSERVICES_PATHS")
	void testLocateFluentFormsFiles_Fail_NoCore(Path sampleFluentFormsRestServicesPath, @TempDir Path rootDir) throws Exception {
		// OMITTED Path testCoreFilePath = createTestFile(rootDir, SAMPLE_FLUENTFORMS_CORE_PATH);
		Path testRestServicesFilePath = createTestFile(rootDir, sampleFluentFormsRestServicesPath);

		FluentFormsFileset result = FluentFormsFiles.locateFluentFormsFiles(rootDir);
		
		assertAll(
				()->assertTrue(result.fluentFormsCore().isEmpty(), "FluentForms Core should be empty"),
				()->assertEquals(testRestServicesFilePath, result.restServices().orElseThrow())
				);
	}

	@ParameterizedTest
	@FieldSource("SAMPLE_FLUENTFORMS_CORE_PATHS")
	void testLocateFluentFormsFiles_Fail_NoRestServices(Path sampleFluentFormsCorePath, @TempDir Path rootDir) throws Exception {
		Path testCoreFilePath = createTestFile(rootDir, sampleFluentFormsCorePath);
		// OMITTED Path testRestServicesFilePath = createTestFile(rootDir, SAMPLE_FLUENTFORMS_RESTSERVICES_PATH);
		
		FluentFormsFileset result = FluentFormsFiles.locateFluentFormsFiles(rootDir);
		
		assertAll(
				()->assertEquals(testCoreFilePath, result.fluentFormsCore().orElseThrow()),
				()->assertTrue(result.restServices().isEmpty(), "FluentForms REST Services should be empty")
				);
	}

	@ParameterizedTest
	@FieldSource("SAMPLE_FLUENTFORMS_PATHS_PRODUCT_WITH_EXTRA_CORE_PATHS")
	void testThereCanBeOnlyOne_Fail_MoreThanOneCore(SamplePathsWithExtra samplePaths, @TempDir Path rootDir) throws Exception {
		createTestFile(rootDir, samplePaths.extraPath());
		createTestFile(rootDir, samplePaths.sampleFluentFormsCorePath());
		createTestFile(rootDir, samplePaths.sampleFluentFormsRestServicesPath());
		
		IllegalStateException ex = assertThrows(IllegalStateException.class, ()->FluentFormsFiles.locateFluentFormsFiles(rootDir));
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("FluentForms Core Library", "Found multiple", "should at most be one"));
	}

	@ParameterizedTest
	@FieldSource("SAMPLE_FLUENTFORMS_PATHS_PRODUCT_WITH_EXTRA_RESTSERVICES_PATHS")
	void testThereCanBeOnlyOne_Fail_MoreThanOneRestServices(SamplePathsWithExtra samplePaths, @TempDir Path rootDir) throws Exception {
		createTestFile(rootDir, samplePaths.extraPath());
		createTestFile(rootDir, samplePaths.sampleFluentFormsCorePath());
		createTestFile(rootDir, samplePaths.sampleFluentFormsRestServicesPath());
		
		IllegalStateException ex = assertThrows(IllegalStateException.class, ()->FluentFormsFiles.locateFluentFormsFiles(rootDir));
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("FluentForms REST Services Library", "Found multiple", "should at most be one"));
	}

	private Path createTestFile(Path rootDir, Path filePath) throws IOException {
		Path testFilePath = rootDir.resolve(filePath);
		Files.createFile(testFilePath);
		return testFilePath;
	}
	
	private record SamplePaths(Path sampleFluentFormsCorePath, Path sampleFluentFormsRestServicesPath) {}
	
	private static List<SamplePaths> productOf(List<Path> corePaths, List<Path> restServicePaths) {
		return corePaths.stream()
			  	 		.flatMap(corePath->restServicePaths.stream()
			  			 								   .map(restPath->new SamplePaths(corePath, restPath)))
			  	 		.toList();
	}
	
	private record SamplePathsWithExtra(Path sampleFluentFormsCorePath, Path sampleFluentFormsRestServicesPath, Path extraPath) {}

	private static List<SamplePathsWithExtra> productWithExtra(List<Path> corePaths, List<Path> restServicePaths, List<Path> extraPaths) {
		return corePaths.stream()
			  	 		.flatMap(corePath->restServicePaths.stream()
			  			 								   .flatMap(restPath->extraPaths.stream()
			  			 										   						.map(extraPath->new SamplePathsWithExtra(corePath, restPath, extraPath))))	
			  	 		.toList();
	}
	
}
