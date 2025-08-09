package com._4point.aem.aem_utils.aem_cntrl.domain;

import static java.nio.file.attribute.PosixFilePermission.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._4point.aem.aem_utils.aem_cntrl.domain.AemProcess.AemProcessException;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.ipi.ProcessRunner;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.ipi.ProcessRunner.ListResult;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.ipi.ProcessRunner.ProcessRunnerException;

public class ShimFiles {
	private static final Logger log = LoggerFactory.getLogger(ShimFiles.class);
	private static final Set<PosixFilePermission> FILE_PERMISSIONS = EnumSet.of(OWNER_READ, OWNER_EXECUTE, GROUP_READ, GROUP_EXECUTE, OTHERS_READ, OTHERS_EXECUTE);

	private static final Path RUN_START = OperatingSystem.getOs().runStart();
	private static final Path RUN_STOP = OperatingSystem.getOs().runStop();

	/**
	 * Interface for creating {@link ShimFiles} instances during processing.  It only requires things that are not
	 * known at Spring Context initialization.
	 */
	@FunctionalInterface
	public static interface RuntimeFactory extends BiFunction<JavaVersion, Path, ShimFiles> {};

	public enum CreateType {
		NEW, EXISTING, NEW_OR_EXISTING;
	}
	
	private final JavaVersion aemJavaVersion;
	private final Path aemQuickstartJarDir;
	private final ProcessRunner processRunner;
	
	public ShimFiles(JavaVersion aemJavaVersion, Path aemQuickstartJarDir, ProcessRunner processRunner) {
		this.aemJavaVersion = aemJavaVersion;
		this.processRunner = processRunner;
		this.aemQuickstartJarDir = aemQuickstartJarDir;
	}
	
	private String getJavaEnv(Path aemQuickstartJarDir) {
		try {
			ListResult result = processRunner.runtoListResult(OperatingSystem.getOs().jbangCommand("jdk", "java-env", aemJavaVersion.getVersionString()), aemQuickstartJarDir).get();
			return result.stdout().stream().collect(Collectors.joining("\n"));
		} catch (ProcessRunnerException | InterruptedException | ExecutionException e) {
			throw new AemProcessException(e);
		}
	}
	
	public ShimFiles createBatFiles(CreateType createType) {
		try {
			Path startScriptPath = aemQuickstartJarDir.resolve(RUN_START);
			Path stopScriptPath = aemQuickstartJarDir.resolve(RUN_STOP);
			
			validateScriptFileExistence(createType, startScriptPath, stopScriptPath);
			String javaEnv = getJavaEnv(aemQuickstartJarDir);
			
			writeScript(startScriptPath, javaEnv + "\n" + aemQuickstartJarDir.resolve(AemFiles.START_SCRIPT).toString());
			writeScript(stopScriptPath, javaEnv + "\n" + aemQuickstartJarDir.resolve(AemFiles.STOP_SCRIPT).toString());
			return this;
		} catch (IOException | UnsupportedOperationException e) {
			throw new AemProcessException("Error while writing start/stop bat files to %s.".formatted(aemQuickstartJarDir), e);
		}
	}

	private static void validateScriptFileExistence(CreateType createType, Path startScriptPath, Path stopScriptPath)
			throws FileAlreadyExistsException, FileNotFoundException {
		boolean startScriptExists = Files.exists(startScriptPath);
		boolean stopScriptExists = Files.exists(stopScriptPath);
		boolean bothExist = startScriptExists && stopScriptExists;
		boolean bothAbsent = !startScriptExists && !stopScriptExists;
		boolean eitherExists = startScriptExists || stopScriptExists;
		
		if (createType == CreateType.NEW && eitherExists) {
				throw new FileAlreadyExistsException("Cannot create start/stop bat files in %s because %s."
								.formatted(startScriptPath.getParent(), bothExist ? "both files already exist" : (startScriptExists ? startScriptPath : startScriptPath) +  " already exists"));
		} else if (createType == CreateType.EXISTING && !bothExist) {
				throw new FileNotFoundException("Cannot create start/stop bat files in %s because %s."
								.formatted(startScriptPath.getParent(), bothAbsent ? "neither file exists" : (startScriptExists ? startScriptPath : startScriptPath) + " does not exist"));
		}
	}

	private static void writeScript(Path runStart, String script) throws IOException, UnsupportedOperationException {
		Files.writeString(runStart, script);
		if (OperatingSystem.isUnix()) {
			Files.setPosixFilePermissions(runStart, FILE_PERMISSIONS);
		}
	}

	public CompletableFuture<ListResult> startAem() throws InterruptedException, ExecutionException {
		log.atInfo().log("Starting AEM");
		return processRunner.runtoListResult(new String[] {aemQuickstartJarDir.resolve(RUN_START).toString()}, aemQuickstartJarDir);
//		if (process.exitCode() != 0) {
//			log.atError().addArgument(()->process.stdoutAsString()).log("Error occurred during AEM startup [STDOUT] {}");
//			log.atError().addArgument(()->process.stderrAsString()).log("Error occurred during AEM startup [STDERR] {}");
//		} else {
//			log.atDebug().addArgument(()->process.stdoutAsString()).log("AEM startup [STDOUT] {}");
//			log.atDebug().addArgument(()->process.stderrAsString()).log("AEM startup [STDERR] {}");
//		}
	}

	public CompletableFuture<ListResult> stopAem() throws InterruptedException, ExecutionException {
		log.atInfo().log("Stopping AEM");
		return processRunner.runtoListResult(new String[] {aemQuickstartJarDir.resolve(RUN_STOP).toString()}, aemQuickstartJarDir);
	}
}
