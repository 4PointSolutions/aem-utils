package com._4point.aem.aem_utils.aem_cntrl.domain;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.AemDir;
import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.AemDir.AemDirType;
import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.LogFile;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.WaitForLog;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer.TailerFactory;

public class WaitForLogImpl implements WaitForLog {
	
	private final AemDir aemDir;
	private final TailerFactory tailerFactory;
	private final BiFunction<Path, TailerFactory, LogFile> logFileFactory;
	
	public WaitForLogImpl(AemDir aemDir, TailerFactory tailerFactory, BiFunction<Path, TailerFactory, LogFile> logFileFactory) {
		this.aemDir = aemDir;
		this.tailerFactory = tailerFactory;
		this.logFileFactory = logFileFactory;
	}


	/**
	 * Waits for a log entry matching the specified regex in the AEM log files.
	 * 
	 * @param regexArgument The regex to match against log entries.
	 * @param timeout       The maximum time to wait for a matching log entry.
	 * @param fromOption          The source of the log (e.g., "error", "info").
	 * @param aemDir        The directory where AEM logs are stored. If null, the
	 *                      default AEM directory is used.
	 */
	@Override
	public void waitForLog(RegexArgument regexArgument, Duration timeout, FromOption fromOption, final Path unqualifiedAemDir) {
		internalWaitForLog(regexArgument, timeout, fromOption, aemDir.toQualified(unqualifiedAemDir));
	}

	private String internalWaitForLog(RegexArgument regexArgument, Duration timeout, FromOption from, final Path finalAemDir) {
		// find the log file
		LogFile logFile = logFileFactory.apply(finalAemDir, tailerFactory);
		
		
		System.out.println("Path: " + finalAemDir + ", is " + AemDirType.of(finalAemDir).toString() + ", log file: " + logFile);
		
		Optional<String> log =logFile.monitorLogFile(toPattern(regexArgument), timeout, toLogFileFromOption(from));
		
		return log.orElseThrow(() -> new WaitForLogException("No log entry matching " + toPattern(regexArgument) + " found in " + logFile + " within " + timeout));
	}

	private LogFile.FromOption toLogFileFromOption(FromOption from) {
		return switch (from) {
			case START -> LogFile.FromOption.START;
			case END -> LogFile.FromOption.END;
		};
	}

	private Pattern toPattern(RegexArgument regexArgument) {
		return switch (regexArgument) {
			case RegexArgument.RegexStartup r -> AemProcess.AEM_START_TARGET_PATTERN;
			case RegexArgument.RegexShutdown r -> AemProcess.AEM_STOP_TARGET_PATTERN;
			case RegexArgument.RegexCustom r -> r.regex();
		};
	}

	

}
