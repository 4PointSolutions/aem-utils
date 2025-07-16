package com._4point.aem.aem_utils.aem_cntrl.adapters.spi;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.adapters.JacksonJsonData;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.adapters.SpringRestClientRestClient;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.AemConfiguration;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.AemConfigManager;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.MobileFormsSettings;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

@WireMockTest
class RestClientAemConfigManagerWireMockTest {

	AemConfigManager underTest;

	@BeforeEach
	void setup(WireMockRuntimeInfo wmRuntimeInfo) {
		AemConfiguration restClientAemConfig = new AemConfiguration.SimpleAemConfiguration(
				"localhost", // host
				wmRuntimeInfo.getHttpPort(), 		// port
				"", 		// no username
				"", 		// no password
				false,		// don't useSSL
				null); 		// No SslConfiguration
		underTest = new RestClientAemConfigManager(SpringRestClientRestClient.create(restClientAemConfig.url(), restClientAemConfig.user(), restClientAemConfig.password()), JacksonJsonData::from);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testMobileFormsSettings(boolean protectedMode) {
		// Given
		stubFor(get(urlEqualTo("/system/console/configMgr/com.adobe.forms.admin.impl.LCFormsAdminServiceImpl?post=true&ts=170"))
						.withQueryParam("post", equalTo("true"))
						.withQueryParam("ts", equalTo("170"))
				.willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(SAMPLE_RESPONSE_FMT_STR.formatted(Boolean.toString(protectedMode)))));
		
		// When
		MobileFormsSettings result = underTest.mobileFormsSettings();
		
		// Then
		assertNotNull(result);
		assertAll(
				() -> assertEquals(JsonMobileFormsSettings.PID, result.pid()),
				() -> assertEquals(8, result.properties().size()),
				() -> assertEquals(protectedMode, result.protectedMode())
				);
		
		// Given
		stubFor(post(urlEqualTo("/system/console/configMgr/" + JsonMobileFormsSettings.PID))
				.withMultipartRequestBody(aMultipart().withName("apply").withBody(equalTo("true")))
				.withMultipartRequestBody(aMultipart().withName("action").withBody(equalTo("ajaxConfigManager")))
				.withMultipartRequestBody(aMultipart().withName("$location").withBody(equalTo("")))
				.withMultipartRequestBody(aMultipart().withName("lcforms.data.protectedMode").withBody(equalTo(Boolean.toString(!protectedMode))))
				.withMultipartRequestBody(aMultipart().withName("propertylist").withBody(containing("lcforms.data.protectedMode")))
				.willReturn(aResponse().withBody("<html></html>").withHeader("Content-Type", "text/html").withStatus(200)));
		
		// When
		result.protectedMode(!protectedMode); 	// Reverse the protected mode setting
		underTest.mobileFormsSettings(result);	// Update the settings
		
		// Then
		// If we got this far, then the test received a response based on the stub above and therefore passed.
		
	}

	private static final String SAMPLE_RESPONSE_FMT_STR =
			"""
			{
				"pid": "com.adobe.forms.admin.impl.LCFormsAdminServiceImpl",
				"title": "Mobile Forms Configurations",
				"description": "Mobile Forms Configurations",
				"properties": {
					"lcforms.cache.strategy": {
						"name": "Caching Strategy",
						"optional": false,
						"is_set": true,
						"type": {
							"labels": [
								"None",
								"Conservative",
								"Aggressive"
							],
							"values": [
								"none",
								"conservative",
								"aggressive"
							]
						},
						"value": "aggressive",
						"description": "Caching Strategy (lcforms.cache.strategy)"
					},
					"lcforms.cache.unit": {
						"name": "Cache Size in terms of number of forms (128)",
						"optional": false,
						"is_set": true,
						"type": 1,
						"value": "128",
						"description": "Number of forms to cache in-memory (lcforms.cache.unit)"
					},
					"lcforms.cache.maxObjectSize": {
						"name": "Max Object Size to be cached(8M)",
						"optional": false,
						"is_set": true,
						"type": 1,
						"value": "8M",
						"description": "Max Object Size limit to be cached (lcforms.cache.maxObjectSize)"
					},
					"lcforms.debug.debugOptions": {
						"name": "Debug Options",
						"optional": false,
						"is_set": true,
						"type": 1,
						"value": "",
						"description": "Options for enabling debugging. e.g.: 1-a5-b5-c5 (lcforms.debug.debugOptions)"
					},
					"lcforms.debug.allowDebugParameters": {
						"name": "Allow debug parameters in request",
						"optional": false,
						"is_set": true,
						"type": 11,
						"value": false,
						"description": "If enabled, HTML5 Forms would start honoring debugDir & log parameters (lcforms.debug.allowDebugParameters)"
					},
					"lcforms.model.embedHttpImages": {
						"name": "Embed Http\\/Https Images",
						"optional": false,
						"is_set": true,
						"type": 11,
						"value": true,
						"description": "select the option to embed Http\\/Https images using the data scheme instead of the http\\/https url in html (lcforms.model.embedHttpImages)"
					},
					"lcforms.data.keepDataDescription": {
						"name": "Keep Data Description",
						"optional": false,
						"is_set": true,
						"type": 11,
						"value": false,
						"description": "select the option to keep the dataDescription Tag in dataXML. This will increase the size of dataxml (lcforms.data.keepDataDescription)"
					},
					"lcforms.data.protectedMode": {
						"name": "Protected Mode",
						"optional": false,
						"is_set": true,
						"type": 11,
						"value": %s,
						"description": "select the option to enable protected mode (lcforms.data.protectedMode)"
					}
				},
				"bundleLocation": "",
				"bundle_location": null,
				"service_location": ""
			}
			""";
}
