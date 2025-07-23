package com._4point.aem.aem_utils.aem_cntrl.domain;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._4point.aem.aem_utils.aem_cntrl.domain.AemFiles.SlingProperties;
import com._4point.aem.aem_utils.aem_cntrl.domain.AemInstallationFiles.AemFileset;
import com._4point.aem.aem_utils.aem_cntrl.domain.AemInstallationFiles.AemVersion;
import com._4point.aem.aem_utils.aem_cntrl.domain.FluentFormsFiles.FluentFormsFileset;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.AemInstaller;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.ipi.ProcessRunner;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.AemConfigManager;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.MobileFormsSettings;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer.TailerFactory;


public class AemInstallerImpl implements AemInstaller {
	private static final Logger log = LoggerFactory.getLogger(AemInstallerImpl.class);

	private final TailerFactory tailerFactory;
	private final ProcessRunner processRunner;
	private final AemConfigManager aemConfigManager;

	public AemInstallerImpl(TailerFactory tailerFactory, ProcessRunner processRunner, AemConfigManager aemConfigManager) {
		this.tailerFactory = tailerFactory;
		this.processRunner = processRunner;
		this.aemConfigManager = aemConfigManager;
	}

	/**
	 *  Installs AEM into the rootDir 
	 * 
	 * @param rootDir - Directory where the AEM directories will reside.  It is typically root on Windows and /opt on Linux.
	 * @param aemFilesLocation - Directory where the Adobe installation files and FluentForms files reside (they must all be in one directory)
	 * @param jsonDataFactory 
	 * @param restClient 
	 * @throws Exception - If anything unusual happens, this typically results in an Exception and processing stops.
	 */
	@Override
	public void installAem(Path rootDir, Path aemFilesLocation) throws Exception {
		// Creates the directory structure (throws error if it already exists)
		//    May be able to determine the directory name from the filenames used (e.g. detect the SP file and use that SP # in the directory name)
		
		//    This expects that the main location has already been created (/opt/adobe or /Adobe) and has appropriate permissions
		//    On Linux, this means:
		//     > sudo useradd aem_user
		//     > sudo mkdir /opt/adobe
		//     > sudo chown aem_user /opt/adobe
		//
		//    The installer should be run as aem_user
		//
		AemFileset aemFiles = AemInstallationFiles.locateAemFiles(aemFilesLocation);
		AemVersion aemVersionInfo = aemFiles.aemVersion();
		Path aemDir = AemInstallationFiles.aemDir(rootDir, aemVersionInfo);
		log.atInfo().addArgument(rootDir.toString()).log("Creating Directories under {}.");
		Files.createDirectories(aemDir);  // Create directory where AEM quickstart will reside
 		
		// Copy the quickstart.jar
		log.atInfo().log("Copying Quickstart files");
		Path quickstartJarPath = aemFiles.copyQuickstartToTarget(aemDir);
		
		// Copy license.properties
		log.atInfo().log("Copying License file");
		aemFiles.copyLicensePropertiesToTarget(aemDir);
		
		AemProcess aemQuickstart = new AemProcess.UninitializedAemInstance(quickstartJarPath, aemVersionInfo.aemJavaVersion(), processRunner).unpackQuickstart(tailerFactory);
		
		log.atInfo().log("Running AEM to initialize AEM");
		aemQuickstart.startQuickstartInitializeAem();
		
		Path installDir = aemDir.resolve(AemFiles.INSTALL_DIR);
		Files.createDirectories(installDir);  // Create directory where AEM quickstart will reside
		
		log.atInfo().log("Copying Service Pack file");
		aemFiles.copyServicePackToTarget(installDir)
				.ifPresent(p->{	// If the service pack file was copied, then run AEM to install it.
					log.atInfo().log("Running AEM to install Service Pack");
					aemQuickstart.startQuickstartInstallServicePack();
				});

		
		// TODO: Modify sling,properties to include JSaafe e entry
		log.atInfo().log("Copying Forms Addon file");
		aemFiles.copyFormsAddOnToTarget(installDir);
		log.atInfo().log("Running AEM to install Forms Addon");
		aemQuickstart.startQuickstartInstallFormsAddOn();

		// Install Fluent Forms
		log.atInfo().log("Copying FluentForms files");
		FluentFormsFileset fluentFormsFiles = FluentFormsFiles.locateFluentFormsFiles(aemFilesLocation);
		fluentFormsFiles.copyFilesToTarget(installDir);

		// No need to start AEM, these will get installed the next time it is run.
		
		// Update sling.properties with bouncy castle (JSAFE) setting
		// 
		log.atInfo().log("Updating sling.properties file");
		SlingProperties.under(aemDir)
		        	    .orElseThrow(() -> new FileNotFoundException("Unable to locate sling.properties file under " + aemDir))
		        	    .updateSlingProperties();

			
		aemQuickstart.startQuickstartPerformAction(()->enableProtectedMode(aemConfigManager));
		
		// Maybe allow options to increase timeouts (jacorb and apache aries)
		
		// Maybe allow HTML5 options: AllowHiddenFFields and enable save button
	}

	private static void enableProtectedMode(AemConfigManager aemConfigManager) {
		// Enable Designer HTML Rendering (turn protected mode off)
		log.atInfo().log("Enabling Protected Mode for HTML5 Rendering");
		MobileFormsSettings mobileFormsSettings = aemConfigManager.mobileFormsSettings();
		mobileFormsSettings.protectedMode(false);
		aemConfigManager.mobileFormsSettings(mobileFormsSettings);
	}
}
