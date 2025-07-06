package com._4point.aem.aem_utils.aem_cntrl.commands;

import static java.util.Objects.*;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import com._4point.aem.aem_utils.aem_cntrl.domain.OperatingSystem;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.AemInstaller;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "install")
public class InstallCommand implements Callable<Integer> {
	
	@Option(names = {"-s", "--srcDir"}, description = "source directory containing the AEM installation files")
	Path srcDir;
	
	@Option(names = {"-d", "--destDir"}, description = "destination directory for the AEM installation")
	Path destDir;

	private final AemInstaller aemInstaller;
	
	public InstallCommand(AemInstaller aemInstaller) {
		this.aemInstaller = aemInstaller;
	}

	@Override
	public Integer call() throws Exception {
		aemInstaller.installAem(requireNonNullElseGet(destDir, ()->defaultDestDir()), requireNonNullElseGet(srcDir, ()->Path.of("")));
		return 0;
	}

	private static Path defaultDestDir() {
		return OperatingSystem.isWindows() ? Path.of("\\Adobe") : Path.of("/opt/adobe");
	}

}
