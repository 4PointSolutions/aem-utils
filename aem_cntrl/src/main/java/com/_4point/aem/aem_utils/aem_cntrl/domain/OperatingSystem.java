package com._4point.aem.aem_utils.aem_cntrl.domain;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.SystemUtils;

public enum OperatingSystem {
	WINDOWS(List.of("CMD.exe","/C", "jbang"), "runStart.bat", "runStop.bat"),
	LINUX(List.of("jbang"), "runStart", "runStop"),
	MACOS(List.of("jbang"), "runStart", "runStop");

	private final List<String> jbangCommand;
	private final Path runStart;
	private final Path runStop;
	
	private OperatingSystem(List<String> jbangCommand, String runStartStr, String runStopStr) {
		this.jbangCommand = jbangCommand;
		this.runStart = Path.of(runStartStr);
		this.runStop = Path.of(runStopStr);
	}

	public static OperatingSystem getOs() {
		if (SystemUtils.IS_OS_WINDOWS) {
			return WINDOWS;
		} else if (SystemUtils.IS_OS_LINUX) {
			return LINUX;
		} else if (SystemUtils.IS_OS_MAC) {
			return MACOS;
		} else {
			throw new IllegalStateException("Unsupported Operating System (%s)".formatted(SystemUtils.OS_NAME));
		}
	}

	public static boolean isWindows() {
		return getOs() == WINDOWS;
	}

	public static boolean isLinux() {
		return getOs() == LINUX;
	}

	public static boolean isMocOs() {
		return getOs() == MACOS;
	}

	public static boolean isUnix() {
		return getOs() == LINUX || getOs() == MACOS;
	}

	public String[] jbangCommand(String...params) {
		return Stream.concat(jbangCommand.stream(), Arrays.stream(params)).toArray(String[]::new);
	}
	public Path runStart() {
		return runStart;
	}

	public Path runStop() {
		return runStop;
	}
}
