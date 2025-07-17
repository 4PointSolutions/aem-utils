package com._4point.aem.aem_utils.aem_cntrl.domain;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AemFiles extends InstallationFiles {
	private static final Logger log = LoggerFactory.getLogger(AemFiles.class);
	
	public record AemFileset(Path quickstart, Optional<Path> servicePack, Path formsAddOn, Optional<Path> licenseProperties) {

		AemFileset(Path quickstart, Optional<Path> servicePack, Path formsAddOn) {
			this(quickstart, servicePack, formsAddOn, Optional.empty());
		}

		AemFileset(Path quickstart, Optional<Path> servicePack, Path formsAddOn, Path licenseProperties) {
			this(quickstart, servicePack, formsAddOn, Optional.of(licenseProperties));
		}

		AemVersion aemVersion() {
			AemQuickstart.VersionInfo quickstartVersionInfo = AemQuickstart.versionInfo(quickstart);
			Optional<AemServicePack.VersionInfo> servicePackVersionInfo = servicePack.map(AemServicePack::versionInfo);
			AemFormsAddOn.VersionInfo formsAddonVersionInfo = AemFormsAddOn.versionInfo(formsAddOn);
			validateVersionInfo(quickstartVersionInfo, servicePackVersionInfo, formsAddonVersionInfo);
			return new AemVersion(quickstartVersionInfo.majorVersion(), 
								  quickstartVersionInfo.minorVersion(),
								  servicePackVersionInfo.map(AemServicePack.VersionInfo::servicePack).orElse(0),
								  quickstartVersionInfo.aemRelease.aemJavaVersion);
		}
		
		public Path copyQuickstartToTarget(Path targetDir) {
			return copyToTarget(quickstart(), targetDir);
		}
		
		public Optional<Path> copyServicePackToTarget(Path targetDir) {
			return servicePack.map(sp->copyToTarget(sp, targetDir));
		}
		
		public Path copyFormsAddOnToTarget(Path targetDir) {
			return copyToTarget(formsAddOn(), targetDir);
		}
		
		public void copyLicensePropertiesToTarget(Path tagetDir) {
			licenseProperties().ifPresentOrElse(p->copyToTarget(p, tagetDir), ()->log.atWarn().log("No license file found."));	
		}
	}

	public static AemFileset locateAemFiles(Path dir) {
		try {
			return new AemFileset(thereCanBeOnlyOne(AemQuickstart.findFiles(dir), "AEM Quickstart"), 
								  thereCanBeMaybeOne(AemServicePack.findFiles(dir), "AEM Service Pack"), 
								  thereCanBeOnlyOne(AemFormsAddOn.findFiles(dir), "AEM Forms Add-on"),
								  LicenseProperties.findFile(dir));
		} catch (InstallationFileException e) {
			throw new IllegalStateException("Error locating AEM files in directory: %s".formatted(dir.toAbsolutePath().toString()), e);
		}
	}

	public static void validateVersionInfo(AemQuickstart.VersionInfo quickstartVersionInfo,
										   Optional<AemServicePack.VersionInfo> servcicePackVersionInfo,
										   AemFormsAddOn.VersionInfo formsAddonVersionInfo) {
		// TODO Check all the versions against a list of compatible versions, throw an exception if anything is amiss
		
	}

	public static Path aemDir(Path rootDir, AemVersion aemVersion) {
		return rootDir.resolve("AEM_%d%d_SP%s".formatted(aemVersion.majorversion, aemVersion.minorVersion, aemVersion.servicePack));
	}
	
	public record AemVersion(int majorversion, int minorVersion, int servicePack, JavaVersion aemJavaVersion) {

		public String aemVersion() {
			return "%d.%d".formatted(majorversion, minorVersion);
		}
		
		public String aemVersionWithSp() {
			return "%s_SP%d".formatted(aemVersion(), servicePack);
		}
	}
	
	public static class AemQuickstart extends InstallationFile {
		// Handle AEM 6.5 formatted quickstart name (AEM_6.5_Quickstart.jar) or AEM LTS quickstart name (cq-quickstart-6.6.0.jar)
		private static final Pattern AEM65_ORIG_FILENAME_PATTERN = Pattern.compile("AEM_(?<majorVersion>\\d)\\.(?<minorVersion>\\d)(?<patchVersion>)_Quickstart\\.jar");	// Note: patchVersion match group is intentionally empty
		private static final Pattern AEM65_LTS_FILENAME_PATTERN = Pattern.compile("cq-quickstart-(?<majorVersion>\\d)\\.(?<minorVersion>\\d)(\\.(?<patchVersion>\\d))\\.jar");
		private static final Pattern FILENAME_PATTERN = Pattern.compile("((AEM_)|(cq-quickstart-))(?<majorVersion>\\d)\\.(?<minorVersion>\\d)((\\.(?<patchVersion>\\d))|(_Quickstart))\\.jar");
		
		// This is a private constructor to prevent instantiation
		private AemQuickstart() {}

		public static List<Path> findFiles(Path dir) {
			return findFilesMatching(dir, FILENAME_PATTERN);
		}

		public enum AemBaseRelease {
			AEM65_ORIG(AEM65_ORIG_FILENAME_PATTERN, JavaVersion.VERSION_11), // AEM 6.5 original quickstart file (AEM_6.5_Quickstart.jar)
			AEM65_LTS(AEM65_LTS_FILENAME_PATTERN, JavaVersion.VERSION_21);	 // AEM 6.5 LTS quickstart file (cq-quickstart-6.6.0.jar)
			
			final Pattern quickstartFilePattern;
			final JavaVersion aemJavaVersion;

			private AemBaseRelease(Pattern quickstartFilePattern, JavaVersion aemJavaVersion) {
				this.quickstartFilePattern = quickstartFilePattern;
				this.aemJavaVersion = aemJavaVersion;
			}
		}
		
		public record VersionInfo(int majorVersion, int minorVersion, AemBaseRelease aemRelease) {}
		
		public static VersionInfo versionInfo(Path path) {
			String filename = path.getFileName().toString();
			Optional<VersionInfo> aem65OrigResults = getVersionInfo(filename, AEM65_ORIG_FILENAME_PATTERN);
			Optional<VersionInfo> aem65LtsResults = getVersionInfo(filename, AEM65_LTS_FILENAME_PATTERN);
            // We've already checked that there is only one quickstart file in the directory, so orElseThrow should never throw an exception.
			return aem65OrigResults.or(()-> aem65LtsResults).orElseThrow(()-> new IllegalStateException("Unexpected state: No AEM quickstart version info found for file: %s".formatted(filename)));
		}
		
		private static Optional<VersionInfo> getVersionInfo(String filename, Pattern pattern) {
			Matcher matcher = pattern.matcher(filename);
			if (matcher.matches()) {
				// Uncomment the following lines to see the results of the matcher
//				 MatchResult results = matcher.results().findAny().orElseThrow();
//				 IntStream.range(1, results.groupCount() + 1)
//				 		 .forEach(i -> System.out.println("Group %d: %s".formatted(i, results.group(i))));
    
				int majorVersion = Integer.parseInt(matcher.group("majorVersion"));
				int minorVersion = Integer.parseInt(matcher.group("minorVersion"));
				String patchVersion = matcher.group("patchVersion");
				AemBaseRelease aemRelease = patchVersion.isEmpty() ? AemBaseRelease.AEM65_ORIG : AemBaseRelease.AEM65_LTS;
				return Optional.of(new VersionInfo(majorVersion, minorVersion, aemRelease));
			}
			return Optional.empty();
		}
	}
	
	public static class AemServicePack extends InstallationFile {
		private static final Pattern FILENAME_PATTERN = Pattern.compile("aem-service-pkg-(?<majorVersion>\\d)\\.(?<minorVersion>\\d)\\.(?<servicePack>\\d+)\\.(?<patch>\\d+)\\.zip");
		
		// This is a private constructor to prevent instantiation
		public AemServicePack() {}

		public static List<Path> findFiles(Path dir) {
			return findFilesMatching(dir, FILENAME_PATTERN);
		}

		public record VersionInfo(int majorVersion, int minorVersion, int servicePack, int patch) {}
		
		public static VersionInfo versionInfo(Path path) {
			Matcher matcher = FILENAME_PATTERN.matcher(path.getFileName().toString());

			MatchResult results = matcher.results().findAny().orElseThrow();

			return new VersionInfo(Integer.valueOf(results.group(1)), Integer.valueOf(results.group(2)), Integer.valueOf(results.group(3)), Integer.valueOf(results.group(4)));
		}
	}	

	public static class AemFormsAddOn extends InstallationFile {
		private static final Pattern FILENAME_PATTERN = Pattern.compile("adobe-aemfd-%s-pkg-(?<majorVersion>\\d)\\.(?<minorVersion>\\d)\\.(?<buildNum>\\d+)\\.zip".formatted(abbrev()));
		
		// This is a private constructor to prevent instantiation
		private AemFormsAddOn() {}

		public static List<Path> findFiles(Path dir) {
			return findFilesMatching(dir, FILENAME_PATTERN);
		}

		public record VersionInfo(int majorVersion, int minorVersion, int buildNum) {}
		
		public static VersionInfo versionInfo(Path path) {
			Matcher matcher = FILENAME_PATTERN.matcher(path.getFileName().toString());

			MatchResult results = matcher.results().findAny().orElseThrow();

			return new VersionInfo(Integer.valueOf(results.group(1)), Integer.valueOf(results.group(2)), Integer.valueOf(results.group(3)));
		}
	}

	public static class LicenseProperties extends InstallationFile {
		
		// This is a private constructor to prevent instantiation
		private LicenseProperties() {}

		private static final Path LICENSE_PROPERTIES_PATH = Path.of("license.properties");
		
		public static Optional<Path> findFile(Path dir) {
			Path location = dir.resolve(LICENSE_PROPERTIES_PATH);
			return Files.exists(location) ? Optional.of(location) : Optional.empty();
		}
	}

	public static class SlingProperties extends InstallationFile {
		private static final Path SLING_PROPERTIES_PATH = Path.of("sling.properties");

		private final Path slingPropertiesFile;
		
		private SlingProperties(Path slingPropertiesFile) {
			this.slingPropertiesFile = slingPropertiesFile;
		}

		private static Path constructSlingPropertiesPath(Path aemDir) {
			return aemDir.resolve("conf").resolve(SLING_PROPERTIES_PATH);
		}

		public void updateSlingProperties() {
			try {
				String targetString = "sling.bootdelegation.class.com.rsa.jsafe.provider.JsafeJCE=com.rsa.*";
				// Read in file, if it doesn't conotain the target string then add it to the end of the file
				// If it does contain the target string, then do nothing
				List<String> lines = new ArrayList<>(Files.readAllLines(slingPropertiesFile));	// Since Files.readAllLines() returns an unspecified (either mutable nor immutable) list, we need to create a mutable copy
				if (lines.stream().anyMatch(l -> l.contains(targetString))) {
					log.atWarn().log("Sling properties file already contains the target string.");
					return;
				}
				log.atInfo().log("Adding target string to sling properties file.");
				lines.add(targetString);
				Files.write(slingPropertiesFile, lines);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		
		public static Optional<SlingProperties> under(Path crxQuickstartDir) throws FileNotFoundException {
			Path location = constructSlingPropertiesPath(crxQuickstartDir);
			if (Files.exists(location)) {
				return Optional.of(new SlingProperties(location));
			} else {
				log.atWarn().log("No sling properties file found at {}", location);
				return Optional.empty();
			}
		}
	}
	
	private static String abbrev() {
		return switch (OperatingSystem.getOs()) {
			case WINDOWS -> "win";
			case LINUX -> "linux";
			case MACOS -> "macos";
		};
	}
}
