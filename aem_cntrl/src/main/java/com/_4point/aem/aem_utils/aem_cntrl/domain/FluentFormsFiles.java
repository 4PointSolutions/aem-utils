package com._4point.aem.aem_utils.aem_cntrl.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;


public class FluentFormsFiles extends InstallationFiles {
	
	public record FluentFormsFileset(Optional<Path> fluentFormsCore, Optional<Path> restServices) {
		public void copyFilesToTarget(Path tagetDir) {
			fluentFormsCore().ifPresent(p->copyToTarget(p, tagetDir));
			restServices().ifPresent(p->copyToTarget(p, tagetDir));;
		}
	};
	
	public static FluentFormsFileset locateFluentFormsFiles(Path dir) {
		return new FluentFormsFileset(thereCanBeMaybeOne(FluentFormsCore.findFiles(dir), "FluentForms Core Library"),
									  thereCanBeMaybeOne(RestServices.findFiles(dir), "FluentForms REST Services Library"));
	}

	public static class FluentFormsCore extends InstallationFile {
		private static final Pattern FILENAME_PATTERN = Pattern.compile("fluentforms\\.core-(?<majorVersion>\\d)\\.(?<minorVersion>\\d)\\.(?<patchLevel>\\d)-SNAPSHOT.jar");	// TODO: Make SNAPSHOT extension optional

		public static List<Path> findFiles(Path dir) {
			return findFilesMatching(dir, FILENAME_PATTERN);
		}
	}

	public static class RestServices extends InstallationFile {
		private static final Pattern FILENAME_PATTERN = Pattern.compile("rest-services\\.server-(?<majorVersion>\\d)\\.(?<minorVersion>\\d)\\.(?<patchLevel>\\d)-SNAPSHOT.jar");	// TODO: Make SNAPSHOT extension optional

		public static List<Path> findFiles(Path dir) {
			return findFilesMatching(dir, FILENAME_PATTERN);
		}
	}
	
}
