package com._4point.aem.aem_utils.aem_cntrl.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import com._4point.aem.aem_utils.aem_cntrl.adapters.ipi.JavaLangProcessRunner;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.CommonsIoTailerTailer;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.RestClientAemConfigManager;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.adapters.JacksonJsonData;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.adapters.SpringRestClientRestClient;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.RestClient;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.ipi.ProcessRunner;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.AemConfigManager;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer.TailerFactory;


@Disabled("This test is expected to be run manually. It is not part of the automated test suite.  Comment out this line to run it manually.")
@Tag("RequiresAemFiles")
class AemInstallerImplTest {
//	private static final Path AEM_FILES_LOC = Path.of("/Adobe", "AEM_65_SP19");
	private static final Path AEM_FILES_LOC = Path.of("AemSoftware");

	private static final Path EXPECTED_AEM_INSTALL_LOC = Path.of(OperatingSystem.isWindows() ? "Adobe" : "adobe", "AEM_65_SP21");
	private static final Path SAMPLE_AEM_QUICKSTART_PATH = Path.of("AEM_6.5_Quickstart.jar");
	private static final Path DEST_DIR = Path.of(OperatingSystem.isWindows() ? "\\" : "/opt");

	private final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();	// Configure client to follow redirects since AEM uses them a lot.
	private final RestClient restClient = SpringRestClientRestClient.create("http://localhost:4502", "admin", "admin", org.springframework.web.client.RestClient.builder().requestFactory(new JdkClientHttpRequestFactory(httpClient)));
	private final TailerFactory tailerFactory = new CommonsIoTailerTailer.TailerFactory();
	private final ProcessRunner processRunner = JavaLangProcessRunner.<Stream<String>, Stream<String>>builder()
																	 .setOutputStreamHandler(s->s)
																	 .setErrorStreamHandler(s->s)
																	 .build();
	private final AemConfigManager aemConfigManager = new RestClientAemConfigManager(restClient, JacksonJsonData::from);
	
	private final AemInstallerImpl underTest = new AemInstallerImpl(tailerFactory, processRunner, aemConfigManager);

	// Performs install to a temp directory.
	@Test
	void testInstallAem(@TempDir Path installLoc) throws Exception {

		underTest.installAem(DEST_DIR, installLoc.resolve(AEM_FILES_LOC));
		
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
