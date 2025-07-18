package com._4point.aem.aem_utils.aem_cntrl.commands;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.Defaults;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.WaitForLog;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "waitforlog", aliases = {"wflog"}, description = "Waits for a message matching a regular expression to appear in the AEM error.log file.")
public class WaitForLogCommand  implements Callable<Integer> {

	@ArgGroup(exclusive = true)
	RegexOptions regexOptions;
	
	static class RegexOptions {
		@Option(names = {"-re", "--regex"}, description = "regular expression to wait for") String regEx;
		@Option(names = {"-su", "--startup"}, description = "wait for AEM to start") boolean startup;
		@Option(names = {"-sd", "--shutdown"}, description = "wait for AEM to shutdown") boolean shutdown;
	}
	
	@Option(names = {"-ad", "--aemdir"}, description = "the AEM directory (i.e. the directory containing a crx-quickstart subdirectory)")
	Path aemDir;

	@Option(names = {"-t", "--timeout"}, description = "timeout value (in ISO-8601 duration format PnDTnHnMn.nS). Defaults to PT10M, i.e. a 10 minute timeout.")
	String timeout;

	@ArgGroup(exclusive = true)
	FromOptions fromOptions;
	
	static class FromOptions {
		@Option(names = {"-fs", "--fromStart"}, description = "if specified, start from searching from the start of the log") boolean fromStart;
		@Option(names = {"-fe", "--fromEnd"}, description = "if specified, start from searching from the end of the log (this is the default)") boolean fromEnd;
    }

	private final WaitForLog waitForLog;
	private final Defaults defaults;
	
	public WaitForLogCommand(WaitForLog waitForLog, Defaults defaults) {
		this.waitForLog = waitForLog;
		this.defaults = defaults;
	}


	@Override
	public Integer call() throws Exception {
		System.out.println("Wait For Log called.");
		return 0;
	}
}
