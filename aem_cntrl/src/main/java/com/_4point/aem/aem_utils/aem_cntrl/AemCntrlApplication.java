package com._4point.aem.aem_utils.aem_cntrl;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.client.RestClientSsl;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient.Builder;

import com._4point.aem.aem_utils.aem_cntrl.adapters.ipi.JavaLangProcessRunner;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.CommonsIoTailerTailer;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.RestClientAemConfigManager;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.adapters.JacksonJsonData;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.adapters.SpringRestClientRestClient;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.AemConfiguration;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.AemConfiguration.SslConfiguration;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.JsonData;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.RestClient;
import com._4point.aem.aem_utils.aem_cntrl.commands.AemCntrlCommandLine;
import com._4point.aem.aem_utils.aem_cntrl.domain.AemInstallerImpl;
import com._4point.aem.aem_utils.aem_cntrl.domain.DefaultsImpl;
import com._4point.aem.aem_utils.aem_cntrl.domain.WaitForLogImpl;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.AemInstaller;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.WaitForLog;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.ipi.ProcessRunner;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.AemConfigManager;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer.TailerFactory;

import picocli.CommandLine;
import picocli.CommandLine.IFactory;

@SpringBootApplication
@EnableConfigurationProperties(AemCntrlAemConfiguration.class)
public class AemCntrlApplication implements CommandLineRunner, ExitCodeGenerator {
	public static final String APP_CONFIG_PREFIX = "aemcntrl";

	private static final Logger log = LoggerFactory.getLogger(AemCntrlApplication.class);

	private final IFactory factory;
	private final AemCntrlCommandLine aemCntrlCommandLine;
	private int exitCode;

	public AemCntrlApplication(IFactory factory, AemCntrlCommandLine aemCntrlCommandLine) {
		this.factory = factory;
		this.aemCntrlCommandLine = aemCntrlCommandLine;
	}

	public static void main(String[] args) {
		System.exit(SpringApplication.exit(new SpringApplicationBuilder(AemCntrlApplication.class)
													.web(WebApplicationType.NONE)
													.run(args)));
	}

	@Override
	public int getExitCode() {
		if (exitCode == CommandLine.ExitCode.OK) {
			log.debug("AEM Control exited successfully.");
		} else if (exitCode == CommandLine.ExitCode.USAGE) {
			log.warn("AEM Control exited with usage error.  Please check the command line arguments.");
		} else if (exitCode == CommandLine.ExitCode.SOFTWARE) {
			log.error("AEM Control exited with a software error.  Please check the logs for more details.");
		} else {
			log.error("AEM Control exited with an unknown error code: {}", exitCode);
		}
		return exitCode;
	}

	@Override
	public void run(String... args) throws Exception {
		// let picocli parse command line args and run the business logic
        exitCode = new CommandLine(aemCntrlCommandLine, factory).execute(args);
		log.debug("Picocli returned: {}", exitCode);
    }
	
	@Bean
	AemConfiguration aemConfiguration(AemCntrlAemConfiguration aemCntrlAemConfiguration) {
		return AemCntrlAemConfiguration.aemConfiguration(aemCntrlAemConfiguration);
	}

	@Bean
	RestClient restClient(AemConfiguration aemConfiguration, RestClientSsl restClientSsl) {
		HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();	// Configure client to follow redirects since AEM uses them a lot.
		Builder restClientBuilder = org.springframework.web.client.RestClient.builder().requestFactory(new JdkClientHttpRequestFactory(httpClient));
		return SpringRestClientRestClient.create(
												aemConfiguration.url(), 
												aemConfiguration.user(), 
												aemConfiguration.password(), 
												aemConfiguration.useSslConfiguration() ? useSslBundle(restClientBuilder, aemConfiguration.sslConfiguration().get(), restClientSsl) : restClientBuilder
				 								);
	}
	
	private static Builder useSslBundle(Builder restClientBuilder, SslConfiguration sslConfiguration, RestClientSsl restClientSsl) {
		if (sslConfiguration instanceof AemCntrlAemConfiguration.AemCntrlSslConfiguration sslConfig) {
			return restClientBuilder.apply(restClientSsl.fromBundle(sslConfig.sslBundle()));
		}
		throw new IllegalStateException("Unknown SSL Configuration Type: " + sslConfiguration.getClass().getName());
	}

	@Bean
	JsonData.JsonDataFactory jsonDataFactory() {
		return JacksonJsonData::from;
	}

	@Bean
	TailerFactory tailerFactory() {
		return new CommonsIoTailerTailer.TailerFactory();
	}
	
	@Bean
	ProcessRunner processRunner() {
		return JavaLangProcessRunner.<Stream<String>, Stream<String>>builder()
				.setOutputStreamHandler(s->s)
				.setErrorStreamHandler(s->s)
				.build();
	}
	
	@Bean
	AemConfigManager aemConfigManager(RestClient restClient, JsonData.JsonDataFactory jsonDataFactory) {
		return new RestClientAemConfigManager(restClient, jsonDataFactory);
	}

	@Bean
	AemInstaller aemInstaller(TailerFactory tailerFactory, ProcessRunner processRunner, AemConfigManager aemConfigManager) {
		return new AemInstallerImpl(tailerFactory, processRunner, aemConfigManager);
	}
	
	@Bean
	Supplier<Path> defaultAemDirSupplier(AemCntrlAemConfiguration aemCntrlAemConfiguration) {
		return ()->DefaultsImpl.aemDir();
	}
	
	@Bean
	WaitForLog waitForLog(Supplier<Path> defaultAemDirSupplier, TailerFactory tailerFactory) {
		return new WaitForLogImpl(defaultAemDirSupplier, tailerFactory);
	}
}
