package com._4point.aem.aem_utils.aem_cntrl.domain;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AemFiles {
	private static final Logger log = LoggerFactory.getLogger(AemFiles.class);

	static final Path CRX_QUICKSTART_DIR = Path.of("crx-quickstart");
	static final Path LOG_FILE = CRX_QUICKSTART_DIR.resolve("logs").resolve("error.log");
	static final Path BIN_DIR = CRX_QUICKSTART_DIR.resolve("bin");
	static final Path INSTALL_DIR = CRX_QUICKSTART_DIR.resolve("install");
	static final Path START_SCRIPT = BIN_DIR.resolve("start");
	static final Path STOP_SCRIPT = BIN_DIR.resolve("stop");

	public static class SlingProperties {
		private static final Path SLING_PROPERTIES_PATH = Path.of("sling.properties");

		private final Path slingPropertiesFile;
		
		private SlingProperties(Path slingPropertiesFile) {
			this.slingPropertiesFile = slingPropertiesFile;
		}

		private static Path constructSlingPropertiesPath(Path quickstartDir) {
			return quickstartDir.resolve("conf").resolve(SLING_PROPERTIES_PATH);
		}

		public void updateSlingProperties() {
			try {
				String targetString = "sling.bootdelegation.class.com.rsa.jsafe.provider.JsafeJCE=com.rsa.*";
				// Read in file, if it doesn't conotain the target string then add it to the end of the file
				// If it does contain the target string, then do nothing
				List<String> lines = new ArrayList<>(Files.readAllLines(slingPropertiesFile));	// Since Files.readAllLines() returns an unspecified (either mutable nor immutable) list, we need to create a mutable copy
				if (lines.stream().anyMatch(l -> l.contains(targetString))) {
					log.atWarn().log("Sling properties file already contains the target string.");
					return;
				}
				log.atInfo().log("Adding target string to sling properties file.");
				lines.add(targetString);
				Files.write(slingPropertiesFile, lines);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		
		public static Optional<SlingProperties> under(Path crxQuickstartDir) throws FileNotFoundException {
			Path location = constructSlingPropertiesPath(crxQuickstartDir);
			if (Files.exists(location)) {
				return Optional.of(new SlingProperties(location));
			} else {
				log.atWarn().log("No sling properties file found at {}", location);
				return Optional.empty();
			}
		}
	}
	

}
