package com._4point.aem.aem_utils.aem_cntrl.domain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.SystemUtils;

public enum MockInstallFiles {
	SAMPLE_AEM_ORIG_QUICKSTART_PATH("AEM_6.5_Quickstart.jar"),
	SAMPLE_AEM_LTS_QUICKSTART_PATH("cq-quickstart-6.6.0.jar"),
	SAMPLE_AEM_SERVICE_PACK_PATH("aem-service-pkg-6.5.19.0.zip"),
	SAMPLE_AEM_FORMS_ADDON_PATH("adobe-aemfd-" + getAbbrev() + "-pkg-6.0.1120.zip"),
	SAMPLE_LICENSE_PROPERTIES_PATH("license.properties"),
	SECOND_AEM_FOMRS_ADDON_PATH("adobe-aemfd-" + getAbbrev() + "-pkg-6.0.9999.zip"),
	;

	private final Path filename;
	
	private MockInstallFiles(Path filename) {
		this.filename = filename;
	}

	private MockInstallFiles(String filename) {
		this(Path.of(filename));
	}
	
	/**
	 * Returns the name of the mock install file.
	 * 
	 * @return
	 */
	public Path filename() {
		return filename;
	}

	private static String getAbbrev() {
		if (SystemUtils.IS_OS_WINDOWS) {
			return "win";
		} else if (SystemUtils.IS_OS_LINUX) {
			return "linux";
		} else if (SystemUtils.IS_OS_MAC) {
			return "macos";
		} else {
			throw new IllegalStateException("Unsupported Operating System (%s)".formatted(SystemUtils.OS_NAME));
		}
	}
	
	public Path createMockFile(Path rootDir) throws IOException {
		Path testFilePath = rootDir.resolve(filename);
		Files.createFile(testFilePath);
		return testFilePath;
	}
}
