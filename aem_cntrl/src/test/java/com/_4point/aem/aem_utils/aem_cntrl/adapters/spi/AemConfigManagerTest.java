package com._4point.aem.aem_utils.aem_cntrl.adapters.spi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com._4point.aem.aem_utils.aem_cntrl.adapters.ipi.JacksonJsonData;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.RestClientAemConfigManager;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.RestClient;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.RestClient.ContentType;
import com._4point.aem.aem_utils.aem_cntrl.domain.MobileFormsSettings;
import com._4point.aem.aem_utils.aem_cntrl.domain.MobileFormsSettings.Factory;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.AemConfigManager;


@ExtendWith(MockitoExtension.class)
class AemConfigManagerTest {

	@Mock(stubOnly = true) RestClient mockRestClient;
	@Mock(stubOnly = true) RestClient.GetRequest.Builder mockGetBuilder;
	@Mock(stubOnly = true) RestClient.MultipartPayload.Builder mockPayloadBuilder;
	@Mock(stubOnly = true) RestClient.GetRequest mockGetRequest;
	@Mock(stubOnly = true) RestClient.MultipartPayload mockPayload;
	@Mock(stubOnly = true) RestClient.Response mockResponse;

	AemConfigManager underTest;
	
	@BeforeEach
	void setUp() {
		// Initialize the AemConfigManager with mocked dependencies
		underTest = new RestClientAemConfigManager(mockRestClient, JacksonJsonData::from);
	}
	
	@Test
	void testMobileFormsSettings() throws Exception {
		// Given
		when(mockRestClient.getRequestBuilder("/system/console/configMgr/" + MobileFormsSettings.PID)).thenReturn(mockGetBuilder);
		when(mockGetBuilder.queryParam("post", "true")).thenReturn(mockGetBuilder);
		when(mockGetBuilder.queryParam("ts", "170")).thenReturn(mockGetBuilder);
		when(mockGetBuilder.build()).thenReturn(mockGetRequest);
		when(mockGetRequest.getFromServer(RestClient.ContentType.APPLICATION_JSON)).thenReturn(Optional.of(mockResponse));
		when(mockResponse.data()).thenReturn(new ByteArrayInputStream(SAMPLE_CONFIG_JSON.getBytes()));
		
		// When
		MobileFormsSettings result = underTest.mobileFormsSettings();
		
		// Then
		assertNotNull(result);
		assertAll(
				() -> assertEquals(MobileFormsSettings.PID, result.pid()),
				() -> assertEquals(8, result.properties().size())
				);
	}

	@Test
	void testMobileFormsSettings_MobileFormsSettings() throws Exception {
		// Given
		MobileFormsSettings settings = new MobileFormsSettings.Factory(JacksonJsonData::from).create(SAMPLE_CONFIG_JSON);
		when(mockRestClient.multipartPayloadBuilder("/system/console/configMgr/" + settings.pid()))
				.thenReturn(mockPayloadBuilder);
		when(mockPayloadBuilder.add("apply", "true")).thenReturn(mockPayloadBuilder);
		when(mockPayloadBuilder.add("action", "ajaxConfigManager")).thenReturn(mockPayloadBuilder);
		when(mockPayloadBuilder.add("$location", "")).thenReturn(mockPayloadBuilder);
		settings.properties().forEach(entry -> {
			when(mockPayloadBuilder.add(entry.getKey(), entry.getValue())).thenReturn(mockPayloadBuilder);
		});
		when(mockPayloadBuilder.add("propertylist", settings.properties().stream().map(Entry::getKey).collect(Collectors.joining(",")))).thenReturn(mockPayloadBuilder);
		when(mockPayloadBuilder.build()).thenReturn(mockPayload);
		when(mockPayload.postToServer(ContentType.TEXT_HTML)).thenReturn(Optional.of(mockResponse));
		
		// When
		underTest.mobileFormsSettings(settings);
		
		// Then
		// Since all the verification is in the mock setup, the fact that we've made it this far without an exception means the test passed.
	}

	public static final String SAMPLE_CONFIG_JSON = """
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
			            "value": true,
			            "description": "select the option to enable protected mode (lcforms.data.protectedMode)"
			        }
			    },
			    "bundleLocation": "",
			    "bundle_location": null,
			    "service_location": ""
			}			
			""";
}
