package com._4point.aem.aem_utils.aem_cntrl.adapters.spi;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.assertThat; 
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer;
import com._4point.testing.matchers.javalang.ExceptionMatchers;

class CommonsIoTailerTailerTest {
	
	private static final String LOG_MESSAGE_FORMAT_STR = "This is log line #%d.";
	private static final Tailer.TailerFactory tailerFactory = new CommonsIoTailerTailer.TailerFactory();

	enum ExpectedResult {
		FindsLine, TimesOut;
	}

	enum TestScenario {
		FromBeginning_AlreadyWritten(tailerFactory::fromBeginning, 2, ExpectedResult.FindsLine),
		FromBeginning_NotYetWritten(tailerFactory::fromBeginning, 25, ExpectedResult.FindsLine),
		FromBeginning_NeverWritten(tailerFactory::fromBeginning, 500000, ExpectedResult.TimesOut),
		FromEnd_AlreadyWritten(tailerFactory::fromEnd, 2, ExpectedResult.TimesOut),
		FromEnd_NotYetWritten(tailerFactory::fromEnd, 25, ExpectedResult.FindsLine),
		FromEnd_NeverWritten(tailerFactory::fromEnd, 500000, ExpectedResult.TimesOut),
			;

		private final Function<Path, Tailer> factory;
		private final int targetLine;
		private final ExpectedResult expectedResult;

		private TestScenario(Function<Path, Tailer> factory, int targetLine, ExpectedResult expectedResult) {
			this.factory = factory;
			this.targetLine = targetLine;
			this.expectedResult = expectedResult;
		}
	}
	
	@ParameterizedTest
	@EnumSource
	void testStream_findsLine(TestScenario scenario, @TempDir Path tempDir) throws Exception {
		String targetLine = LOG_MESSAGE_FORMAT_STR.formatted(scenario.targetLine);
		switch(scenario.expectedResult) {
			case FindsLine -> {
				String result = runTest(targetLine, tempDir.resolve("logfile"), true, scenario.factory);
				assertNotNull(result);
				assertEquals(targetLine, result);
				}
			case TimesOut -> {
				CompletionException ex = assertThrows(CompletionException.class, ()->runTest(targetLine, tempDir.resolve("logfile"), false, scenario.factory));
				assertThat(ex, ExceptionMatchers.hasCauseMatching(instanceOf(TimeoutException.class)));
			}
		}
//		Files.lines(logFile).forEach(System.out::println);
		
	}

	private String runTest(String targetLine, Path logFile, boolean debug, Function<Path, Tailer> factory) throws InterruptedException, Exception {
		try (var handle = LogFileWriter.start(logFile, Duration.ofMillis(100), debug)) {
			Thread.sleep(Duration.ofSeconds(1));	// Accumulate some log entries before the tailing starts
			
			try(Tailer tailer = factory.apply(logFile)) {
				String resultLine = CompletableFuture.supplyAsync(
						()->tailer.stream()
								  .map(s->{
									  		if (debug) {
									  			System.out.println("[%s] Found '%s' in log".formatted(Thread.currentThread().getName(), s));
									  		}
									  		return s;
								          })
								  .filter(s->s.equals(targetLine))
								  .findFirst()
								  .orElseThrow()
					).orTimeout(5, TimeUnit.SECONDS).join();
				return resultLine;
			}		
		}
	}

	private static class LogFileWriter {
		private final AtomicBoolean stop = new AtomicBoolean(false);
		private final Path path;
		private final Duration delay;
		private final boolean writeDebug;

		private LogFileWriter(Path path, Duration delay, boolean writeDebug) {
			this.path = path;
			this.delay = delay;
			this.writeDebug = writeDebug;
		}
		
		private LogFileWriter(Path path, Duration delay) {
			this(path, delay, false);
		}
		
		public LogFileWriterHandle start() {
			stop.set(false);
			return new LogFileWriterHandle(CompletableFuture.runAsync(this::mainLoop));
		}
		
		private void stop() {
			stop.set(true);
		}
		
		private void mainLoop() {
			try (LogLineWriter writer = LogLineWriter.from(path, writeDebug)) {
				while (stop.get() == false) {
					writer.writeLine();
					Thread.sleep(delay);
				}
			} catch (Exception  e) {
				throw new IllegalStateException("Error occurred in mainLoop", e);
			}
		}

		public static LogFileWriter create(Path filePath, Duration delay) {
			return new LogFileWriter(filePath, delay);
		}
		
		public static LogFileWriterHandle start(Path filePath, Duration delay) {
			return create(filePath, delay).start();
		}
		
		public static LogFileWriter create(Path filePath, Duration delay, boolean debug) {
			return new LogFileWriter(filePath, delay, debug);
		}
		
		public static LogFileWriterHandle start(Path filePath, Duration delay, boolean debug) {
			return create(filePath, delay, debug).start();
		}
		
		public class LogFileWriterHandle implements AutoCloseable {
			private final CompletableFuture<Void> completableFuture;

			private LogFileWriterHandle(CompletableFuture<Void> completableFuture) {
				this.completableFuture = completableFuture;
			}
			
			public void close() {
				LogFileWriter.this.stop();
				try {
					completableFuture.get();
				} catch (InterruptedException | ExecutionException e) {
					throw new IllegalStateException("Error occurred when stopping mainLoop", e);
				}
			}
		}

		// Writes numbered lines out to a file.
		private static class LogLineWriter implements AutoCloseable {
			private final PrintWriter pw;
			private final boolean debug;
			private int counter;
			
			private LogLineWriter(PrintWriter pw, boolean debug) {
				this.pw = pw;
				this.debug = debug;
				this.counter = 0;
			}

			// Write out line to log file
			public void writeLine() {
				String lineToWrite = LOG_MESSAGE_FORMAT_STR.formatted(++counter);
				if (debug) {
					System.out.println("Wrote '%s' to log file".formatted(lineToWrite));
				}
				pw.println(lineToWrite);
				pw.flush();
			}

			@Override
			public void close() throws Exception {
				if (debug) { System.out.println("Closing LogWriter"); }
				pw.close();
			}
			
			public static LogLineWriter from(Path p, boolean debug) throws IOException {
				return new LogLineWriter(new PrintWriter(Files.newBufferedWriter(p)), debug);
			}
		}
	}
	
}
