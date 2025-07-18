package com._4point.aem.aem_utils.aem_cntrl.commands;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.WaitForLog;

class WaitForLogCommandTests {

	@SpringBootTest(args = {"wflog"})
	static class WaitForLogCommandNoArgsTest {
		
		@MockitoBean WaitForLog waitForLogMock;
		
		@Test
		void testInstall_NoArgs() throws Exception {
		}
		
	}
}
