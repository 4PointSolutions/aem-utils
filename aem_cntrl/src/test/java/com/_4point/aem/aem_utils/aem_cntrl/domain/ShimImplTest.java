package com._4point.aem.aem_utils.aem_cntrl.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.Shim;

@ExtendWith(MockitoExtension.class)
class ShimImplTest {

	@Mock AemFiles.AemDir aemDirMock;
	@Mock ShimFiles.RuntimeFactory shimFilesFactoryMock;
	@Mock ShimFiles shimFilesMock;

	@ParameterizedTest
	@CsvSource({
		"ADD, NEW", 
		"UPDATE, EXISTING", 
		"ADD_UPDATE, NEW_OR_EXISTING" 
		})
	void testShim_Operation(Shim.Operation operation, ShimFiles.CreateType expectedCreateType, @TempDir Path tempDir) throws Exception {
		// Given
		// Create faux AEM 6.5 LTS Quickstart Jar file 
		Files.createFile(tempDir.resolve("cq-quickstart-6.6.0.jar"));
		// Initialize Mocks
		when(shimFilesMock.createBatFiles(any(ShimFiles.CreateType.class))).thenReturn(shimFilesMock);
		when(aemDirMock.toQualified(any(Path.class))).thenReturn(tempDir);
		when(shimFilesFactoryMock.apply(any(), any(Path.class))).thenReturn(shimFilesMock);

		final Shim underTest = new ShimImpl(aemDirMock, shimFilesFactoryMock);
		
		// When
		underTest.shim(operation, Path.of("unqualifiedAemDir"));
		
		// Then
		verify(aemDirMock, times(1)).toQualified(any(Path.class));
		verify(shimFilesMock, times(1)).createBatFiles(expectedCreateType);
		verify(shimFilesFactoryMock, times(1)).apply(eq(JavaVersion.VERSION_21), eq(tempDir));
	}

	@ParameterizedTest
	@CsvSource({
		"AEM_6.5_Quickstart.jar, VERSION_11", 
		"cq-quickstart-6.6.0.jar, VERSION_21" 
		})
	void testShim_AemDir(Path quickstartFile, JavaVersion expectedVersion, @TempDir Path tempDir) throws Exception{
		// Given
		// Create faux AEM 6.5 LTS Quickstart Jar file 
		Files.createFile(tempDir.resolve(quickstartFile));
		// Initialize Mocks
		when(shimFilesMock.createBatFiles(any(ShimFiles.CreateType.class))).thenReturn(shimFilesMock);
		when(aemDirMock.toQualified(any(Path.class))).thenReturn(tempDir);
		when(shimFilesFactoryMock.apply(any(), any(Path.class))).thenReturn(shimFilesMock);

		final Shim underTest = new ShimImpl(aemDirMock, shimFilesFactoryMock);
		
		// When
		underTest.shim(Shim.Operation.ADD, Path.of("unqualifiedAemDir"));
		
		// Then
		verify(aemDirMock, times(1)).toQualified(any(Path.class));
		verify(shimFilesMock, times(1)).createBatFiles(ShimFiles.CreateType.NEW);
		verify(shimFilesFactoryMock, times(1)).apply(eq(expectedVersion), eq(tempDir));
	}

	// TODO:  Create tests that throw ShimException when no quickstart files are found or multiple quickstart files are found.
	@Test
	void testShim_NoQuickstartFiles(@TempDir Path tempDir) throws Exception {
		// Given
		// Initialize Mocks
		when(aemDirMock.toQualified(any(Path.class))).thenReturn(tempDir);

		final Shim underTest = new ShimImpl(aemDirMock, shimFilesFactoryMock);

		// When/Then
		Shim.ShimException e = assertThrows(Shim.ShimException.class,
				() -> underTest.shim(Shim.Operation.ADD, Path.of("unqualifiedAemDir")));
		assertEquals("No AEM Quickstart found in directory: " + tempDir, e.getMessage());
		verify(aemDirMock, times(1)).toQualified(any(Path.class));
		verifyNoInteractions(shimFilesFactoryMock);
	}
	
	@Test
	void testShim_MultipleQuickstartFiles(@TempDir Path tempDir) throws Exception {
		// Given
		// Create faux AEM 6.5 LTS Quickstart Jar file
		Files.createFile(tempDir.resolve("cq-quickstart-6.6.0.jar"));
		Files.createFile(tempDir.resolve("AEM_6.5_Quickstart.jar"));
		// Initialize Mocks
		when(aemDirMock.toQualified(any(Path.class))).thenReturn(tempDir);

		final Shim underTest = new ShimImpl(aemDirMock, shimFilesFactoryMock);

		// When/Then
		Shim.ShimException e = assertThrows(Shim.ShimException.class,
				() -> underTest.shim(Shim.Operation.ADD, Path.of("unqualifiedAemDir")));
		assertEquals("Multiple AEM Quickstarts found in directory: " + tempDir, e.getMessage());
		verify(aemDirMock, times(1)).toQualified(any(Path.class));
		verifyNoInteractions(shimFilesFactoryMock);
	}
}
