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
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.WaitForLog.WaitForLogException;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer.TailerFactory;


/**
 * Collection of classes for interacting with post-installation AEM files and directories.
 * 
 */
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
	
	/**
	 * Class for handling interactions with the AEM error.log file.
	 * 
	 * 
	 */
	public static class LogFile {
		private static final Path LOG_FILE_PATH = CRX_QUICKSTART_DIR.resolve("logs").resolve("error.log");

		public enum FromOption {
			START(TailerFactory.FromOption.BEGINNING), END(TailerFactory.FromOption.END);

			private final TailerFactory.FromOption tailerFactoryFromOption;

			private FromOption(TailerFactory.FromOption tailerFactoryFromOption) {
				this.tailerFactoryFromOption = tailerFactoryFromOption;
			}

			private TailerFactory.FromOption tailerFactoryFromOption() {
				return tailerFactoryFromOption;
			}
			
		}
		
		private final Path logFile;
		private final TailerFactory tailerFactory;

		public LogFile(Path logFile, TailerFactory tailerFactory) {
			this.logFile = logFile;
			this.tailerFactory = tailerFactory;
		}

		public Optional<String> monitorLogFile(Pattern pattern, Duration timeout, FromOption fromOption) {
			try(Tailer tailer = tailerFactory.from(logFile, fromOption.tailerFactoryFromOption())) {
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
	
	/**
	 * Class for working with the AEM directory.  This is often provided "unqualified" by 
	 * the user, so this class provides methods to convert the unqualified AEM directory 
	 * to a fully qualified/verified path.
	 */
	public static class AemDir {
		enum AemDirType {
			DEFAULT, 	// Relative to the default AEM directory
			RELATIVE, 	// Relative to the current working directory
			ABSOLUTE,	// Absolute path
			NULL, 		// Null
			;
			
			static AemDirType of(Path aemDir) {
				if (aemDir == null) {
					return NULL;
				} else if (aemDir.getRoot() != null) {
	            	return ABSOLUTE;
	            } else if (isRelative(aemDir.subpath(0, 1).toString())) {
	            	return RELATIVE;
	            } else {
	            	return DEFAULT;
	            }
	        }
			
			private static boolean isRelative(String firstElement) {
				return ".".equals(firstElement) || "..".equals(firstElement);
			}
		}

		private final Supplier<Path> defaultAemDirSupplier;

		public AemDir(Supplier<Path> defaultAemDirSupplier) {
			this.defaultAemDirSupplier = defaultAemDirSupplier;
		}

		public Path toQualified(Path unqualifiedAemDir) {
			return locateAemDir(adjustProvidedAemDirParam(unqualifiedAemDir));
		}
		
		/**
		 * Adjusts the AEM directory based on the type of Path specified. 
		 *   - If the Path is null, it returns the default AEM directory.
		 *   - If the Path is a relative path, it resolves it against the default AEM directory.
		 *   - If the Path is a relative path starting with . or .. , it resolves it against the current directory.
		 *   - If the Path is an absolute path, it returns the Path as is.
		 * 
		 * @param aemDir
		 * @return
		 */
		private Path adjustProvidedAemDirParam(Path aemDir) {
			// Adjust the AEM directory based on the type of Path specified.
			AemDirType aemDirType = AemDirType.of(aemDir);
			if (aemDirType == AemDirType.NULL) {				// Not specified, use the default AEM directory
				return defaultAemDirSupplier.get();
			} else if (aemDirType == AemDirType.DEFAULT) {		// Specified as a relative path to the default AEM directory
				return defaultAemDirSupplier.get().resolve(aemDir);
			} // else ABSOLUTE or RELATIVE, no change needed
			return aemDir;
		}

		private Path locateAemDir(final Path adjustedAemDir) {
			// If the adjusted AEM directory does not contain crx-quickstart, then locate a child directory that does.
			return isAemDir(adjustedAemDir) ? adjustedAemDir : locateAemChildDir(adjustedAemDir);
		}
		
		private static Path locateAemChildDir(Path aemParentDir) {
	        try {
	        	List<Path> aemDirs = locateAemDirs(aemParentDir).toList();
	        	if (aemDirs.size() == 0) {
	        		throw new WaitForLogException("No AEM directory found in " + aemParentDir);
	        	} else if (aemDirs.size() > 1) {
	        		throw new WaitForLogException("Too many AEM directories found in " + aemParentDir + ". Please be more specific in your AEM directory specification.");
	        	}
	        	return aemDirs.getFirst();
	        } catch (IOException e) {
	            throw new WaitForLogException("Error locating AEM directories in " + aemParentDir, e);
	        }
		}
		
		private static Stream<Path> locateAemDirs(Path aemParentDir) throws IOException {
			return Files.list(aemParentDir)
					.filter(p -> isAemDir(p)) // Filter directories that contain the CRX Quickstart directory)
					;
		}
		
		private static boolean isAemDir(Path p) {
	        return Files.isDirectory(p) && Files.exists(p.resolve(AemFiles.CRX_QUICKSTART_DIR));
		}

	}
}
