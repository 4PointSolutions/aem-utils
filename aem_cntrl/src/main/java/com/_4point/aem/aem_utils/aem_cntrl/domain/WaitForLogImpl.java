package com._4point.aem.aem_utils.aem_cntrl.domain;

import java.nio.file.Path;
import java.time.Duration;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.AemDir;
import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.LogFile;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.WaitForLog;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.WaitForLog.RegexArgument.RegexCustom;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.WaitForLog.RegexArgument.RegexShutdown;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.WaitForLog.RegexArgument.RegexStartup;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer.TailerFactory;

public class WaitForLogImpl implements WaitForLog {
	private static final Logger log = LoggerFactory.getLogger(WaitForLogImpl.class);
	
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
		Path finalAemDir = aemDir.toQualified(unqualifiedAemDir);									// find the aem directory
		LogFile logFile = logFileFactory.apply(finalAemDir, tailerFactory);							// locate the log file and create the log file object
		log.info("Waiting for {} log entry in log {} within {}", toString(regexArgument), logFile.toString(), timeout);
		logFile.monitorLogFile(toPattern(regexArgument), timeout, toLogFileFromOption(fromOption))	// monitor the log file for a matching entry, throw an exception if not found
			   .orElseThrow(() -> new WaitForLogException("No log entry matching " + toPattern(regexArgument) + " found in " + logFile.toString() + " within " + timeout));
		log.info("Found {} log entry in {}", toString(regexArgument), logFile.toString());
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

	private String toString(RegexArgument regexArgument) {
         return switch (regexArgument) {
             case RegexStartup r -> "Startup";
             case RegexShutdown r -> "Shutdown";
             case RegexCustom r -> "Custom Regex: '" + r.regex() + "'";
         };
     }
}
