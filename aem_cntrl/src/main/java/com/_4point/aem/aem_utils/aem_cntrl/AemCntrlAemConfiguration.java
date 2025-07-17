package com._4point.aem.aem_utils.aem_cntrl;

import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.AemConfiguration;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.AemConfiguration.SslConfiguration;



/**
 * Configuration parameters that pertain to connecting to AEM.
 * 
 * @param servername	the machine name of the AEM host machine
 * @param port	the port that AEM is running on
 * @param user	the username that will be used for authentication (Can be encoded using JASYPT ENC())
 * @param password	the password that will be used for authentication (Can be encoded using JASYPT ENC())
 * @param useSsl	boolean indicating whether to connect using SSL or not. (Defaults to false)
 *
 */
@ConfigurationProperties(AemCntrlApplication.APP_CONFIG_PREFIX + ".aem")
public record AemCntrlAemConfiguration(
	@DefaultValue("localhost") String servername,	// "aem.servername"
	@DefaultValue("4502") Integer port,				// "aem.port"
	@DefaultValue("admin") String user,				// "aem.user"
	@DefaultValue("admin") String password,			// "aem.password"
	@DefaultValue("false") Boolean useSsl,			// "aem.useSsl"
	@DefaultValue("aem") String sslBundle			// "aem.sslBundle"	- Spring SSL Bundle for trust store
	) {
	
	record AemCntrlSslConfiguration(String sslBundle) implements SslConfiguration {}
	
	public static AemConfiguration aemConfiguration(AemCntrlAemConfiguration aemConfig) {
		return new AemConfiguration.SimpleAemConfiguration(aemConfig.servername(), 
														   aemConfig.port(), 
														   aemConfig.user(), 
														   aemConfig.password(), 
														   aemConfig.useSsl(),
														   sslConfiguration(aemConfig.sslBundle())
														   );
		
        }

	private static Optional<SslConfiguration> sslConfiguration(String sslBundleName) {
            return sslBundleName == null || sslBundleName.isBlank()
                    ? Optional.empty()
                    : Optional.of(new AemCntrlSslConfiguration(sslBundleName));
	}
}