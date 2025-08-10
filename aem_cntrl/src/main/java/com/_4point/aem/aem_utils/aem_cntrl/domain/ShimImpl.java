package com._4point.aem.aem_utils.aem_cntrl.domain;

import java.nio.file.Path;
import java.util.List;

import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.AemDir;
import com._4point.aem.aem_utils.aem_cntrl.domain.AemInstallationFiles.AemQuickstart.VersionInfo;
import com._4point.aem.aem_utils.aem_cntrl.domain.ShimFiles.CreateType;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.Shim;

public class ShimImpl implements Shim {

	private final AemDir aemDir;
	private final ShimFiles.RuntimeFactory shimFilesFactory;

	public ShimImpl(AemDir aemDir, ShimFiles.RuntimeFactory shimFilesFactory) {
		this.aemDir = aemDir;
		this.shimFilesFactory = shimFilesFactory;
	}

	@Override
	public void shim(Operation operation, Path unqualifiedAemDir) {
		Path finalAemDir = aemDir.toQualified(unqualifiedAemDir);									// find the aem directory

		// Determine AEM Version and Java Version
		List<Path> quickstarts = AemInstallationFiles.AemQuickstart.findFiles(finalAemDir);
		if (quickstarts.isEmpty()) {
			throw new Shim.ShimException("No AEM Quickstart found in directory: " + finalAemDir);
		} else if (quickstarts.size() > 1) {
			throw new Shim.ShimException("Multiple AEM Quickstarts found in directory: " + finalAemDir);
		}
		VersionInfo versionInfo = AemInstallationFiles.AemQuickstart.versionInfo(quickstarts.getFirst());
		// Call ShimFiles factory to create ShimFiles instance
		ShimFiles shimFiles = shimFilesFactory.apply(versionInfo.aemRelease().aemJavaVersion, finalAemDir);
	
		ShimFiles.CreateType createType = toCreateType(operation);
		shimFiles.createBatFiles(createType);
	}

	private static CreateType toCreateType(Operation operation) {
		return switch (operation) {
			case ADD -> ShimFiles.CreateType.NEW;
			case UPDATE -> ShimFiles.CreateType.EXISTING;
			case ADD_UPDATE -> ShimFiles.CreateType.NEW_OR_EXISTING;
        };
	}

}
