package com._4point.aem.aem_utils.aem_cntrl.domain;

import static com._4point.aem.aem_utils.aem_cntrl.domain.AemInstallationFiles.AemQuickstart.findQuickstart;
import static com._4point.aem.aem_utils.aem_cntrl.domain.AemInstallationFiles.AemQuickstart.versionInfo;

import java.nio.file.Path;

import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.AemDir;
import com._4point.aem.aem_utils.aem_cntrl.domain.AemInstallationFiles.AemQuickstart.VersionInfo;
import com._4point.aem.aem_utils.aem_cntrl.domain.InstallationFiles.InstallationFileException;
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
		VersionInfo versionInfo;
		try {
			versionInfo = versionInfo(findQuickstart(finalAemDir));
		} catch (InstallationFileException e) {
			throw new Shim.ShimException(e.getMessage() + " Directory: %s".formatted(finalAemDir), e);
		}
		
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
