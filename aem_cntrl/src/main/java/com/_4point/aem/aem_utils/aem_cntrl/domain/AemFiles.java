package com._4point.aem.aem_utils.aem_cntrl.domain;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer.TailerFactory;


public class AemFiles {
	private static final Logger log = LoggerFactory.getLogger(AemFiles.class);

	static final Path CRX_QUICKSTART_DIR = Path.of("crx-quickstart");
	static final Path BIN_DIR = CRX_QUICKSTART_DIR.resolve("bin");
	static final Path INSTALL_DIR = CRX_QUICKSTART_DIR.resolve("install");
	static final Path START_SCRIPT = BIN_DIR.resolve("start");
	static final Path STOP_SCRIPT = BIN_DIR.resolve("stop");

	public static class SlingProperties {
		private static final Path SLING_PROPERTIES_PATH = CRX_QUICKSTART_DIR.resolve("conf").resolve("sling.properties");

		private final Path slingPropertiesFile;
		
		private SlingProperties(Path slingPropertiesFile) {
			this.slingPropertiesFile = slingPropertiesFile;
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
		
		public static Optional<SlingProperties> under(Path aemDir) {
			Path location = aemDir.resolve(SLING_PROPERTIES_PATH);
			if (Files.exists(location)) {
				return Optional.of(new SlingProperties(location));
			} else {
				log.atWarn().log("No sling properties file found at {}", location);
				return Optional.empty();
			}
		}
	}
	
	public static class LogFile {
		private static final Path LOG_FILE_PATH = CRX_QUICKSTART_DIR.resolve("logs").resolve("error.log");

		private final Path logFile;
		private final TailerFactory tailerFactory;

		public LogFile(Path logFile, TailerFactory tailerFactory) {
			this.logFile = logFile;
			this.tailerFactory = tailerFactory;
		}

		public Optional<String> monitorLogFileFromStart(Pattern pattern, Duration timeout) {
			try(Tailer tailer = tailerFactory.fromEnd(logFile)) {
				return monitorLogFile(tailer, pattern, timeout);
			} catch (Exception e) {
				throw new LogFileException(e);
			}
		}
		public Optional<String> monitorLogFileFromEnd(Pattern pattern, Duration timeout) {
			try(Tailer tailer = tailerFactory.fromEnd(logFile)) {
				return monitorLogFile(tailer, pattern, timeout);
			} catch (Exception e) {
				throw new LogFileException(e);
			}
		}

		private Optional<String> monitorLogFile(Tailer tailer, Pattern pattern, Duration timeout) {
			try(Stream<String> stream = tailer.stream()) {
				var result = CompletableFuture.supplyAsync(()->lookForLine(stream, pattern))
											  .completeOnTimeout(Optional.empty(), timeout.toMillis(), TimeUnit.MILLISECONDS)
											  .join();
				return result;
			} catch (Exception e) {
				throw new LogFileException(e);
			}
		}

		private static String logInput(String inputLine) {
			log.atTrace().addArgument(inputLine).log("Found line '{}' in stream.");
			return inputLine;
		}
		
		private static Optional<String> lookForLine(Stream<String> stream, Pattern pattern) {
			log.atDebug().addArgument(pattern.toString()).log("Looking for line matching regex '{}'.");
			return stream
						 .map(LogFile::logInput)	// Doesn't transform input, just logs it.
						 .filter(s->pattern.matcher(s).matches())
						 .findAny();
		}

		public static LogFile under(Path aemDir, TailerFactory tailerFactory) {
			return new LogFile(aemDir.resolve(LOG_FILE_PATH), tailerFactory);
		}
		
		@SuppressWarnings("serial")
		public static class LogFileException extends RuntimeException {
			public LogFileException(String message) {
				super(message);
			}

			public LogFileException(Throwable cause) {
				super(cause);
			}

			public LogFileException(String message, Throwable cause) {
				super(message, cause);
			}

			public LogFileException() {
			}
		}
	}
}
