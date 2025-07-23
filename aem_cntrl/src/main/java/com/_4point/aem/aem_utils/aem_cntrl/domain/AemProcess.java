package com._4point.aem.aem_utils.aem_cntrl.domain;

import static java.nio.file.attribute.PosixFilePermission.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.LogFile;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.ipi.ProcessRunner;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.ipi.ProcessRunner.ListResult;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.ipi.ProcessRunner.ProcessRunnerException;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer.TailerFactory;


public class AemProcess {
	private static final Logger log = LoggerFactory.getLogger(AemProcess.class);
	
	// RegEx that we look for to know that AEMM has stopped.
	// N.B>  These are package private to allow for unit testing of the RegEx expressions.
	//   Older versions of AEM write commong.logservice, but newer ones write commons.log 
	/* package */ static final Pattern AEM_STOP_TARGET_PATTERN = Pattern.compile(".*org\\.apache\\.sling\\.installer\\.core\\.impl\\.OsgiInstallerImpl Apache Sling OSGi Installer Service stopped.*");
	// Regex the we look for to know that AEM has started
	/* package */ static final Pattern AEM_START_TARGET_PATTERN = Pattern.compile(".*com\\.adobe\\.granite\\.workflow\\.core\\.launcher\\.WorkflowLauncherListener StartupListener\\.startupFinished called.*");
	// Regex that we look for to know that the AE< Service Pack is installed
	/* package */ static final Pattern AEM_SP_START_TARGET_PATTERN = Pattern.compile(".*com\\.adobe\\.granite\\.installer\\.Updater Content Package AEM-\\d\\.\\d-Service-Pack-\\d+ Installed successfully");
	// Regex that we look for to know that the AEM Forms Add-on is installed.
	/* package */ static final Pattern AEM_FORMS_ADD_ON_START_TARGET_PATTERN = Pattern.compile(".*Installed BMC XMLFormService of type BMC_NATIVE.*");

	private static final Path RUN_START = OperatingSystem.getOs().runStart();
	private static final Path RUN_STOP = OperatingSystem.getOs().runStop();
	
	private final Path aemQuickstartDir;
	private final TailerFactory tailerFactory;
	private final ProcessRunner processRunner;
	
	public AemProcess(Path aemQuickstartDir, TailerFactory tailerFactory, ProcessRunner processRunner) {
		this.aemQuickstartDir = aemQuickstartDir;
		this.tailerFactory = tailerFactory;
		this.processRunner = processRunner;
	}

	private String runUntilLogContains(Pattern targetPattern, Duration timeout, String...options) throws AemProcessException {		
		return runUntilLogContains(targetPattern, timeout, null, options); // no action to perform
	}

	private String runUntilLogContains(Pattern targetPattern, Duration timeout, Runnable action, String...options) throws AemProcessException {		
		try {
			CompletableFuture<Void> process = startAem().thenAccept(lr->handleResult(lr, "startup"));
			log.atInfo().log("AEM Started");
			LogFile logFile = LogFile.under(aemQuickstartDir, tailerFactory);
			try {
				String startResult = logFile.monitorLogFileFromEnd(targetPattern, timeout).orElseThrow(()->new AemProcessException("Failed to find string matching '%s' in log file before timeout (%d secs).".formatted(targetPattern, timeout.getSeconds())));
				sleepForSeconds(2, "Letting AEM finish");	// Give things some time to settle down before we shut everything down. 2 seconds should be enough
				if (action != null) {
					log.atInfo().log("Performing action after AEM startup.");
					action.run();
				}
				return startResult;
			} finally {
				log.atInfo().log("Stopping AEM");
				stopAem().thenAccept(lr->handleResult(lr, "shutdown"));;
				// *INFO* [FelixStartLevel] org.apache.sling.commons.logservice BundleEvent STOPPING
				logFile.monitorLogFileFromEnd(AEM_STOP_TARGET_PATTERN, timeout).orElseThrow(()->new AemProcessException("Failed to find string matching '%s' in log file before timeout (%d secs).".formatted(targetPattern, timeout.getSeconds())));
				log.atInfo().log("AEM Stopped");
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new AemProcessException(e);
		}
	}

	private CompletableFuture<ListResult> startAem() throws InterruptedException, ExecutionException {
		log.atInfo().log("Running AEM");
		return processRunner.runtoListResult(new String[] {aemQuickstartDir.resolve(RUN_START).toString()}, aemQuickstartDir);
//		if (process.exitCode() != 0) {
//			log.atError().addArgument(()->process.stdoutAsString()).log("Error occurred during AEM startup [STDOUT] {}");
//			log.atError().addArgument(()->process.stderrAsString()).log("Error occurred during AEM startup [STDERR] {}");
//		} else {
//			log.atDebug().addArgument(()->process.stdoutAsString()).log("AEM startup [STDOUT] {}");
//			log.atDebug().addArgument(()->process.stderrAsString()).log("AEM startup [STDERR] {}");
//		}
	}

	private CompletableFuture<ListResult> stopAem() throws InterruptedException, ExecutionException {
		log.atInfo().log("Stopping AEM at callers request.");
		return processRunner.runtoListResult(new String[] {aemQuickstartDir.resolve(RUN_STOP).toString()}, aemQuickstartDir);
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
		return runUntilLogContains(AEM_START_TARGET_PATTERN, Duration.ofMinutes(15));
	}

	public String startQuickstartInstallServicePack()  {
		return runUntilLogContains(AEM_SP_START_TARGET_PATTERN, Duration.ofMinutes(10));
	}

	public String startQuickstartInstallFormsAddOn()  {
		return runUntilLogContains(AEM_FORMS_ADD_ON_START_TARGET_PATTERN, Duration.ofMinutes(20));
	}

	public String startQuickstartPerformAction(Runnable action) {
		return runUntilLogContains(AEM_START_TARGET_PATTERN, Duration.ofMinutes(5), action);
	}
	
	private static void sleepForSeconds(int numSecs, String reason) {
		try {
			log.atInfo().addArgument(reason).addArgument(numSecs).log("{}, sleeping for {} seconds.");
			Thread.sleep(Duration.ofSeconds(numSecs));
		} catch (InterruptedException e) {
			log.atError().setCause(e).log("Sleep of %s seconds was interrupted.".formatted(numSecs));
			throw new AemProcessException(e);
		}
	}
	
	// TODO: Create an initializedAemInstance class too (for one that's already been set up)
	//       Create a static method that detects whether AEM has been initialzed and returns the correct initalized/uninitialized instance.
	public static class UninitializedAemInstance {
		private static final Set<PosixFilePermission> FILE_PERMISSIONS = EnumSet.of(OWNER_READ, OWNER_EXECUTE, GROUP_READ, GROUP_EXECUTE, OTHERS_READ, OTHERS_EXECUTE);
		private final Path aemQuickstartJarFilename;
		private final Path aemQuickstartJarDir;
		private final JavaVersion aemJavaVersion;
		private final ProcessRunner processRunner;

		
		public UninitializedAemInstance(Path aemQuickstartJarPath, JavaVersion aemJavaVersion, ProcessRunner processRunner) {
			this.aemQuickstartJarFilename = aemQuickstartJarPath.getFileName();
			this.aemQuickstartJarDir = aemQuickstartJarPath.getParent();
			this.aemJavaVersion = aemJavaVersion;
			this.processRunner = processRunner;
			if (Files.exists(aemQuickstartJarDir.resolve(AemFiles.CRX_QUICKSTART_DIR))) {
				throw new IllegalArgumentException("Directory (%s) already contains initialized AEM instance.".formatted(aemQuickstartJarDir.toString()));
			}
			if (Files.isWritable(aemQuickstartJarDir.resolve(AemFiles.CRX_QUICKSTART_DIR))) {
				throw new IllegalArgumentException("Directory (%s) is not writable.".formatted(aemQuickstartJarDir.toString()));
			}
		}

		public AemProcess unpackQuickstart(TailerFactory tailerFactory)  {
			int exitCode = processRunner.runUntilCompletes(setupQuickstartProcessBuilder("-unpack"), aemQuickstartJarDir, Duration.ofMinutes(3));
			log.atDebug().addArgument(exitCode).log("Unpacking exit code = {}.");
			createBatFiles();
			return new AemProcess(aemQuickstartJarDir, tailerFactory, processRunner);
		}

		private String[] setupQuickstartProcessBuilder(String... options) {
			String[] baseCommand = {"run", "--java=" + aemJavaVersion.getVersionString(), "--java-options=-Xmx2g", "-Djava.awt.headless=true", aemQuickstartJarFilename.toString() };
			String[] command = Stream.concat(Arrays.stream(baseCommand), Arrays.stream(options)).toArray(String[]::new);
			return OperatingSystem.getOs().jbangCommand(command);
		}

		private String getJavaEnv() {
			try {
				ListResult result = processRunner.runtoListResult(OperatingSystem.getOs().jbangCommand("jdk", "java-env", aemJavaVersion.getVersionString()), aemQuickstartJarDir).get();
				return result.stdout().stream().collect(Collectors.joining("\n"));
			} catch (ProcessRunnerException | InterruptedException | ExecutionException e) {
				throw new AemProcessException(e);
			}
		}
		
		private void createBatFiles() {
			try {
				String javaEnv = getJavaEnv();
				
				writeScript(aemQuickstartJarDir.resolve(RUN_START), javaEnv + "\n" + aemQuickstartJarDir.resolve(AemFiles.START_SCRIPT).toString());
				writeScript(aemQuickstartJarDir.resolve(RUN_STOP), javaEnv + "\n" + aemQuickstartJarDir.resolve(AemFiles.STOP_SCRIPT).toString());
			} catch (IOException | UnsupportedOperationException e) {
				throw new AemProcessException("Error while writing start/stop bat files to %s.".formatted(aemQuickstartJarDir), e);
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
