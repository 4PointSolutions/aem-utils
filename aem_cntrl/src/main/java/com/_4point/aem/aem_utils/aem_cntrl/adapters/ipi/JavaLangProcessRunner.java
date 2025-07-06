package com._4point.aem.aem_utils.aem_cntrl.adapters.ipi;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._4point.aem.aem_utils.aem_cntrl.domain.ports.ipi.ProcessRunner;

/**
 * Class for running system commands.  It handles creation of threads for handling the inputs and outputs.  The
 * user of this class just needs to supply lamdas to provide inputs and process outputs.
 * 
 * If the process does not read any inputs or produce any outputs that are important, then these handlers can be
 * omitted.
 * 
 * @param <O> The result of the stdout handler.
 * @param <E> The result of the stderr handler.
 */
public class JavaLangProcessRunner<O, E> implements ProcessRunner {
	private static final Logger log = LoggerFactory.getLogger(JavaLangProcessRunner.class);
	private static final ExecutorService EXECUTOR_SERVICE = Executors.newVirtualThreadPerTaskExecutor();

	private final Function<Stream<String>, O> outputStreamHandler; 
	private final Function<Stream<String>, E> errorStreamHandler;
	private final Supplier<Stream<String>> inputStreamHandler;
	
	public JavaLangProcessRunner(Function<Stream<String>, O> outputStreamHandler,
			Function<Stream<String>, E> errorStreamHandler, Supplier<Stream<String>> inputStreamHandler) {
		this.outputStreamHandler = outputStreamHandler != null ? outputStreamHandler : JavaLangProcessRunner::consumeStream;
		this.errorStreamHandler = errorStreamHandler != null ? errorStreamHandler : JavaLangProcessRunner::consumeStream;
		this.inputStreamHandler = inputStreamHandler != null ? inputStreamHandler : ()->Stream.empty();
	};

	/**
	 * Runs a command and waits for it to complete.   
	 * 
	 * @param processBuilder the ProcessBuilder to run.  This should be configured with the command and any arguments to be run.
	 * @param timeout the maximum time to wait for the process to complete.  If this timeout is exceeded, an exception will be thrown.
	 * @return exit code
	 */
	@Override
	public int runUntilCompletes(String command[], Path dir, Duration timeout) {
		RunningProcess<Stream<String>, Stream<String>> process = JavaLangProcessRunner.<Stream<String>, Stream<String>>builder()
				.setOutputStreamHandler(s->s)
				.setErrorStreamHandler(s->s)
				.build()
				.run(command, dir);
		log.atInfo().log("Running AEM with options");
		try {
			Stream<String> stdoutStream = process.stdout().get();
			CompletableFuture<Void> stdoutFuture = CompletableFuture.runAsync(()->stdoutStream.forEach(s->log.atDebug().log(s)), EXECUTOR_SERVICE);
			Stream<String> stderrStream = process.stderr().get();
			CompletableFuture<Void> stderrFuture = CompletableFuture.runAsync(()->stderrStream.forEach(s->log.atDebug().log(s)), EXECUTOR_SERVICE);
			int result = process.waitForCompletion();
			return result;
		} catch (InterruptedException | ExecutionException e) {
			throw new ProcessRunnerException(e);
		}
	}
	
	/**
	 * Runs a command.   
	 * 
	 * Note, the stdout and stderr Streams should be retrieved and joined with a terminal operation prior to 
	 * calling exit.join() otherwise the process may block if the stdout or stderr output is large enough to cause a block
	 * on the associated pipe.
	 * 
	 * Also note, an InputStreamSupplier must be provided if the command being run reads from stdin.  Failure to do so, may cause the process
	 * to hang and never terminate. 
	 * 
	 * @param command Command to be run
	 * @return Result object containing CompletableFutures for all the outputs from the process.
	 */
	public RunningProcess<O,E> run(String command[], Path dir) {
		try {
			Process process = new ProcessBuilder(command).directory(dir.toFile()).start();
			
			@SuppressWarnings("unused")
			CompletableFuture<Void> stdin = CompletableFuture.runAsync(()->copyFrom(inputStreamHandler, process.getOutputStream()), EXECUTOR_SERVICE);
			CompletableFuture<O> stdout = CompletableFuture.supplyAsync(()->this.outputStreamHandler.apply(process.inputReader().lines()), EXECUTOR_SERVICE);
			CompletableFuture<E> stderr = CompletableFuture.supplyAsync(()->this.errorStreamHandler.apply(process.errorReader().lines()), EXECUTOR_SERVICE);
			return new RunningProcess<O,E>(process, stdout, stderr);
		} catch (IOException e) {
			throw new ProcessRunnerException("Error while running process.", e);
		}
	}

	/**
	 * Results of running the command.  There are CompletableFutures for all the outputs (the exit code, stdout and stderr).
	 * 
	 * @param <O> The result of the stdout handler.
	 * @param <E> The result of the stderr handler.
	 */
	public static class RunningProcess<O, E> {
		private static final Duration DEFAULT_TERMINATION_WAIT_TIME = Duration.ofMinutes(5);
		
		private final Process process;
		private final CompletableFuture<O> stdout;
		private final CompletableFuture<E> stderr;
		
		private RunningProcess(Process process, CompletableFuture<O> stdout, CompletableFuture<E> stderr) {
			this.process = process;
			this.stdout = stdout;
			this.stderr = stderr;
		}

		public CompletableFuture<O> stdout() {
			return stdout;
		}

		public CompletableFuture<E> stderr() {
			return stderr;
		}

		public int waitForCompletion() throws InterruptedException {
			return process.waitFor();
		}

		public boolean isRunning() {
			return process.isAlive();
		}
		
		public OptionalInt terminateAfter(Duration timeout) throws InterruptedException {
			boolean waitResult = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
			if (waitResult) {
				// process terminated
				return OptionalInt.of(process.exitValue());
			} else {
				terminateProcess(timeout);
				return OptionalInt.empty();
			}
		}

		public void terminateProcess(Duration timeout) throws InterruptedException {
			// process not terminated, so terminate it.
//			processHandles().forEach(ph->terminateProcess(ph, timeout));	// Terminate All Processes - Everything shuts down ungracefully.
			terminateProcess(process.toHandle(), timeout);					// Terminate just the root process - AEm Does not shut down
		}

		private Stream<ProcessHandle> processHandles() {
			return Stream.concat(process.descendants().toList().reversed().stream(), Stream.of(process.toHandle()));
		}

		public record ProcessInfo(long pid, Optional<String> command, Optional<String> user) {
			public static ProcessInfo from(ProcessHandle handle) {
				ProcessHandle.Info info = handle.info();
				return new ProcessInfo(handle.pid(), info.command(), info.user());
			}
		}
		public Stream<ProcessInfo> allProcessIds() {
			return processHandles().map(ProcessInfo::from);
		}
		
		public void terminateProcess() throws InterruptedException {
			terminateProcess(DEFAULT_TERMINATION_WAIT_TIME);
		}
		
		public static void terminateProcess(ProcessHandle process, Duration timeout) {
			String cmdLine = process.info().command().orElse("== Command Line Unavailable (PID=%d) ==".formatted(process.pid()));
			if (!process.isAlive()) {
				log.atInfo()
				   .addArgument(cmdLine)
				   .log("Process already stopped. ({})");
				return;
			}
			log.atInfo()
			   .addArgument(cmdLine)
			   .log("Asking process to shut down. ({})");
			process.destroy();
			ProcessHandle destroyResult = process.onExit().completeOnTimeout(null, timeout.toMillis(), TimeUnit.MILLISECONDS).join();
			if (destroyResult == null) {
				log.atWarn()
				   .addArgument(cmdLine)
				   .log("Process did not shut down voluntarily. Terminating it forceably. ({})");
				// Didn't terminate when asked nicely, so forcibly terminate
				process.destroyForcibly();
				ProcessHandle forceResult = process.onExit().completeOnTimeout(null, timeout.toMillis(), TimeUnit.MILLISECONDS).join();
				if (forceResult == null) {
					// Didn't terminate forcibly, so alert the user.
					String msg = "Unable to terminate process (%s)".formatted(cmdLine);
					log.atError().log(msg);
					throw new IllegalStateException(msg);
				}
			} else {
				log.atInfo()
				   .addArgument(cmdLine)
				   .log("Process shut down after destroy. ({})");
			}
			log.atInfo()
			   .addArgument(cmdLine)
			   .log("Process shut down. ({})");
			
		}
	}

	public static <O, E> Builder<O, E> builder() {
		return new Builder<O, E>();
	}
	
	private static <T> T consumeStream(Stream<String> inStream) {
		@SuppressWarnings("unused")
		Optional<String> any = inStream.filter(s->false).findAny();
		return null;
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
	@Override
	public CompletableFuture<ListResult> runtoListResult(String command[], Path dir, Supplier<Stream<String>> inputSupplier) throws ProcessRunnerException {
		ProcessBuilder processBuilder = new ProcessBuilder(command).directory(dir.toFile());
		try {
			Process process = processBuilder.start();
			CompletableFuture<Void> stdin = CompletableFuture.runAsync(()->copyFrom(inputSupplier, process.getOutputStream()));
			CompletableFuture<List<String>> stdout = CompletableFuture.supplyAsync(()->process.inputReader().lines().toList());
			CompletableFuture<List<String>> stderror = CompletableFuture.supplyAsync(()->process.errorReader().lines().toList());
			return CompletableFuture.supplyAsync(()->runProcess(process, stdout, stderror, stdin));
		} catch (IOException e) {
			throw new ProcessRunnerException("Error while running process.", e);
		}
	}

	private static void copyFrom(Supplier<Stream<String>> inputSupplier, OutputStream os) {
		try(var myos = os; var writer = new PrintWriter(os)) {
			inputSupplier.get().forEach(writer::println);
		} catch (IOException e) {
			throw new UncheckedIOException("Error when closing stdin.", e);
		}
	}
	
	private static ListResult runProcess(Process process, CompletableFuture<List<String>> stdout, CompletableFuture<List<String>> stderr, CompletableFuture<Void> stdin) {
		try {
			return new ListResult(process.waitFor(), stdout.get(), stderr.get(), stdin.get());
		} catch (InterruptedException | ExecutionException e) {
			throw new ProcessRunnerException("Error while running process.", e);
		}
	}
	
	/**
	 * A Builder that creates a ProcessRunner instance.
	 * 
	 * 
	 * @param <O> The result of the stdout handler.
	 * @param <E> The result of the stderr handler.
	 */
	public static class Builder<O, E> {
		private Function<Stream<String>, O> outputStreamHandler; 
		private Function<Stream<String>, E> errorStreamHandler;
		private Supplier<Stream<String>> inputStreamHandler;

		/**
		 * Adds an OutputStreamHandler
		 * 
		 * The OuotputStreamHandler will process the stdout output from the process.  If omitted, the
		 * stdout output is discarded.
		 * 
		 * @param outputStreamHandler
		 * @return
		 */
		public Builder<O, E> setOutputStreamHandler(Function<Stream<String>, O> outputStreamHandler) {
			this.outputStreamHandler = outputStreamHandler;
			return this;
		}

		/**
		 * Adds an ErrorStreamHandler
		 * 
		 * The ErrorStreamHandler will process the stderr output from the process.  If omitted, the
		 * stderr output is discarded.
		 * 
		 * @param errorStreamHandler
		 * @return
		 */
		public Builder<O, E> setErrorStreamHandler(Function<Stream<String>, E> errorStreamHandler) {
			this.errorStreamHandler = errorStreamHandler;
			return this;
		}

		/**
		 * Adds an InputStreamSupplier
		 * 
		 * The InputStreamSupplier must provide a Stream>String> that will be read by the running process.
		 * This is an optional parameter however if the process tries to read from stdin and this has *not*
		 * been provided, then the process will hang.  
		 * 
		 * @param inputStreamHandler
		 * @return
		 */
		public Builder<O, E> setInputStreamHandler(Supplier<Stream<String>> inputStreamHandler) {
			this.inputStreamHandler = inputStreamHandler;
			return this;
		}
		
		/**
		 * Builds a ProcessRunner
		 * 
		 * @return the ProcessRunner, ready to be run.
		 */
		public JavaLangProcessRunner<O, E> build() {
			return new JavaLangProcessRunner<>( 
						outputStreamHandler, 
						errorStreamHandler, 
						inputStreamHandler);
		}
	}
}
