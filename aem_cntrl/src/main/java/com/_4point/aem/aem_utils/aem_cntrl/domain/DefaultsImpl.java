package com._4point.aem.aem_utils.aem_cntrl.domain;

import java.nio.file.Path;

/**
 * Utility class providing default values for AEM control operations.
 */
public class DefaultsImpl {

	private DefaultsImpl() {
		// Prevent instantiation
	}

	public static Path aemDir() {
		return OperatingSystem.isWindows() ? Path.of("\\Adobe") : Path.of("/opt/adobe");
	}
}
