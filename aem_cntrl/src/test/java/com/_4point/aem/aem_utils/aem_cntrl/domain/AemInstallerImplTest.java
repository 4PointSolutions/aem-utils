package com._4point.aem.aem_utils.aem_cntrl.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import javax.swing.filechooser.FileSystemView;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import com._4point.aem.aem_utils.aem_cntrl.adapters.ipi.JacksonJsonData;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.CommonsIoTailerTailer;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.SpringRestClientRestClient;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.RestClient;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer.TailerFactory;


@Disabled("Don't want to perform full install at this time.")
@Tag("RequiresAemFiles")
class AemInstallerImplTest {
//	private static final Path AEM_FILES_LOC = Path.of("/Adobe", "AEM_65_SP19");
	private static final Path AEM_FILES_LOC = Path.of("AemSoftware");

	private static final Path EXPECTED_AEM_INSTALL_LOC = Path.of(OperatingSystem.isWindows() ? "Adobe" : "adobe", "AEM_65_SP21");
	private static final Path SAMPLE_AEM_QUICKSTART_PATH = Path.of("AEM_6.5_Quickstart.jar");
	private static final Path SOURCE_DIR = OperatingSystem.isWindows() ? Path.of("\\Adobe") : FileSystemView.getFileSystemView().getHomeDirectory().toPath();
	private static final Path DEST_DIR = Path.of(OperatingSystem.isWindows() ? "\\" : "/opt");

	private final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();	// Configure client to follow redirects since AEM uses them a lot.
	private final RestClient restClient = SpringRestClientRestClient.create("http://localhost:4502", "admin", "admin", org.springframework.web.client.RestClient.builder().requestFactory(new JdkClientHttpRequestFactory(httpClient)));
	private final TailerFactory tailerFactory = new CommonsIoTailerTailer.TailerFactory();
	
	private final AemInstallerImpl underTest = new AemInstallerImpl(restClient, JacksonJsonData::from, tailerFactory);
	
	@Test
	void testInstallAem(@TempDir Path installLoc) throws Exception {

		underTest.installAem(DEST_DIR, SOURCE_DIR.resolve(AEM_FILES_LOC));
		
		printFiles(installLoc);
		assertTrue(Files.exists(installLoc.resolve(EXPECTED_AEM_INSTALL_LOC).resolve(SAMPLE_AEM_QUICKSTART_PATH)));
	}

	private void printFiles(Path dir) {
		listFiles(dir)
			 .flatMap(p->Files.isDirectory(p) ? listFiles(p) : Stream.of(p))
			 .forEach(p->System.out.println(p.toAbsolutePath().toString()));
			 ;
	}

	private Stream<Path> listFiles(Path dir) {
		try {
			return Files.list(dir);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
