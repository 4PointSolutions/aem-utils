package com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi;

import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.RestClientAemConfigManager;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.RestClientAemConfigManager.AemConfigManagerException;

public interface AemConfigManager {

	MobileFormsSettings mobileFormsSettings();

	void mobileFormsSettings(MobileFormsSettings settings) throws AemConfigManagerException;

}