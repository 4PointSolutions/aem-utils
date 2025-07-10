package com._4point.aem.aem_utils.aem_cntrl.domain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;

/**
 * These are mock installation files that are used for testing.  The simulate the files that come from Adobe and will be used to install AEM.  
 */
public enum MockInstallFiles {
	SAMPLE_AEM_ORIG_QUICKSTART_PATH("AEM_6.5_Quickstart.jar", ""),
	SAMPLE_AEM_LTS_QUICKSTART_PATH("cq-quickstart-6.6.0.jar", ""),
	SAMPLE_AEM_SERVICE_PACK_PATH("aem-service-pkg-6.5.19.0.zip", "crx-quickstart/install"),
	SAMPLE_AEM_FORMS_ADDON_PATH("adobe-aemfd-" + getAbbrev() + "-pkg-6.0.1120.zip", "crx-quickstart/install"),
	SAMPLE_LICENSE_PROPERTIES_PATH("license.properties", ""),
	SECOND_AEM_FOMRS_ADDON_PATH("adobe-aemfd-" + getAbbrev() + "-pkg-6.0.9999.zip", "crx-quickstart/install"),
	SCAFFOLD_START_PATH("runStart" + (SystemUtils.IS_OS_WINDOWS ? ".bat" : ""), ""),
	SCAFFOLD_STOP_PATH("runStop" + (SystemUtils.IS_OS_WINDOWS ? ".bat" : ""), "")
	;

	private final Path filename;
	private final Path expectedLocationDir;
	
	private MockInstallFiles(Path filename, Path expectedLocationDir) {
		this.filename = filename;
		this.expectedLocationDir = expectedLocationDir;
	}

	private MockInstallFiles(String filename, String expectedLocationDir) {
		this(Path.of(filename), Path.of(expectedLocationDir));
	}
	
	/**
	 * Returns the name of the mock install file.
	 * 
	 * @return
	 */
	public Path filename() {
		return filename;
	}

	public Path expectedLocation() {
		return expectedLocationDir.resolve(filename);
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
	
	enum AemInstallType {
		// Original version of AEM 6.5 with service pack and forms addon.
		AEM_ORIG( 11, "AEM_65_SP19",
				SAMPLE_AEM_ORIG_QUICKSTART_PATH,
				SAMPLE_AEM_SERVICE_PACK_PATH,
				SAMPLE_AEM_FORMS_ADDON_PATH,
				SAMPLE_LICENSE_PROPERTIES_PATH
				), 
		// GA version of AEM LTS 6.5 without service pack.
		AEM_LTS_NOSP( 21, "AEM_66_SP0",
				SAMPLE_AEM_LTS_QUICKSTART_PATH,
				SAMPLE_AEM_FORMS_ADDON_PATH,
				SAMPLE_LICENSE_PROPERTIES_PATH
				)
		;
		
		private final int javaVersion;
		private final Path aemDir;
		private final List<MockInstallFiles> files;

		private AemInstallType(int javaVersion, Path aemDir, List<MockInstallFiles> files) {
			this.javaVersion = javaVersion;
			this.aemDir = aemDir;
			this.files = files;
		}

		private AemInstallType(int javaVersion, String aemDirName, MockInstallFiles... files) {
			this(javaVersion, Path.of(aemDirName), List.of(files));
		}

		public void createMockFiles(Path rootDir) throws IOException {
			for (MockInstallFiles file : files) {
				file.createMockFile(rootDir);
			}
		}

		public List<MockInstallFiles> files() {
			return files;
		}

		public int javaVersion() {
			return javaVersion;
		}

		public Path aemDir() {
			return aemDir;
		}
	}
}
