package com._4point.aem.aem_utils.aem_cntrl.commands;

import static java.util.Objects.*;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.AemInstaller;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.Defaults;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "install")
public class InstallCommand implements Callable<Integer> {
	
	@Option(names = {"-s", "--srcDir"}, description = "source directory containing the AEM installation files")
	Path srcDir;
	
	@Option(names = {"-d", "--destDir"}, description = "destination directory for the AEM installation")
	Path destDir;

	private final AemInstaller aemInstaller;
	private final Defaults defaults;
	
	public InstallCommand(AemInstaller aemInstaller, Defaults defaults) {
		this.aemInstaller = aemInstaller;
		this.defaults = defaults;
	}

	@Override
	public Integer call() throws Exception {
		aemInstaller.installAem(requireNonNullElseGet(destDir, defaults::aemDir), requireNonNullElseGet(srcDir, ()->Path.of("")));
		return 0;
	}
}
