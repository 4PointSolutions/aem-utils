package com._4point.aem.aem_utils.aem_cntrl.domain;

import java.nio.file.Path;

import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.Defaults;

public class DefaultsImpl implements Defaults {

	@Override
	public Path aemDir() {
		return OperatingSystem.isWindows() ? Path.of("\\Adobe") : Path.of("/opt/adobe");
	}
}
