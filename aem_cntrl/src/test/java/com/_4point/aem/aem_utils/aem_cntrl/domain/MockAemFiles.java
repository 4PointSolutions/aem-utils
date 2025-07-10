package com._4point.aem.aem_utils.aem_cntrl.domain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * This enum is used to represent the mock AEM files that are used for testing purposes.
 * The represent files created by installing AEM
 * 
 */
public enum MockAemFiles {
	SLING_PROPERTIES(Path.of("conf", "sling.properties"), MockAemFiles.SAMPLE_SLING_PROPERTIES),
	;

	private static final String SAMPLE_SLING_PROPERTIES = 
			"""
			#Overlay properties for configuration
			#Wed Apr 02 09:40:35 EDT 2025
			sling.bootdelegation.sun=sun.*,com.sun.*
			org.osgi.framework.system.capabilities.extra=${org.apache.sling.launcher.system.capabilities.extra}
			sling.framework.install.startlevel=1
			""";

	private final Path filename;
	private final String contents;

	private MockAemFiles(Path filename, String contents) {
		this.filename = filename;
		this.contents = contents;
	}
	
	public Path createMockFile(Path aemQuickstartDir) throws IOException {
		createParentDir(aemQuickstartDir, filename.getParent());
		return copyContentsToFile(aemQuickstartDir.resolve(filename), contents);
	}

	public Path filename() {
		return filename;
	}

	public String contents() {
		return contents;
	}

	private static Path copyContentsToFile(Path testFilePath, String contents) throws IOException {
		Files.writeString(testFilePath, contents, StandardOpenOption.CREATE_NEW);
		return testFilePath;
	}

	private static Path createParentDir(Path aemQuickstartDir, Path parent) throws IOException {
		if (parent != null) {
			Path parentDir = aemQuickstartDir.resolve(parent);
			if (!Files.exists(parentDir)) {
				Files.createDirectories(parentDir);
			}
		}
		return parent;
	}
}
