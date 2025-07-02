package com._4point.aem.aem_utils.aem_cntrl.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com._4point.aem.aem_utils.aem_cntrl.domain.ports.ipi.JsonData;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.ipi.JsonData.JsonDataFactory;


/**
 * This class represents a generic set of settings for AEM configuration available from within Configuration Manager. 
 * It provides a base implementation that is used by other classes that represent specific setting sets. 
 * 
 */
public class AemConfigSettings {
	private final String pid;
	private final Map<String, String> properties;

	AemConfigSettings(ParseResult parseResult) {
		this(parseResult.pid(), parseResult.properties());
	}
	
	private AemConfigSettings(String pid, Map<String, String> properties) {
		this.pid = pid;
		this.properties = properties;
	}
	
	public String pid() {
		return pid;
	}
	
	public String get(String name) {
		String value = properties.get(name);
		if (value == null) {
			throw new IllegalArgumentException("No such property: " + name);
		}
		return value;
	}
	
	public void set(String name, String value) {
		// We use replace because all valid values should have been added initially by constructor.
		if (properties.replace(name, value) == null) {
			throw new IllegalArgumentException("No such property: " + name);
		}
	}
	
	public boolean getBoolean(String name) {
		return get(name).equalsIgnoreCase("true");
	}

	public void setBoolean(String name, boolean value) {
		set(name, Boolean.toString(value));
	}

	public int getInt(String name) {
		return Integer.parseInt(get(name));
	}

	public void setInt(String name, int value) {
		set(name, Integer.toString(value));
	}

	public Set<Map.Entry<String, String>> properties() {
		return properties.entrySet();
	}
	
	private record ParseResult(String pid, Map<String, String> properties) {
	}
	
	static class AemConfigSettingsFactory {
		private final JsonDataFactory jsonDataFactory;

		public AemConfigSettingsFactory(JsonDataFactory jsonDataFactory) {
			this.jsonDataFactory = jsonDataFactory;
		}

		public AemConfigSettings create(String json) throws AemConfigSettingsException {
			return new AemConfigSettings(parseJson(jsonDataFactory.apply(json)));
		}

		private static ParseResult parseJson(JsonData jsonData) throws AemConfigSettingsException {
			
			String pid = jsonData.at("/pid").orElseThrow(() -> new AemConfigSettingsException("Missing pid"));
			
			Map<String, String> map = jsonData.subsetAt("/properties")
											  .map(AemConfigSettingsFactory::getPropertyValues)
											  .orElseThrow(()->new AemConfigSettingsException("Missing properties"));
			
			return new ParseResult(pid, map);
		}

		private static Map<String, String> getPropertyValues(JsonData propertiesJsonData) {
			return propertiesJsonData.children()
									 .collect(Collectors.toMap(Function.identity(), 
											 p->getPropertyValue(propertiesJsonData, p),
											 (p,__) -> { throw new AemConfigSettingsException("Duplicate property: "); },
											 LinkedHashMap::new));  // Use a linked hash map to preserve insertion order
		}


		private static String getPropertyValue(JsonData propertiesJsonData, String property) {
			return propertiesJsonData.at("/" + property + "/value")
									 .orElseThrow(()->new AemConfigSettingsException("Missing property value (" + property + ")"));
		}
		
	}

	/**
	 * This exception is thrown when there is an error in the configuration
	 * settings.
	 */
	@SuppressWarnings("serial")
	public static class AemConfigSettingsException extends RuntimeException {

		public AemConfigSettingsException() {
		}

		public AemConfigSettingsException(String message, Throwable cause) {
			super(message, cause);
		}

		public AemConfigSettingsException(String message) {
			super(message);
		}

		public AemConfigSettingsException(Throwable cause) {
			super(cause);
		}
	}
}
