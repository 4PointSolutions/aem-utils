package com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi;

import java.util.Set;
import java.util.Map.Entry;

public interface MobileFormsSettings {

	String pid();

	Set<Entry<String, String>> properties();

	String cacheStrategy();

	void cacheStrategy(String cacheStrategy);

	int cacheUnits();

	void cacheUnits(int cacheUnits);

	String cacheObjectSize();

	void cacheObjectSize(String cacheObjectSize);

	String debugOptions();

	void debugOptions(String debugOptions);

	boolean allowDebugParameters();

	void allowDebugParameters(boolean alllowDebugParameters);

	boolean embedHttpImages();

	void embedHttpImages(boolean embedHttpImages);

	boolean keepDataDescription();

	void keepDataDescription(boolean keepDataDescription);

	boolean protectedMode();

	void protectedMode(boolean protectedMode);

}