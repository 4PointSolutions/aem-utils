package com._4point.aem.aem_utils.aem_cntrl.adapters.spi;

import java.util.Map.Entry;

import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.JsonData.JsonDataFactory;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.MobileFormsSettings;

import java.util.Set;


/**
 * This class represents the settings for Mobile Forms configuration in AEM.
 * 
 */
public class JsonMobileFormsSettings implements MobileFormsSettings {
	public static final String PID = "com.adobe.forms.admin.impl.LCFormsAdminServiceImpl";;
	
	private static final String LCFORMS_CACHE_STRATEGY = "lcforms.cache.strategy";
	private static final String LCFORMS_CACHE_UNIT = "lcforms.cache.unit";
	private static final String LCFORMS_CACHE_MAX_OBJECT_SIZE = "lcforms.cache.maxObjectSize";
	private static final String LCFORMS_DEBUG_DEBUG_OPTIONS = "lcforms.debug.debugOptions";
	private static final String LCFORMS_DEBUG_ALLOW_DEBUG_PARAMETERS = "lcforms.debug.allowDebugParameters";
	private static final String LCFORMS_MODEL_EMBED_HTTP_IMAGES = "lcforms.model.embedHttpImages";
	private static final String LCFORMS_DATA_KEEP_DATA_DESCRIPTION = "lcforms.data.keepDataDescription";
	private static final String LCFORMS_DATA_PROTECTED_MODE = "lcforms.data.protectedMode";
	
	private final JsonAemConfigSettings settings;
	
	public JsonMobileFormsSettings(JsonAemConfigSettings settings) {
		this.settings = settings;
	}
	
	public static class Factory {
		private final JsonDataFactory jsonDataFactory;

		public Factory(JsonDataFactory jsonDataFactory) {
			this.jsonDataFactory = jsonDataFactory;
		}

		public MobileFormsSettings create(String json) {
			return new JsonMobileFormsSettings(
					new JsonAemConfigSettings.AemConfigSettingsFactory(jsonDataFactory).create(json));
		}
	}

	@Override
	public String pid() {
		return settings.pid();
	}
	
	@Override
	public Set<Entry<String, String>> properties() {
		return settings.properties();
	}
	
	@Override
	public String cacheStrategy() {
		return settings.get(LCFORMS_CACHE_STRATEGY);
	}

	@Override
	public void cacheStrategy(String cacheStrategy) {
		settings.set(LCFORMS_CACHE_STRATEGY, cacheStrategy);
	}

	@Override
	public int cacheUnits() {
		return settings.getInt(LCFORMS_CACHE_UNIT);
	}

	@Override
	public void cacheUnits(int cacheUnits) {
		settings.setInt(LCFORMS_CACHE_UNIT, cacheUnits);
	}

	@Override
	public String cacheObjectSize() {
		return settings.get(LCFORMS_CACHE_MAX_OBJECT_SIZE);
	}	

	@Override
	public void cacheObjectSize(String cacheObjectSize) {
		settings.set(LCFORMS_CACHE_MAX_OBJECT_SIZE, cacheObjectSize);
	}

	@Override
	public String debugOptions() {
		return settings.get(LCFORMS_DEBUG_DEBUG_OPTIONS);
	}

	@Override
	public void debugOptions(String debugOptions) {
		settings.set(LCFORMS_DEBUG_DEBUG_OPTIONS, debugOptions);
	}

	@Override
	public boolean allowDebugParameters() {
		return settings.getBoolean(LCFORMS_DEBUG_ALLOW_DEBUG_PARAMETERS);
	}

	@Override
	public void allowDebugParameters(boolean alllowDebugParameters) {
		settings.setBoolean(LCFORMS_DEBUG_ALLOW_DEBUG_PARAMETERS, alllowDebugParameters);
	}

	@Override
	public boolean embedHttpImages() {
		return settings.getBoolean(LCFORMS_MODEL_EMBED_HTTP_IMAGES);
	}

	@Override
	public void embedHttpImages(boolean embedHttpImages) {
		settings.setBoolean(LCFORMS_MODEL_EMBED_HTTP_IMAGES, embedHttpImages);
	}

	@Override
	public boolean keepDataDescription() {
		return settings.getBoolean(LCFORMS_DATA_KEEP_DATA_DESCRIPTION);
	}

	@Override
	public void keepDataDescription(boolean keepDataDescription) {
		settings.setBoolean(LCFORMS_DATA_KEEP_DATA_DESCRIPTION, keepDataDescription);
	}

	@Override
	public boolean protectedMode() {
		return settings.getBoolean(LCFORMS_DATA_PROTECTED_MODE);
	}

	@Override
	public void protectedMode(boolean protectedMode) {
		settings.setBoolean(LCFORMS_DATA_PROTECTED_MODE, protectedMode);
	}
}
