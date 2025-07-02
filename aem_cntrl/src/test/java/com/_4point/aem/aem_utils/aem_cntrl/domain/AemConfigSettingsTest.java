package com._4point.aem.aem_utils.aem_cntrl.domain;

//import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com._4point.aem.aem_utils.aem_cntrl.adapters.ipi.JacksonJsonData;
import com._4point.aem.aem_utils.aem_cntrl.domain.AemConfigSettings.AemConfigSettingsException;
import com._4point.aem.aem_utils.aem_cntrl.domain.AemConfigSettings.AemConfigSettingsFactory;
import com._4point.testing.matchers.javalang.ExceptionMatchers;

class AemConfigSettingsTest {

	private static final String TEST_JSON1 = 
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
			            "value": true,
			            "description": "select the option to enable protected mode (lcforms.data.protectedMode)"
			        }
			    },
			    "bundleLocation": "",
			    "bundle_location": null,
			    "service_location": ""
			}
			""";
	
	private final AemConfigSettingsFactory factory = new AemConfigSettingsFactory(JacksonJsonData::from);
	private final AemConfigSettings underTest = factory.create(TEST_JSON1);


	@Test
	void testPid() {
		assertEquals("com.adobe.forms.admin.impl.LCFormsAdminServiceImpl", underTest.pid());
	}

	@Test
	void testGet() {
		assertEquals("aggressive", underTest.get("lcforms.cache.strategy"));
	}

	@Test
	void testGet_fails() {
		String propertyName = "lcforms.cache.doesNotExist";
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> underTest.get(propertyName));
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("No such property", propertyName));
	}

	@Test
	void testSet() {
		String propertyName = "lcforms.data.protectedMode";
		assertEquals("true", underTest.get(propertyName));
		underTest.set(propertyName, "false");
		assertEquals("false", underTest.get(propertyName));
	}

	@Test
	void testSet_fails() {
		String propertyName = "lcforms.cache.doesNotExist";
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> underTest.set(propertyName, "someValue"));
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("No such property", propertyName));
	}

	@Test
	void testGetInt() {
		assertEquals(128, underTest.getInt("lcforms.cache.unit"));
	}

	@Test
	void testSetInt() {
		String propertyName = "lcforms.cache.unit";
		assertEquals(128, underTest.getInt(propertyName));
		underTest.setInt(propertyName, 23);
		assertEquals(23, underTest.getInt(propertyName));
	}

	@Test
	void testGetBoolean() {
		assertEquals("aggressive", underTest.get("lcforms.cache.strategy"));
	}

	@Test
	void testSetBoolean() {
		String propertyName = "lcforms.data.protectedMode";
		assertEquals(true, underTest.getBoolean(propertyName));
		underTest.setBoolean(propertyName, false);
		assertEquals(false, underTest.getBoolean(propertyName));
	}

	@Test
	// Make sure the order of entries is maintained.
	void testCreate_MaintainsOrder() {
		List<String> keyList = underTest.properties().stream().map(Map.Entry::getKey).toList();
		
		List<String> expectedKeyList = List.of("lcforms.cache.strategy", "lcforms.cache.unit",
				"lcforms.cache.maxObjectSize", "lcforms.debug.debugOptions", "lcforms.debug.allowDebugParameters",
				"lcforms.model.embedHttpImages", "lcforms.data.keepDataDescription", "lcforms.data.protectedMode");
		
		assertEquals(expectedKeyList, keyList, "Property keys should be in expected order");
	}

	@Test
	void testCreate_FailsNoPid() {
		String json = 
            """
	        {
	            "title": "Mobile Forms Configurations",
	            "description": "Mobile Forms Configurations"
	        }
			""";
		
		AemConfigSettingsException ex = assertThrows(AemConfigSettingsException.class, () -> factory.create(json));
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("Missing pid"));
	}

	@Test
	void testCreate_FailsNoProperties() {
		String json = 
            """
	        {
			    "pid": "com.adobe.forms.admin.impl.LCFormsAdminServiceImpl",
	            "title": "Mobile Forms Configurations",
	            "description": "Mobile Forms Configurations"
	        }
			""";
		
		AemConfigSettingsException ex = assertThrows(AemConfigSettingsException.class, () -> factory.create(json));
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("Missing properties"));
	}

	@Test
	void testCreate_FailsNoValue() {
		String json = 
            """
	        {
			    "pid": "com.adobe.forms.admin.impl.LCFormsAdminServiceImpl",
	            "title": "Mobile Forms Configurations",
	            "description": "Mobile Forms Configurations",
			    "properties": {
			        "lcforms.cache.strategy": {
			            "name": "Caching Strategy",
			            "optional": false,
			            "description": "Caching Strategy (lcforms.cache.strategy)"
			        }
			    }
	        }
			""";
		
		AemConfigSettingsException ex = assertThrows(AemConfigSettingsException.class, () -> factory.create(json));
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("Missing property value", "lcforms.cache.strategy"));
	}
}
