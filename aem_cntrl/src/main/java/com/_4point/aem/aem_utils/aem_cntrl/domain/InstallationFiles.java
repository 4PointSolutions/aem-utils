package com._4point.aem.aem_utils.aem_cntrl.domain;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

abstract class InstallationFiles {

	protected static Path thereCanBeOnlyOne(List<Path> paths, String description) {
		if (paths.size() > 1) {
			throw new IllegalStateException("Found multiple %s files when there should only be one.".formatted(description));
		} else if (paths.size() < 1) {
			throw new IllegalStateException("Found no %s file when there should be one.".formatted(description));
		} else {
			return paths.getFirst();
		}
	}

	protected static Optional<Path> thereCanBeMaybeOne(List<Path> paths, String description) {
		if (paths.size() > 1) {
			throw new IllegalStateException("Found multiple %s files when there should at most be one.".formatted(description));
		} else if (paths.size() < 1) {
			return Optional.empty();
		} else {
			return Optional.of(paths.getFirst());
		}
	}

	protected static abstract class InstallationFile {
		protected static List<Path> findFilesMatching(Path dir, Pattern regex) {
			try {
				return Files.list(dir)
					 		.filter(p->matchesRegex(p, regex))
					 		.toList();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}		

		}

		private static boolean matchesRegex(Path p, Pattern regex) {
			return regex.matcher(p.getFileName().toString()).matches();
		}		
	}

	protected static Path copyToTarget(Path source, Path aemDir) {
		try {
			Path target = aemDir.resolve(source.getFileName());
			Files.copy(source, target);
			return target;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
