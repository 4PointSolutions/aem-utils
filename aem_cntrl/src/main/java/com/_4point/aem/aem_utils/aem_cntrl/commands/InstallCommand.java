package com._4point.aem.aem_utils.aem_cntrl.commands;

import static java.util.Objects.*;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.AemInstaller;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "install", description = "Installs an on-prem version of AEM with Service Packs and Forms Add-on.")
public class InstallCommand implements Callable<Integer> {
	
	@Option(names = {"-s", "--srcDir"}, description = "source directory containing the AEM installation files.  Defaults to the current directory if not specified.")
	Path srcDir;
	
	@Option(names = {"-d", "--destDir"}, description = "destination directory for the AEM installation. For Windows, this is \\Adobe. For Linux, this is /opt/adobe.")
	Path destDir;

	private final AemInstaller aemInstaller;
	private final Supplier<Path> defaultAemDirSupplier;
	
	public InstallCommand(AemInstaller aemInstaller, Supplier<Path> defaultAemDirSupplier) {
		this.aemInstaller = aemInstaller;
		this.defaultAemDirSupplier = defaultAemDirSupplier;
	}

	@Override
	public Integer call() throws Exception {
		aemInstaller.installAem(requireNonNullElseGet(destDir, defaultAemDirSupplier), requireNonNullElseGet(srcDir, ()->Path.of("")));
		return 0;
	}
}
