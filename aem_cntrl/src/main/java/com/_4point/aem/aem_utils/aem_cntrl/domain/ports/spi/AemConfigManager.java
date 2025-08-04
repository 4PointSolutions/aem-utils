package com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi;

public interface AemConfigManager {

	MobileFormsSettings mobileFormsSettings();

	void mobileFormsSettings(MobileFormsSettings settings) throws AemConfigManagerException;

	@SuppressWarnings("serial")
	public class AemConfigManagerException extends RuntimeException {

		public AemConfigManagerException(String message, Throwable cause) {
			super(message, cause);
		}

		public AemConfigManagerException(String message) {
			super(message);
		}
	}
}