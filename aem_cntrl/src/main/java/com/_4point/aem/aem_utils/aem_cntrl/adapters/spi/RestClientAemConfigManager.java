package com._4point.aem.aem_utils.aem_cntrl.adapters.spi;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.RestClient;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.JsonData.JsonDataFactory;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.RestClient.ContentType;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.RestClient.Response;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.RestClient.RestClientException;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.RestClient.MultipartPayload.Builder;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.AemConfigManager;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.MobileFormsSettings;


public class RestClientAemConfigManager implements AemConfigManager {
	private static final String CONFIG_BASE_PATH = "/system/console/configMgr/";
	
	private final RestClient restClient;
	private final JsonDataFactory jsonDataFactory;

	public RestClientAemConfigManager(RestClient restClient, JsonDataFactory jsonDataFactory) {
		this.restClient = restClient;
		this.jsonDataFactory = jsonDataFactory;
	}

	@Override
	public MobileFormsSettings mobileFormsSettings() {
			return new JsonMobileFormsSettings.Factory(jsonDataFactory).create(retrieveConfigSettings(JsonMobileFormsSettings.PID));
	}

	@Override
	public void mobileFormsSettings(MobileFormsSettings settings) throws AemConfigManagerException {
		postConfigSettings(settings.pid(), settings.properties());
	}
	
	private String retrieveConfigSettings(String pid) throws AemConfigManagerException {
		try {
			Optional<Response> response = restClient.getRequestBuilder(CONFIG_BASE_PATH + pid)
					.queryParam("post", "true")
					.queryParam("ts", "170")
					.build()
					.getFromServer(ContentType.APPLICATION_JSON);
			
			return new String(response.get().data().readAllBytes());
		} catch (RestClientException | IOException e) {
			throw new AemConfigManagerException("Error retrieving config settings from AEM", e);
		}
	}

	private void postConfigSettings(String pid, Set<Entry<String, String>> properties) throws AemConfigManagerException {
		Builder payloadBuilder = restClient.multipartPayloadBuilder(CONFIG_BASE_PATH + pid);
		
		// Add constant parameters to the payload
		payloadBuilder = payloadBuilder.add("apply", "true")
					  				   .add("action", "ajaxConfigManager")
					  				   .add("$location", "");

		// Add each property to the payload
		for (Entry<String, String> property : properties) {
			payloadBuilder = payloadBuilder.add(property.getKey(), property.getValue());
		}
        // Add propertylist as a comma-separated list of property names to the payload
		payloadBuilder = payloadBuilder.add("propertylist", properties.stream().map(Entry::getKey).collect(Collectors.joining(",")));
		
		try {
			Optional<Response> response = payloadBuilder.build().postToServer(ContentType.TEXT_HTML);
			if (!response.isPresent()) {
				throw new AemConfigManagerException("Failed to post config settings to AEM");
			}
		} catch (RestClientException e) {
			throw new AemConfigManagerException("Error posting config settings to AEM", e);
		}
	}
}
