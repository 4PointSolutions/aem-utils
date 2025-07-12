package com._4point.aem.aem_utils.aem_cntrl.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.adapters.JacksonJsonData;


class MobileFormsSettingsTest {
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

	private final MobileFormsSettings underTest = new MobileFormsSettings(new AemConfigSettings.AemConfigSettingsFactory(JacksonJsonData::from).create(TEST_JSON1));
	
	@Test
	void testCacheStrategy() {
		assertEquals("aggressive", underTest.cacheStrategy());
	}

	@Test
	void testCacheStrategyString() {
		assertEquals("aggressive", underTest.cacheStrategy());
		underTest.cacheStrategy("none");
		assertEquals("none", underTest.cacheStrategy());
	}

	@Test
	void testCacheUnits() {
		assertEquals(128, underTest.cacheUnits());
	}

	@Test
	void testCacheUnitsInt() {
		assertEquals(128, underTest.cacheUnits());
		underTest.cacheUnits(256);
		assertEquals(256, underTest.cacheUnits());
	}

	@Test
	void testCacheObjectSize() {
		assertEquals("8M", underTest.cacheObjectSize());
	}

	@Test
	void testCacheObjectSizeInt() {
		assertEquals("8M", underTest.cacheObjectSize());
		underTest.cacheObjectSize("16M");
		assertEquals("16M", underTest.cacheObjectSize());
	}

	@Test
	void testDebugOptions() {
		assertEquals("", underTest.debugOptions());
	}

	@Test
	void testDebugOptionsString() {
		assertEquals("", underTest.debugOptions());
		underTest.debugOptions("1-a5-b5-c5");
		assertEquals("1-a5-b5-c5", underTest.debugOptions());
	}

	@Test
	void testAllowDebugParameters() {
		assertFalse(underTest.allowDebugParameters());
	}

	@Test
	void testAllowDebugParametersBoolean() {
		assertFalse(underTest.allowDebugParameters());
		underTest.allowDebugParameters(true);
		assertTrue(underTest.allowDebugParameters());
		underTest.allowDebugParameters(false);
		assertFalse(underTest.allowDebugParameters());
	}

	@Test
	void testEmbedHttpImages() {
		assertTrue(underTest.embedHttpImages());
	}

	@Test
	void testEmbedHttpImagesBoolean() {
		assertTrue(underTest.embedHttpImages());
		underTest.embedHttpImages(false);
		assertFalse(underTest.embedHttpImages());
		underTest.embedHttpImages(true);
		assertTrue(underTest.embedHttpImages());
	}

	@Test
	void testKeepDataDescription() {
		assertFalse(underTest.keepDataDescription());
	}

	@Test
	void testKeepDataDescriptionBoolean() {
		assertFalse(underTest.keepDataDescription());
		underTest.keepDataDescription(true);
		assertTrue(underTest.keepDataDescription());
		underTest.keepDataDescription(false);
		assertFalse(underTest.keepDataDescription());
	}

	@Test
	void testProtectedMode() {
		assertTrue(underTest.protectedMode());
	}

	@Test
	void testProtectedModeBoolean() {
		assertTrue(underTest.protectedMode());
		underTest.protectedMode(false);
		assertFalse(underTest.protectedMode());
		underTest.protectedMode(true);
		assertTrue(underTest.protectedMode());
	}

}
