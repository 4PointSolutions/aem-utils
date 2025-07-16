package com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports;
public interface AemConfiguration {
	String servername();
	Integer port();
	String user();
	String password();
	Boolean useSsl();
	SslConfiguration sslConfiguration();

	default public String url() {
		return "http" + (useSsl() ? "s" : "") + "://" + servername() + (port() != 80 ? ":" + port() : "") + "/";
	}

	public interface SslConfiguration {}	// Tagging interface for SSL configuration
	
	public record SimpleAemConfiguration(
			String servername,
			Integer port,
			String user,
			String password,
			Boolean useSsl,
			SslConfiguration sslConfiguration) implements AemConfiguration
	{}
}

