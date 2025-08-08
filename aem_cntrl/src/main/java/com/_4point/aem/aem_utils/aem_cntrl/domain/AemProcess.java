package com._4point.aem.aem_utils.aem_cntrl.domain;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.LogFile;
import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.LogFile.FromOption;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.ipi.ProcessRunner;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.ipi.ProcessRunner.ListResult;
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

	
	private final Path aemQuickstartDir;
	private final TailerFactory tailerFactory;
	private final ShimFiles shimFiles;
	
	public AemProcess(Path aemQuickstartDir, TailerFactory tailerFactory, ShimFiles shimFiles) {
		this.aemQuickstartDir = aemQuickstartDir;
		this.tailerFactory = tailerFactory;
		this.shimFiles = shimFiles;
	}

	private String runUntilLogContains(Pattern targetPattern, Duration timeout, String...options) throws AemProcessException {		
		return runUntilLogContains(targetPattern, timeout, null, options); // no action to perform
	}

	private String runUntilLogContains(Pattern targetPattern, Duration timeout, Runnable action, String...options) throws AemProcessException {		
		try {
			CompletableFuture<Void> process = shimFiles.startAem().thenAccept(lr->handleResult(lr, "startup"));
			log.atInfo().log("AEM Started");
			LogFile logFile = LogFile.under(aemQuickstartDir, tailerFactory);
			try {
				String startResult = logFile.monitorLogFile(targetPattern, timeout, FromOption.END).orElseThrow(()->new AemProcessException("Failed to find string matching '%s' in log file before timeout (%d secs).".formatted(targetPattern, timeout.getSeconds())));
				sleepForSeconds(2, "Letting AEM finish");	// Give things some time to settle down before we shut everything down. 2 seconds should be enough
				if (action != null) {
					log.atInfo().log("Performing action after AEM startup.");
					action.run();
				}
				return startResult;
			} finally {
				shimFiles.stopAem().thenAccept(lr->handleResult(lr, "shutdown"));;
				// *INFO* [FelixStartLevel] org.apache.sling.commons.logservice BundleEvent STOPPING
				logFile.monitorLogFile(AEM_STOP_TARGET_PATTERN, timeout, FromOption.END).orElseThrow(()->new AemProcessException("Failed to find string matching '%s' in log file before timeout (%d secs).".formatted(targetPattern, timeout.getSeconds())));
				log.atInfo().log("AEM Stopped");
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new AemProcessException(e);
		}
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
	
	public static class AemProcessFactory {
		private final TailerFactory tailerFactory;
		private final ProcessRunner processRunner;
		private final ShimFiles.RuntimeFactory shimFilesFactory;
	
		
		public AemProcessFactory(TailerFactory tailerFactory, ProcessRunner processRunner, ShimFiles.RuntimeFactory shimFilesFactory) {
			this.tailerFactory = tailerFactory;
			this.processRunner = processRunner;
			this.shimFilesFactory = shimFilesFactory;
		}

		public AemProcess create(Path quickstartJarPath, JavaVersion aemJavaVersion) {
			return create(quickstartJarPath.getParent(), quickstartJarPath.getFileName(), aemJavaVersion);
		}
		
		private AemProcess create(Path aemQuickstartJarDir, Path aemQuickstartJarFilename, JavaVersion aemJavaVersion) {
			if (Files.exists(aemQuickstartJarDir.resolve(AemFiles.CRX_QUICKSTART_DIR))) {
				throw new IllegalArgumentException("Directory (%s) already contains initialized AEM instance.".formatted(aemQuickstartJarDir.toString()));
			}
			if (Files.isWritable(aemQuickstartJarDir.resolve(AemFiles.CRX_QUICKSTART_DIR))) {
				throw new IllegalArgumentException("Directory (%s) is not writable.".formatted(aemQuickstartJarDir.toString()));
			}
			
			new UninitializedAemInstance(aemJavaVersion, aemQuickstartJarDir, aemQuickstartJarFilename, processRunner).unpackQuickstart();
			
			final ShimFiles shimFiles = shimFilesFactory.apply(aemJavaVersion, aemQuickstartJarDir)
													    .createBatFiles();

			return new AemProcess(aemQuickstartJarDir, tailerFactory, shimFiles);
		}
	}

	private static class UninitializedAemInstance {

		private final Path aemQuickstartJarFilename;
		private final Path aemQuickstartJarDir;
		private final JavaVersion aemJavaVersion;
		private final ProcessRunner processRunner;

		public UninitializedAemInstance(JavaVersion aemJavaVersion, Path aemQuickstartJarDir, Path aemQuickstartJarFilename, ProcessRunner processRunner) {
			this.aemQuickstartJarFilename = aemQuickstartJarFilename;
			this.aemQuickstartJarDir = aemQuickstartJarDir;
			this.aemJavaVersion = aemJavaVersion;
			this.processRunner = processRunner;
		}

		public void unpackQuickstart()  {
			log.atInfo().log("Unpacking Quickstart jar");
			int exitCode = processRunner.runUntilCompletes(setupQuickstartProcessBuilder("-unpack"), aemQuickstartJarDir, Duration.ofMinutes(3));
			log.atDebug().addArgument(exitCode).log("Unpacking exit code = {}.");
		}

		private String[] setupQuickstartProcessBuilder(String... options) {
			String[] baseCommand = {"run", "--java=" + aemJavaVersion.getVersionString(), "--java-options=-Xmx2g", "-Djava.awt.headless=true", aemQuickstartJarFilename.toString() };
			String[] command = Stream.concat(Arrays.stream(baseCommand), Arrays.stream(options)).toArray(String[]::new);
			return OperatingSystem.getOs().jbangCommand(command);
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
