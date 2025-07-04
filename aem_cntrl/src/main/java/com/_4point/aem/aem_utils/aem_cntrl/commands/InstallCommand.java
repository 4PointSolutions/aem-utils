package com._4point.aem.aem_utils.aem_cntrl.commands;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import com._4point.aem.aem_utils.aem_cntrl.domain.OperatingSystem;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.AemInstaller;

import picocli.CommandLine.Command;

@Command(name = "install")
public class InstallCommand implements Callable<Integer> {
	
	private final AemInstaller aemInstaller;
	
	public InstallCommand(AemInstaller aemInstaller) {
		this.aemInstaller = aemInstaller;
	}

	@Override
	public Integer call() throws Exception {
		aemInstaller.installAem(OperatingSystem.isWindows() ? Path.of("\\Adobe") : Path.of("/opt/adobe"), Path.of(""));
		return 0;
	}

}
