package com._4point.aem.aem_utils.aem_cntrl.adapters.spi.adapters;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.AbstractJsonDataTest;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.JsonData;

class JacksonJsonDataTest extends AbstractJsonDataTest {

	public JacksonJsonDataTest() {
		super(JacksonJsonData::from);
	}
	
	// All the tests are inherited from AbstractJsonDataTest.
}
