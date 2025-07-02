package com._4point.aem.aem_utils.aem_cntrl.domain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com._4point.aem.aem_utils.aem_cntrl.domain.ProcessRunner.ListResult;
import com._4point.aem.aem_utils.aem_cntrl.domain.ProcessRunner.RunningProcess;


class ProcessRunnerTest {

	private static final String LOREM_IPSUM_LOC = Path.of("com", "_4point", "aem", "aem_utils", "aem_cntrl", "domain", "LoremIpsumGenerator.java").toString();
	private static final ProcessBuilder PROCESS_BUILDER = new ProcessBuilder(OperatingSystem.getOs().jbangCommand(LOREM_IPSUM_LOC))
																	.directory(Path.of("src","test","java").toFile());
	private static final ProcessBuilder PROCESS_BUILDER_WITH_INPUT = new ProcessBuilder(OperatingSystem.getOs().jbangCommand(LOREM_IPSUM_LOC, "useInput"))
																	.directory(Path.of("src","test","java").toFile());

	@Test
	void testRunToList_NoInput() throws Exception {
		ListResult result = ProcessRunner.runtoListResult(PROCESS_BUILDER).join();
		
		assertAll(
				()->assertThat("Results should not be empty", result.stdout(), is(not(empty()))),
				()->assertThat("Errors should be empty", result.stderr(), either(emptyCollectionOf(String.class)).or(hasItem(containsStringIgnoringCase("Building jar for LoremIpsumGenerator.java")))),
				()->assertEquals(0, result.exitCode(), "No errors should be detected")
				);
	}

	@Test
	void testRunToList_WithInput() throws Exception {
		String expectedTestInput = "test input string";
		ListResult result = ProcessRunner.runtoListResult(PROCESS_BUILDER_WITH_INPUT, ()->Stream.of(expectedTestInput)).join();
		
		assertAll(
				()->assertThat("Results should contain expected output", result.stdout(), hasItem(containsString(expectedTestInput))),
				()->assertThat("Errors should be empty", result.stderr(), either(emptyCollectionOf(String.class)).or(hasItem(containsStringIgnoringCase("Building jar for LoremIpsumGenerator.java")))),
				()->assertEquals(0, result.exitCode(), "No errors should be detected")
				);
	}

	@Test
	void testRun_NoInput_NoOutputRead() throws Exception {
		RunningProcess<Void, Void> result = ProcessRunner.<Void, Void>builder() 
										 		 .build()
										 		 .run(PROCESS_BUILDER);
		
		Integer exitCode = result.waitForCompletion();
		
		assertAll(
				()->assertEquals(0, exitCode, "No errors should be detected")
				);
	}

	@Test
	void testRun_NoInput_OutputReadOnly() throws Exception {
		RunningProcess<List<String>, Void> result = ProcessRunner.<List<String>, Void>builder()
													 	 .setOutputStreamHandler(s->s.toList())
													 	 .build()
													 	 .run(PROCESS_BUILDER);
		
		List<String> stdout = result.stdout().join();
		Integer exitCode = result.waitForCompletion();
		
		assertAll(
				()->assertThat("Results should not be empty", stdout, is(not(empty()))),
				()->assertEquals(0, exitCode, "No errors should be detected")
				);
	}

	@Test
	void testRun_NoInput_OutputAndErrorRead() throws Exception {
		RunningProcess<List<String>, List<String>> result = ProcessRunner.<List<String>, List<String>>builder()
																.setOutputStreamHandler(s->s.toList())
																.setErrorStreamHandler(s->s.toList())
																.build()
																.run(PROCESS_BUILDER);
		
		List<String> stdout = result.stdout().join();
		List<String> stderr = result.stderr().join();
		Integer exitCode = result.waitForCompletion();
		
		assertAll(
				()->assertThat("Results should not be empty", stdout, is(not(empty()))),
				()->assertThat("Errors should be empty", stderr, either(emptyCollectionOf(String.class)).or(hasItem(containsStringIgnoringCase("Building jar for LoremIpsumGenerator.java")))),
				()->assertEquals(0, exitCode, "No errors should be detected")
				);
	}

	@Test
	void testRun_WithInput_NoOutputRead() throws Exception {
		String expectedTestInput = "test input string";
		RunningProcess<Void, Void> result = ProcessRunner.<Void, Void>builder() 
												 .setInputStreamHandler(()->Stream.of(expectedTestInput))
										 		 .build()
										 		 .run(PROCESS_BUILDER);
		
		Integer exitCode = result.waitForCompletion();
		
		assertAll(
				()->assertEquals(0, exitCode, "No errors should be detected")
				);
	}

	@Test
	void testRun_WithInput_OutputReadOnly() throws Exception {
		String expectedTestInput = "test input string";
		RunningProcess<List<String>, Void> result = ProcessRunner.<List<String>, Void>builder()
													 	 .setOutputStreamHandler(s->s.toList())
													 	 .setInputStreamHandler(()->Stream.of(expectedTestInput))
													 	 .build()
													 	 .run(PROCESS_BUILDER_WITH_INPUT);
		
		List<String> stdout = result.stdout().join();
		Integer exitCode = result.waitForCompletion();
		
		assertAll(
				()->assertThat("Results should contain expected output", stdout, hasItem(containsString(expectedTestInput))),
				()->assertEquals(0, exitCode, "No errors should be detected")
				);
	}

	@Test
	void testRun_WithInput_OutputAndErrorRead() throws Exception {
		String expectedTestInput = "test input string";
		RunningProcess<List<String>, List<String>> result = ProcessRunner.<List<String>, List<String>>builder()
																.setOutputStreamHandler(s->s.toList())
																.setErrorStreamHandler(s->s.toList())
																.setInputStreamHandler(()->Stream.of(expectedTestInput))
																.build()
																.run(PROCESS_BUILDER_WITH_INPUT);
		
		List<String> stdout = result.stdout().join();
		List<String> stderr = result.stderr().join();
		Integer exitCode = result.waitForCompletion();
		
		assertAll(
				()->assertThat("Results should contain expected output", stdout, hasItem(containsString(expectedTestInput))),
				()->assertThat("Errors should be empty", stderr, either(emptyCollectionOf(String.class)).or(hasItem(containsStringIgnoringCase("Building jar for LoremIpsumGenerator.java")))),
				()->assertEquals(0, exitCode, "No errors should be detected")
				);
	}


	@Test
	void testRun_WithInput() throws Exception {
		String expectedTestInput = "test input string";
		ListResult result = ProcessRunner.runtoListResult(PROCESS_BUILDER_WITH_INPUT, ()->Stream.of(expectedTestInput)).join();
		
		assertAll(
				()->assertThat("Results should contain expected output", result.stdout(), hasItem(containsString(expectedTestInput))),
				()->assertThat("Errors should be empty", result.stderr(), either(emptyCollectionOf(String.class)).or(hasItem(containsStringIgnoringCase("Building jar for LoremIpsumGenerator.java")))),
				()->assertEquals(0, result.exitCode(), "No errors should be detected")
				);
	}

	@EnabledOnOs(OS.WINDOWS) // Doesn't block on Linux
	@Test
	void testRun_NoInput_OutputReadOnly__BlocksAndDoesNotComplete() throws Exception {
		RunningProcess<List<String>, Void> result = ProcessRunner.<List<String>, Void>builder()
													 	 .setOutputStreamHandler(s->s.limit(1).toList())	// Shortcutted, so stdout is not all read.
													 	 .build()
													 	 .run(PROCESS_BUILDER);
		
		List<String> stdout = result.stdout().join();
		OptionalInt exitResult = result.terminateAfter(Duration.ofSeconds(2)); // Process will block and timeout because the output is not being read from the pipe
		
		assertTrue(exitResult.isEmpty(), ()->"Expected process to have no exit code, but it had (%d).".formatted(exitResult.orElseThrow()));
	}

	@EnabledOnOs(OS.WINDOWS) // Doesn't block on Linux
	@Test
	void testRun_NoInput_OutputReadOnly_BlocksAndFinishes() throws Exception {
		RunningProcess<Stream<String>, Void> result = ProcessRunner.<Stream<String>, Void>builder()
													 	 .setOutputStreamHandler(s->s)
													 	 .build()
													 	 .run(PROCESS_BUILDER);
		
		CompletableFuture<OptionalInt> runner = CompletableFuture.supplyAsync(()->{
																					try {
																						return result.terminateAfter(Duration.ofSeconds(2));
																					} catch (InterruptedException e) {
																						throw new IllegalStateException("Interruption Exception should not be thrown.", e);
																					}
																				});	// Process would block however we're going to unblock it.
		Thread.sleep(Duration.ofSeconds(1));
		List<String> stdout = result.stdout().join().toList();					// Reading will unblock it.
		OptionalInt exitResult = runner.join();
		
		assertAll(
				()->assertThat("Results should not be empty", stdout, is(not(empty()))),
				()->assertEquals(0, exitResult.orElseThrow(), "No errors should be detected")
				);
	}

}
