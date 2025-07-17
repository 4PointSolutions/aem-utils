package com._4point.aem.aem_utils.aem_cntrl.adapters.spi;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com._4point.aem.aem_utils.aem_cntrl.AemCntrlApplication;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.JsonData;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.RestClient;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.RestClient.ContentType;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.RestClient.Response;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.RestClient.MultipartPayload.Builder;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.AemConfigManager;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.MobileFormsSettings;



@Disabled("These tests are disabled because they require a running AEM instance.  They are not run as part of the build, but can be run manually if desired.")
@SpringBootTest(properties = AemCntrlApplication.APP_CONFIG_PREFIX + ".run_installer=false", classes = AemCntrlApplication.class)
class RestClientAemConfigManagerIntegrationTest {
	private record Property(String name, String value) {
		static Property of(String name, String value) {
			return new Property(name, value);
		}
		@Override
		public String toString() {
			return name + "=" + value;
		}
	};

	private static final String PROPERTIES_JSON_PTR_STR = "/properties";
	private static final List<String> EXPECTED_PROPERTIES = List.of("lcforms.cache.strategy", "lcforms.cache.unit",
			"lcforms.cache.maxObjectSize", "lcforms.debug.debugOptions", "lcforms.debug.allowDebugParameters",
			"lcforms.model.embedHttpImages", "lcforms.data.keepDataDescription", "lcforms.data.protectedMode");
	private static final List<Property> EXPECTED_PROPERTY_VALUES = List.of(
			Property.of("lcforms.cache.strategy", "aggressive"), 
			Property.of("lcforms.cache.unit", "128"), 
			Property.of("lcforms.cache.maxObjectSize", "8M"), 
			Property.of("lcforms.debug.debugOptions", ""),
			Property.of("lcforms.debug.allowDebugParameters", "false"),
			Property.of("lcforms.model.embedHttpImages", "true"), 
			Property.of("lcforms.data.keepDataDescription", "false"), 
			Property.of("lcforms.data.protectedMode", "false"));
	@Autowired RestClient restClient;
	@Autowired JsonData.JsonDataFactory jsonDataFactory;
	
	@Test
	void testRestClient_GetConfig() throws Exception {
		// TODO:  curl http://localhost:4502 -u admin:admin -F 'apply=true'
//		Optional<Response> response = restClient.multipartPayloadBuilder("/system/console/configMgr/com.adobe.forms.admin.impl.LCFormsAdminServiceImpl")
//				  								.add("apply", "true")
//				  								.build()
//				  								.postToServer(ContentType.APPLICATION_JSON);
		
		// GET http://localhost:4502/system/console/configMgr/com.adobe.forms.admin.impl.LCFormsAdminServiceImpl?post=true&ts=170
		Optional<Response> response = restClient.getRequestBuilder("/system/console/configMgr/com.adobe.forms.admin.impl.LCFormsAdminServiceImpl")
												.queryParam("post", "true")
												.queryParam("ts", "170")
												.build()
												.getFromServer(ContentType.APPLICATION_JSON);
					
		assertTrue(response.isPresent(), "Response should be present");
		String jsonString = new String(response.get().data().readAllBytes());
		// System.out.println(jsonString);
		JSONAssert.assertEquals(SAMPLE_RESPONSE, jsonString, JSONCompareMode.STRICT);
	}
		
		
	@Test
	void testRestClient_PostConfig() throws Exception {
		// POST to http://localhost:4502/system/console/configMgr/com.adobe.forms.admin.impl.LCFormsAdminServiceImpl
		Builder payloadBuilder = restClient.multipartPayloadBuilder("/system/console/configMgr/com.adobe.forms.admin.impl.LCFormsAdminServiceImpl");
		payloadBuilder = payloadBuilder.add("apply", "true")
					  				   .add("action", "ajaxConfigManager")
					  				   .add("$location", "");

		for (Property property : EXPECTED_PROPERTY_VALUES) {
			if (property.name().equals("lcforms.data.protectedMode")) {
				payloadBuilder = payloadBuilder.add(property.name(), "true");	// Turn protected mode on
			} else {
				payloadBuilder = payloadBuilder.add(property.name(), property.value());
			}
			payloadBuilder = payloadBuilder.add(property.name(), property.value());
		}

		payloadBuilder = payloadBuilder.add("propertylist", EXPECTED_PROPERTY_VALUES.stream().map(Property::name).collect(Collectors.joining(",")));
		
		
		var requestBuilder = payloadBuilder.build();
		
		
		Optional<Response> response = requestBuilder.postToServer(ContentType.TEXT_HTML);

		assertTrue(response.isPresent(), "Response should be present");
		String responseString = new String(response.get().data().readAllBytes());
		System.out.println(responseString);

// %2C is comma, %24 is $
/*
apply=true&action=ajaxConfigManager
&%24location=
&lcforms.cache.strategy=aggressive
&lcforms.cache.unit=128
&lcforms.cache.maxObjectSize=8M
&lcforms.debug.debugOptions=
&lcforms.debug.allowDebugParameters=false
&lcforms.model.embedHttpImages=true
&lcforms.model.embedHttpImages=false
&lcforms.data.keepDataDescription=false
&lcforms.data.protectedMode=false
&propertylist=lcforms.cache.strategy
%2Clcforms.cache.unit
%2Clcforms.cache.maxObjectSize
%2Clcforms.debug.debugOptions
%2Clcforms.debug.allowDebugParameters
%2Clcforms.model.embedHttpImages
%2Clcforms.data.keepDataDescription
%2Clcforms.data.protectedMode
*/
		
	}

	@Test
	void testJsonData_ReadConfig() {
		JsonData configData = jsonDataFactory.apply(SAMPLE_RESPONSE);
		assertNotNull(configData, "Config Data should not be null");
		
//		List<String> properties = configData.listChildrenAt(PROPERTIES_JSON_PTR_STR);
//		properties.forEach(s->{
//			System.out.println(s);
//			JsonData propertyData = configData.subsetAt(PROPERTIES_JSON_PTR_STR + "/" + s).get();
//			System.out.println(
//					"Property: " + propertyData.at("/name").get() + ", Value: " + propertyData.at("/value").get());
//		});;
		
		
		List<Property> propertiesList = configData.childrenAt(PROPERTIES_JSON_PTR_STR)
				  .map(s->Property.of(s, configData.at(PROPERTIES_JSON_PTR_STR + "/" + s + "/value").orElseThrow()))
				  .toList();
//		System.out.println("Property List: " + propertyList);
//		System.out.println("Property Map: " + propertiesList);
		assertEquals(EXPECTED_PROPERTIES, propertiesList.stream().map(Property::name).toList(), "Property list should match expected");
		assertEquals(EXPECTED_PROPERTY_VALUES, propertiesList, "Property map should match expected");
	}
	
	
	private static final String SAMPLE_RESPONSE =
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
						"value": false,
						"description": "select the option to enable protected mode (lcforms.data.protectedMode)"
					}
				},
				"bundleLocation": "",
				"bundle_location": null,
				"service_location": ""
			}
			""";
	@Test
	void testAemConfigManager_SetProtectedMode() {
		AemConfigManager aemConfigManager = new RestClientAemConfigManager(restClient, jsonDataFactory);
		
		MobileFormsSettings mobileFormsSettings = aemConfigManager.mobileFormsSettings();
		assertTrue(mobileFormsSettings.embedHttpImages());
		assertTrue(mobileFormsSettings.protectedMode());
		
		mobileFormsSettings.protectedMode(false);
		
		aemConfigManager.mobileFormsSettings(mobileFormsSettings);
		
		}

	//	@Test
//	void test() {
//		fail("Not yet implemented");
//	}
}
