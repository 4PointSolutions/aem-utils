package com._4point.aem.aem_utils.aem_cntrl.domain;
public enum JavaVersion {
	VERSION_8("8"), VERSION_11("11"), VERSION_17("17"), VERSION_21("21");

	private final String version;

	private JavaVersion(String version) {
		this.version = version;
	}

	public String getVersionString() {
		return version;
	}
}
