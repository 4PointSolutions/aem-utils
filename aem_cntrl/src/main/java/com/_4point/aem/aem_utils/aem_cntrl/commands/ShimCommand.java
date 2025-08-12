package com._4point.aem.aem_utils.aem_cntrl.commands;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.Shim;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "shim", description = "Creates (or re-creates) shim files for starting/stopping AEM, containing Java environment settings.")
public class ShimCommand implements Callable<Integer> {

	@ArgGroup(exclusive = true, multiplicity = "1")
	ShimOptions shimOptions;
	
	static class ShimOptions {
		@Option(names = {"-a", "--add"}, required = true, description = "create new shim files") boolean add;
		@Option(names = {"-u", "--update"}, required = true, description = "update existing files") boolean update;
		@Option(names = {"-au", "--add_update"}, required = true, description = "add or update files as appropriate") boolean addUpdate;
	}
	
	@Option(names = {"-ad", "--aemdir"}, description = "the AEM directory (i.e. the directory containing a crx-quickstart subdirectory)")
	Path aemDir;

	private final Shim shim;
	
	public ShimCommand(Shim shim) {
		this.shim = shim;
	}

	@Override
	public Integer call() throws Exception {
		shim.shim(toOperation(shimOptions.add, shimOptions.update, shimOptions.addUpdate), aemDir);
		return 0; // Return 0 to indicate success
	}

	private static Shim.Operation toOperation(boolean add, boolean update, boolean addUpdate) {
		if (add) {
			return Shim.Operation.ADD;
		} else if (update) {
			return Shim.Operation.UPDATE;
		} else if (addUpdate) {
			return Shim.Operation.ADD_UPDATE;
		} else {
			// This should never happen because picocli should enforce that one of the options is specified.
			throw new IllegalArgumentException("One of --add, --update, or --add_update must be specified.");
		}
	}
}
