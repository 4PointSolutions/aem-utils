package com._4point.aem.aem_utils.aem_cntrl.domain.ports.ipi;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ProcessRunner {
	
	/**
	 * Runs a command and waits for it to complete.   
	 * 
	 * @param command the command to be run (e.g. "ls -l")
	 * @param directory the directory in which to run the command.
	 * @param timeout the maximum time to wait for the process to complete.  If this timeout is exceeded, an exception will be thrown.
	 * @return exit code
	 */
	/**
	 * @param timeout
	 * @return
	 */
	public int runUntilCompletes(String command[], Path directory, Duration timeout);

	/**
	 * Runs a command and accumulates the output (stdot and stderr) into List<String> objects.
	 * 
	 * Note, the command must not read from stdin.  If it does, this method will hang because the process will be waiting for input 
	 * on stdin that never arrives.  Use runtoListResult(ProcessBuilder command, Supplier<Stream<String>> inputSupplier) instead.
	 * 
	 * Since this stores the output within an in-memory list, a large output could take up a lot of memory. If this is 
	 * unacceptable, use run() instead.
	 * 
	 * @param command command to be executed
	 * @return CompletableFutur containing ListResult object.  ListResult contains the exit code, stdout and stderr outputs.  
	 * @throws ProcessRunnerException
	 */
	public default CompletableFuture<ListResult> runtoListResult(String command[], Path directory) throws ProcessRunnerException {
		return runtoListResult(command, directory, ()->Stream.empty());
	}	

	/**
	 * Runs a command and accumulates the output (stdot and stderr) into List<String> objects.
	 * 
	 * Note, this mothod supports commands that reas from stdin.  
	 * 
	 * Since this stores the output within an in-memory list, a large output could take up a lot of memory. If this is 
	 * unacceptable, use run() instead.
	 * 
	 * @param command command command to be executed
	 * @param inputSupplier a Supplier that provides input to the command as a Stream<String>
	 * @return CompletableFutur containing ListResult object.  ListResult contains the exit code, stdout and stderr outputs.  
	 * @throws ProcessRunnerException
	 */
	public CompletableFuture<ListResult> runtoListResult(String command[], Path directory, Supplier<Stream<String>> inputSupplier) throws ProcessRunnerException;
	
	/**
	 * Result of "asList" methods.
	 */
	public static record ListResult(int exitCode, List<String> stdout, List<String> stderr) {

		// This is just a convenience method to make the code in runProcess() a little cleaner.
		public ListResult(int exitCode, List<String> stdout, List<String> stderr, Void stdin) {
			this(exitCode, stdout, stderr);	// throw away the stdin value.
		}
		
		public String stdoutAsString() {
			return listAsString(stdout);
		}
		
		public String stderrAsString() {
			return listAsString(stderr);
		}
		
		private String listAsString(List<String> strings) {
			return strings.stream().collect(Collectors.joining("\n"));
		}
	}

	/**
	 * Checked Exceptions that occur while running are captured and wrapped in the ProcessRunnderException object.
	 */
	@SuppressWarnings("serial")
	public static class ProcessRunnerException extends RuntimeException {

		public ProcessRunnerException() {
		}

		public ProcessRunnerException(String message, Throwable cause) {
			super(message, cause);
		}

		public ProcessRunnerException(String message) {
			super(message);
		}

		public ProcessRunnerException(Throwable cause) {
			super(cause);
		}
	}
}
