package com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports;

import java.util.Optional;

public interface AemConfiguration {
	String servername();
	Integer port();
	String user();
	String password();
	Boolean useSsl();
	Optional<SslConfiguration> sslConfiguration();

	default public String url() {
		return "http" + (useSsl() ? "s" : "") + "://" + servername() + (port() != 80 ? ":" + port() : "") + "/";
	}
	
	default public boolean useSslConfiguration() {
        return useSsl() && sslConfiguration().isPresent();
    }

	public interface SslConfiguration {}	// Tagging interface for SSL configuration
	
	public record SimpleAemConfiguration(
			String servername,
			Integer port,
			String user,
			String password,
			Boolean useSsl,
			Optional<SslConfiguration> sslConfiguration) implements AemConfiguration
	{}
}

