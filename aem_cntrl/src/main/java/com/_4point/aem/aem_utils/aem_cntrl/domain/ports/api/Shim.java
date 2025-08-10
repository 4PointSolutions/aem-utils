package com._4point.aem.aem_utils.aem_cntrl.domain.ports.api;

import java.nio.file.Path;

public interface Shim {
	enum Operation {
		ADD, UPDATE, ADD_UPDATE;
	}

	void shim(Operation operation, Path aemDir);
	
	@SuppressWarnings("serial")
	public static class ShimException extends RuntimeException {
		public ShimException(String message) {
			super(message);
		}

		public ShimException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
