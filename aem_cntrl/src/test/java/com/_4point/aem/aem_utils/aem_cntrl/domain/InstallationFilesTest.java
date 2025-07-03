package com._4point.aem.aem_utils.aem_cntrl.domain;

import static com._4point.testing.matchers.javalang.ExceptionMatchers.exceptionMsgContainsAll;
import static org.hamcrest.MatcherAssert.assertThat; 
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstallationFilesTest {
	
	private static final String SHOULD_AT_MOST_BE_ONE = "should at most be one";
	private static final String SHOULD_ONLY_BE_ONE = "should only be one";
	private static final String SHOULD_BE_ONE = "should be one";
	private static final String FOUND_MULTIPLE = "Found multiple ";
	private static final String FOUND_NO = "Found no ";
	private static final List<Path> EMPTY_LIST = List.of();
	private static final List<Path> LIST_OF_ONE = List.of(Path.of("foo"));
	private static final List<Path> LIST_OF_TWO = List.of(Path.of("foo"), Path.of("bar"));
	private static final String TEST_DESCRIPTION = "Test Description";


	@Test
	void testThereCanBeOnlyOne_OneFile() {
		Path result = InstallationFiles.thereCanBeOnlyOne(LIST_OF_ONE, TEST_DESCRIPTION);
		assertSame(LIST_OF_ONE.getFirst(), result);
	}

	@Test
	void testThereCanBeMaybeOne_OneFile() {
		Path result = InstallationFiles.thereCanBeMaybeOne(LIST_OF_ONE, TEST_DESCRIPTION).orElseThrow();
		assertSame(LIST_OF_ONE.getFirst(), result);
	}

	@Test
	void testThereCanBeOnlyOne_NoFile() {
		IllegalStateException ex = assertThrows(IllegalStateException.class, ()->InstallationFiles.thereCanBeOnlyOne(EMPTY_LIST, TEST_DESCRIPTION));
		assertThat(ex, exceptionMsgContainsAll(TEST_DESCRIPTION, FOUND_NO, SHOULD_BE_ONE));
	}

	@Test
	void testThereCanBeMaybeOne_NoFile(@TempDir Path dir) {
		Optional<Path> result = InstallationFiles.thereCanBeMaybeOne(EMPTY_LIST, TEST_DESCRIPTION);
		assertTrue(result.isEmpty());
	}

	@Test
	void testThereCanBeOnlyOne_ManyFiles() {
		IllegalStateException ex = assertThrows(IllegalStateException.class, ()->InstallationFiles.thereCanBeOnlyOne(LIST_OF_TWO, TEST_DESCRIPTION));
		assertThat(ex, exceptionMsgContainsAll(TEST_DESCRIPTION, FOUND_MULTIPLE, SHOULD_ONLY_BE_ONE));
	}

	@Test
	void testThereCanBeMaybeOne_ManyFiles() {
		IllegalStateException ex = assertThrows(IllegalStateException.class, ()->InstallationFiles.thereCanBeMaybeOne(LIST_OF_TWO, TEST_DESCRIPTION));
		assertThat(ex, exceptionMsgContainsAll(TEST_DESCRIPTION, FOUND_MULTIPLE, SHOULD_AT_MOST_BE_ONE));
	}

}
