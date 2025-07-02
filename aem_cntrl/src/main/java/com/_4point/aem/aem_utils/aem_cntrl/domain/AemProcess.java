package com._4point.aem.aem_utils.aem_cntrl.domain;

import static java.nio.file.attribute.PosixFilePermission.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._4point.aem.aem_utils.aem_cntrl.domain.ProcessRunner.ListResult;
import com._4point.aem.aem_utils.aem_cntrl.domain.ProcessRunner.ProcessRunnerException;
import com._4point.aem.aem_utils.aem_cntrl.domain.ProcessRunner.RunningProcess;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer.TailerFactory;


public class AemProcess {
	private static final Logger log = LoggerFactory.getLogger(AemProcess.class);
	
	// RegEx that we look for to know that AEMM has stopped.
	// N.B>  These are package private to allow for unit testing of the RegEx expressions.
	//   Older versions of AEM write commong.logservice, but newer ones write commons.log 
	/* package */ static final String AEM_STOP_TARGET_REGEX = ".*org\\.apache\\.sling\\.installer\\.core\\.impl\\.OsgiInstallerImpl Apache Sling OSGi Installer Service stopped.*";
	// Regex the we look for to know that AEM has started
	/* package */ static final String AEM_START_TARGET_REGEX = ".*com\\.adobe\\.granite\\.workflow\\.core\\.launcher\\.WorkflowLauncherListener StartupListener\\.startupFinished called.*";
	// Regex that we look for to know that the AE< Service Pack is installed
	/* package */ static final String AEM_SP_START_TARGET_REGEX = ".*com\\.adobe\\.granite\\.installer\\.Updater Content Package AEM-\\d\\.\\d-Service-Pack-\\d+ Installed successfully";
	// Regex that we look for to know that the AEM Forms Add-on is installed.
	/* package */ static final String AEM_FORMS_ADD_ON_START_TARGET_REGEX = ".*Installed BMC XMLFormService of type BMC_NATIVE.*";

	private static final ExecutorService EXECUTOR_SERVICE = Executors.newVirtualThreadPerTaskExecutor();
	private static final Path RUN_START = OperatingSystem.getOs().runStart();
	private static final Path RUN_STOP = OperatingSystem.getOs().runStop();
	private static final Path QUICKSTART_DIR = Path.of("crx-quickstart");
	private static final Path LOG_FILE = QUICKSTART_DIR.resolve("logs").resolve("error.log");
	private static final Path BIN_DIR = QUICKSTART_DIR.resolve("bin");
	private static final Path START_SCRIPT = BIN_DIR.resolve("start");
	private static final Path STOP_SCRIPT = BIN_DIR.resolve("stop");
	
	private final Path aemQuickstartDir;
	private final TailerFactory tailerFactory;
	
	public AemProcess(Path aemQuickstartDir, TailerFactory tailerFactory) {
		this.aemQuickstartDir = aemQuickstartDir;
		this.tailerFactory = tailerFactory;
	}

	private static int runUntilCompletes(Supplier<ProcessBuilder> processBuilderFactory, Duration timeout) {
		ProcessBuilder processBuilder = processBuilderFactory.get();
		
		RunningProcess<Stream<String>, Stream<String>> process = ProcessRunner.<Stream<String>, Stream<String>>builder()
				.setOutputStreamHandler(s->s)
				.setErrorStreamHandler(s->s)
				.build()
				.run(processBuilder);
		log.atInfo().log("Running AEM with options");
		try {
			Stream<String> stdoutStream = process.stdout().get();
			CompletableFuture<Void> stdoutFuture = CompletableFuture.runAsync(()->stdoutStream.forEach(s->log.atDebug().log(s)), EXECUTOR_SERVICE);
			Stream<String> stderrStream = process.stderr().get();
			CompletableFuture<Void> stderrFuture = CompletableFuture.runAsync(()->stderrStream.forEach(s->log.atDebug().log(s)), EXECUTOR_SERVICE);
			int result = process.waitForCompletion();
			return result;
		} catch (InterruptedException | ExecutionException e) {
			throw new AemProcessException(e);
		}
	}
	
	private String runUntilLogContains(String targetRegEx, Duration timeout, String...options) throws AemProcessException {		
		return runUntilLogContains(targetRegEx, timeout, null, options); // no action to perform
	}

	private String runUntilLogContains(String targetRegEx, Duration timeout, Runnable action, String...options) throws AemProcessException {		
		try {
			CompletableFuture<Void> process = startAem().thenAccept(lr->handleResult(lr, "startup"));
			log.atInfo().log("AEM Started");
			try {
				String startResult = monitorLogFile(logFile(), targetRegEx, timeout).orElseThrow(()->new AemProcessException("Failed to find string matching '%s' in log file before timeout (%d secs).".formatted(targetRegEx, timeout.getSeconds())));
				if (action != null) {
					log.atInfo().log("Performing action after AEM startup.");
					action.run();
				}
				return startResult;
			} finally {
				log.atInfo().log("Stopping AEM");
				stopAem().thenAccept(lr->handleResult(lr, "shutdown"));;
				// *INFO* [FelixStartLevel] org.apache.sling.commons.logservice BundleEvent STOPPING
				monitorLogFile(logFile(), AEM_STOP_TARGET_REGEX, timeout).orElseThrow(()->new AemProcessException("Failed to find string matching '%s' in log file before timeout (%d secs).".formatted(targetRegEx, timeout.getSeconds())));
				log.atInfo().log("AEM Stopped");
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new AemProcessException(e);
		}
	}

	private CompletableFuture<ListResult> startAem() throws InterruptedException, ExecutionException {
		ProcessBuilder processBuilder = new ProcessBuilder(aemQuickstartDir.resolve(RUN_START).toString())
											.directory(aemQuickstartDir.toFile());
		log.atInfo().log("Running AEM");
		return ProcessRunner.runtoListResult(processBuilder);
//		if (process.exitCode() != 0) {
//			log.atError().addArgument(()->process.stdoutAsString()).log("Error occurred during AEM startup [STDOUT] {}");
//			log.atError().addArgument(()->process.stderrAsString()).log("Error occurred during AEM startup [STDERR] {}");
//		} else {
//			log.atDebug().addArgument(()->process.stdoutAsString()).log("AEM startup [STDOUT] {}");
//			log.atDebug().addArgument(()->process.stderrAsString()).log("AEM startup [STDERR] {}");
//		}
	}

	private CompletableFuture<ListResult> stopAem() throws InterruptedException, ExecutionException {
		ProcessBuilder processBuilder = new ProcessBuilder(aemQuickstartDir.resolve(RUN_STOP).toString())
											.directory(aemQuickstartDir.toFile());
		log.atInfo().log("Stopping AEM at callers request.");
		return ProcessRunner.runtoListResult(processBuilder);
	}

	private void handleResult(ListResult process, String operation) {
		if (process.exitCode() != 0) {
			log.atError()
			   .addArgument(operation)
			   .addArgument(()->process.stdoutAsString())
			   .log("Error occurred during AEM {} [STDOUT] {}");
			log.atError()
			   .addArgument(operation)
			   .addArgument(()->process.stderrAsString())
			   .log("Error occurred during AEM {} [STDERR] {}");
		} else {
			log.atDebug()
			   .addArgument(operation)
			   .addArgument(()->process.stdoutAsString())
			   .log("AEM {} [STDOUT] {}");
			log.atDebug()
			   .addArgument(operation)
			   .addArgument(()->process.stderrAsString())
			   .log("AEM {} [STDERR] {}");
		}
	}

	
	
//	private ProcessBuilder setupAppProcessBuilder(String... options) {
//		// jbang jdk java-env 11
//		// jbang jdk java-env 11 > setenv.bat
//		// setenv
//		String[] baseCommand = {"CMD.exe","/C", "jbang", "run", "--java=11", "--java-options=-Xmx2g", "-Djava.awt.headless=false", "-add-modules java.se.ee", aemAppPath.toString() };
//		String[] command = Stream.concat(Arrays.stream(baseCommand), Arrays.stream(options)).toArray(String[]::new);
//		ProcessBuilder processBuilder = new ProcessBuilder(command)
//												.directory(aemQuickstartDir.toFile());
//		return processBuilder;
//	}

//	public String startApp()  {
//		Supplier<ProcessBuilder> processBuilderFactory = ()->setupAppProcessBuilder("start", "-c", aemQuickstartDir.toString(), "-i", "launchpad", "-Dsling.properties=conf/sling.properties");
//		return Integer.toString(runUntilCompletes(processBuilderFactory, Duration.ofMinutes(10)));
//	}

	public String startQuickstartInitializeAem()  {
		return runUntilLogContains(AEM_START_TARGET_REGEX, Duration.ofMinutes(15));
	}

	public String startQuickstartInstallServicePack()  {
		String result = runUntilLogContains(AEM_SP_START_TARGET_REGEX, Duration.ofMinutes(10));
		// The string we are looking for is the end of the install process but we want to give things another few seconds to calm down.
		// Once we have found it, wait another minute.
		try {
			Thread.sleep(Duration.ofSeconds(15));
			return result;
		} catch (InterruptedException e) {
			throw new AemProcessException(e);
		}
	}

	public String startQuickstartInstallFormsAddOn()  {
		String searchString = runUntilLogContains(AEM_FORMS_ADD_ON_START_TARGET_REGEX, Duration.ofMinutes(20));
		// The string we are looking for is *near* the end of the install process but not at the end.
		// Once we have found it, wait another minute.
		try {
			Thread.sleep(Duration.ofMinutes(1));
		} catch (InterruptedException e) {
			throw new AemProcessException(e);
		}
		return searchString;
	}

	public String startQuickstartPerformAction(Runnable action) {
		return runUntilLogContains(AEM_START_TARGET_REGEX, Duration.ofMinutes(5), action);
	}
	
	private Optional<String> monitorLogFile(Path logFile, String regex, Duration timeout) {
		try(Tailer tailer = tailerFactory.fromEnd(logFile); Stream<String> stream = tailer.stream()) {
			var result = CompletableFuture.supplyAsync(()->lookForLine(stream, regex))
										  .completeOnTimeout(Optional.empty(), timeout.toMillis(), TimeUnit.MILLISECONDS)
										  .join();
			sleepForSeconds(2);	// Give things some time to settle down before we shut everything down. 2 seconds should be enough
			return result;
		} catch (Exception e) {
			throw new AemProcessException(e);
		}
	}

	private static Optional<String> lookForLine(Stream<String> stream, String regex) {
		Pattern pattern = Pattern.compile(regex);
		log.atDebug().addArgument(regex).log("Looking for line containing '{}'.");
		return stream
					 .map(AemProcess::logInput)	// Doesn't transform input, just logs it.
					 .filter(s->pattern.matcher(s).matches())
					 .findAny();
	}
	
	private static InterruptedException createException(String targetRegEx, String stderr) {
		log.atError().addArgument(stderr).log("Aem stderr is:\n{}");
		return new InterruptedException("AEM Terminated unexpectedly before regex (%s) was found.".formatted(targetRegEx));
	}

	private static void sleepForSeconds(int numSecs) {
		try {
			Thread.sleep(Duration.ofSeconds(numSecs));
		} catch (InterruptedException e) {
			log.atError().setCause(e).log("Sleep of %s seconds was interrupted.".formatted(numSecs));
		}
	}
	private Path logFile() {
		return aemQuickstartDir.resolve(LOG_FILE);
	}

	private static String logInput(String inputLine) {
		log.atTrace().addArgument(inputLine).log("Found line '{}' in stream.");
		return inputLine;
	}
	
	// TODO: Create an initializedAemInstance class too (for one that's already been set up)
	//       Create a static method that detects whether AEM has been initialzed and returns the correct initalized/uninitialized instance.
	public static class UninitializedAemInstance {
		private static final Set<PosixFilePermission> FILE_PERMISSIONS = EnumSet.of(OWNER_READ, OWNER_EXECUTE, GROUP_READ, GROUP_EXECUTE, OTHERS_READ, OTHERS_EXECUTE);
		private final Path aemQuickstartFilename;
		private final Path aemQuickstartDir;
		private final JavaVersion aemJavaVersion;

		
		public UninitializedAemInstance(Path aemQuickstartPath, JavaVersion aemJavaVersion) {
			this.aemQuickstartFilename = aemQuickstartPath.getFileName();
			this.aemQuickstartDir = aemQuickstartPath.getParent();
			this.aemJavaVersion = aemJavaVersion;
			if (Files.exists(aemQuickstartDir.resolve(QUICKSTART_DIR))) {
				throw new IllegalArgumentException("Directory (%s) already contains initialized AEM instance.".formatted(aemQuickstartDir.toString()));
			}
			if (Files.isWritable(aemQuickstartDir.resolve(QUICKSTART_DIR))) {
				throw new IllegalArgumentException("Directory (%s) is not writable.".formatted(aemQuickstartDir.toString()));
			}
		}

		public AemProcess unpackQuickstart(TailerFactory tailerFactory)  {
			Supplier<ProcessBuilder> processBuilderFactory = ()->setupQuickstartProcessBuilder("-unpack");
			int exitCode = runUntilCompletes(processBuilderFactory, Duration.ofMinutes(3));
			log.atDebug().addArgument(exitCode).log("Unpacking exit code = {}.");
			createBatFiles();
			return new AemProcess(aemQuickstartDir, tailerFactory);
		}

		private ProcessBuilder setupQuickstartProcessBuilder(String... options) {
			String[] baseCommand = {"run", "--java=" + aemJavaVersion.getVersionString(), "--java-options=-Xmx2g", "-Djava.awt.headless=true", aemQuickstartFilename.toString() };
			String[] command = Stream.concat(Arrays.stream(baseCommand), Arrays.stream(options)).toArray(String[]::new);
			ProcessBuilder processBuilder = new ProcessBuilder(OperatingSystem.getOs().jbangCommand(command))
													.directory(aemQuickstartDir.toFile());
			return processBuilder;
		}

		private String getJavaEnv() {
			ProcessBuilder processBuilder = new ProcessBuilder(OperatingSystem.getOs().jbangCommand("jdk", "java-env", aemJavaVersion.getVersionString()))
													.directory(aemQuickstartDir.toFile());

			try {
				ListResult result = ProcessRunner.runtoListResult(processBuilder).get();
				return result.stdout().stream().collect(Collectors.joining("\n"));
			} catch (ProcessRunnerException | InterruptedException | ExecutionException e) {
				throw new AemProcessException(e);
			}
		}
		
		private void createBatFiles() {
			try {
				String javaEnv = getJavaEnv();
				
				writeScript(aemQuickstartDir.resolve(RUN_START), javaEnv + "\n" + aemQuickstartDir.resolve(START_SCRIPT).toString());
				writeScript(aemQuickstartDir.resolve(RUN_STOP), javaEnv + "\n" + aemQuickstartDir.resolve(STOP_SCRIPT).toString());
			} catch (IOException | UnsupportedOperationException e) {
				throw new AemProcessException("Error while writing start/stop bat files to %s.".formatted(aemQuickstartDir), e);
			}
		}

		private static void writeScript(Path runStart, String script) throws IOException, UnsupportedOperationException {
			Files.writeString(runStart, script);
			if (OperatingSystem.isUnix()) {
				Files.setPosixFilePermissions(runStart, FILE_PERMISSIONS);
			}
		}
	}
	
	@SuppressWarnings("serial")
	public static class AemProcessException extends RuntimeException {

		public AemProcessException() {
		}

		public AemProcessException(String message, Throwable cause) {
			super(message, cause);
		}

		public AemProcessException(String message) {
			super(message);
		}

		public AemProcessException(Throwable cause) {
			super(cause);
		}
	}
}
