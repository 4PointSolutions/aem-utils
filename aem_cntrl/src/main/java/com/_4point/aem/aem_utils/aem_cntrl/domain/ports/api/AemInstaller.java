package com._4point.aem.aem_utils.aem_cntrl.domain.ports.api;

import java.nio.file.Path;

public interface AemInstaller {

	/**
	 *  Installs AEM into the rootDir 
	 * 
	 * @param rootDir - Directory where the AEM directories will reside.  It is typically root on Windows and /opt on Linux.
	 * @param aemFilesLocation - Directory where the Adobe installation files and FluentForms files reside (they must all be in one directory)
	 * @param jsonDataFactory 
	 * @param restClient 
	 * @throws Exception - If anything unusual happens, this typically results in an Exception and processing stops.
	 */
	void installAem(Path rootDir, Path aemFilesLocation) throws Exception;

}