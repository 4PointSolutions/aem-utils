package com._4point.aem.aem_utils.aem_cntrl.domain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.WaitForLog;

public class WaitForLogImpl implements WaitForLog {
	
	enum AemDirType {
		DEFAULT, 	// Relative to the default AEM directory
		RELATIVE, 	// Relative to the current working directory
		ABSOLUTE,	// Absolute path
		NULL, 		// Null
		;
		
		static AemDirType of(Path aemDir) {
			if (aemDir == null) {
				return NULL;
			} else if (aemDir.getRoot() != null) {
            	return ABSOLUTE;
            } else if (isRelative(aemDir.subpath(0, 1).toString())) {
            	return RELATIVE;
            } else {
            	return DEFAULT;
            }
        }
		
		private static boolean isRelative(String firstElement) {
			return ".".equals(firstElement) || "..".equals(firstElement);
		}
	}

	private final Supplier<Path> defaultAemDirSupplier;
	
	public WaitForLogImpl(Supplier<Path> defaultAemDirSupplier) {
		this.defaultAemDirSupplier = defaultAemDirSupplier;
	}



	/**
	 * Waits for a log entry matching the specified regex in the AEM log files.
	 * 
	 * @param regexArgument The regex to match against log entries.
	 * @param timeout       The maximum time to wait for a matching log entry.
	 * @param from          The source of the log (e.g., "error", "info").
	 * @param aemDir        The directory where AEM logs are stored. If null, the
	 *                      default AEM directory is used.
	 */
	@Override
	public void waitForLog(RegexArgument regexArgument, Duration timeout, FromOption from, final Path aemDir) {
		internalWaitForLog(regexArgument, timeout, from, locateAemDir(adjustProvidedAemDirParam(aemDir)));
	}

	private void internalWaitForLog(RegexArgument regexArgument, Duration timeout, FromOption from, final Path finalAemDir) {
		// find the log file
		Path logFile = finalAemDir.resolve(AemFiles.LOG_FILE);
		
		
		System.out.println("Path: " + finalAemDir + ", is " + AemDirType.of(finalAemDir).toString() + ", log file: " + logFile);
		
//		System.out.println("Path: " + aemDir + ", is absolute:" + aemDir.isAbsolute() + ", absolute path: " + aemDir.toAbsolutePath());
//		for (Path p : asIterable(aemDir.normalize())) {
//			System.out.println("  " + p );
//		}
//		System.out.println();
//		System.out.println("  Root element: " + aemDir.getRoot());
//		System.out.println("  First element: " + aemDir.subpath(0,1));
		// 
		
		
		
		throw new UnsupportedOperationException("Method not implemented yet.");
	}



	/**
	 * Adjusts the AEM directory based on the type of Path specified. 
	 *   - If the Path is null, it returns the default AEM directory.
	 *   - If the Path is a relative path, it resolves it against the default AEM directory.
	 *   - If the Path is a relative path starting with . or .. , it resolves it against the current directory.
	 *   - If the Path is an absolute path, it returns the Path as is.
	 * 
	 * @param aemDir
	 * @return
	 */
	private Path adjustProvidedAemDirParam(Path aemDir) {
		// Adjust the AEM directory based on the type of Path specified.
		AemDirType aemDirType = AemDirType.of(aemDir);
		if (aemDirType == AemDirType.NULL) {				// Not specified, use the default AEM directory
			return defaultAemDirSupplier.get();
		} else if (aemDirType == AemDirType.DEFAULT) {		// Specified as a relative path to the default AEM directory
			return DefaultsImpl.aemDir().resolve(aemDir);
		} // else ABSOLUTE or RELATIVE, no change needed
		return aemDir;
	}

	private Path locateAemDir(final Path adjustedAemDir) {
		// If the adjusted AEM directory does not contain crx-quickstart, then locate a child directory that does.
		return isAemDir(adjustedAemDir) ? adjustedAemDir : locateAemChildDir(adjustedAemDir);
	}
	
	private static Iterable<Path> asIterable(Path p) {
		return ()->p.iterator();
	}
	
	private static Path locateAemChildDir(Path aemParentDir) {
        try {
        	List<Path> aemDirs = locateAemDirs(aemParentDir).toList();
        	if (aemDirs.size() == 0) {
        		throw new WaitForLogException("No AEM directory found in " + aemParentDir);
        	} else if (aemDirs.size() > 1) {
        		throw new WaitForLogException("Too many AEM directories found in " + aemParentDir + ". Please be more specific in your AEM directory specification.");
        	}
        	return aemDirs.getFirst();
        } catch (IOException e) {
            throw new WaitForLogException("Error locating AEM directories in " + aemParentDir, e);
        }
	}
	
	private static Stream<Path> locateAemDirs(Path aemParentDir) throws IOException {
		return Files.list(aemParentDir)
				.filter(p -> isAemDir(p)) // Filter directories that contain the CRX Quickstart directory)
				;
	}
	
	private static boolean isAemDir(Path p) {
        return Files.isDirectory(p) && Files.exists(p.resolve(AemFiles.CRX_QUICKSTART_DIR));
	}
}
