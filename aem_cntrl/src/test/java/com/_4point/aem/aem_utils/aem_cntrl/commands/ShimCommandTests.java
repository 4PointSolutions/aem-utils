package com._4point.aem.aem_utils.aem_cntrl.commands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.Shim;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.Shim.Operation;

import picocli.CommandLine;

class ShimCommandTests {

	@SpringBootTest(args = {"shim"})
	static class ShimCommandNoArgTest {

		@MockitoBean Shim shimMock;
		@Autowired ExitCodeGenerator exitCodeGenerator;
		
		@Test
		void testCall() {
			// Picocli should error out before calling shim
			verifyNoInteractions(shimMock);
			assertEquals(CommandLine.ExitCode.USAGE, exitCodeGenerator.getExitCode(), "Exit code should be USAGE when no arguments are provided.");
		}
	}

	@SpringBootTest(args = {"shim", "--add"})
	static class ShimCommandAddTest {
		
		@MockitoBean Shim shimMock;
		@Autowired ExitCodeGenerator exitCodeGenerator;

		@Test
		void testWaitForLog_Startup() throws Exception {
			doNothing().when(shimMock).shim(Operation.ADD, null);
			assertEquals(CommandLine.ExitCode.OK, exitCodeGenerator.getExitCode(), "Exit code should be 0 for successful shim with --add argument.");
			verify(shimMock).shim(Operation.ADD, null);
		}
	}

	@SpringBootTest(args = {"shim", "--update"})
	static class ShimCommandUpdateTest {
		
		@MockitoBean Shim shimMock;
		@Autowired ExitCodeGenerator exitCodeGenerator;

		@Test
		void testWaitForLog_Startup() throws Exception {
			doNothing().when(shimMock).shim(Operation.UPDATE, null);
			assertEquals(CommandLine.ExitCode.OK, exitCodeGenerator.getExitCode(), "Exit code should be 0 for successful shim with --add argument.");
			verify(shimMock).shim(Operation.UPDATE, null);
		}
	}

	@SpringBootTest(args = {"shim", "--add_update"})
	static class ShimCommandAddUpdateTest {
		
		@MockitoBean Shim shimMock;
		@Autowired ExitCodeGenerator exitCodeGenerator;

		@Test
		void testWaitForLog_Startup() throws Exception {
			doNothing().when(shimMock).shim(Operation.ADD_UPDATE, null);
			assertEquals(CommandLine.ExitCode.OK, exitCodeGenerator.getExitCode(), "Exit code should be 0 for successful shim with --add argument.");
			verify(shimMock).shim(Operation.ADD_UPDATE, null);
		}
	}

	@SpringBootTest(args = {"shim", "--add_update", "--aemdir", "AemDir"})
	static class ShimCommandAemDirTest {
		
		@MockitoBean Shim shimMock;
		@Autowired ExitCodeGenerator exitCodeGenerator;

		@Test
		void testWaitForLog_Startup() throws Exception {
			doNothing().when(shimMock).shim(Operation.ADD_UPDATE, Path.of("AemDir"));
			assertEquals(CommandLine.ExitCode.OK, exitCodeGenerator.getExitCode(), "Exit code should be 0 for successful shim with --add argument.");
			verify(shimMock).shim(Operation.ADD_UPDATE, Path.of("AemDir"));
		}
	}

	@SpringBootTest(args = {"shim", "--add_update", "--aemdir", "\\\\\\\\AemDir"})
	static class ShimCommandBadAemDirTest {
		
		@MockitoBean Shim shimMock;
		@Autowired ExitCodeGenerator exitCodeGenerator;

		@Test
		void testWaitForLog_Startup() throws Exception {
			// Picocli should error out before calling shim
			verifyNoInteractions(shimMock);
			assertEquals(CommandLine.ExitCode.USAGE, exitCodeGenerator.getExitCode(), "Exit code should be USAGE when bad AEM Path is provided.");
		}
	}

	@SpringBootTest(args = {"shim", "--add", "--update", "--add_update"})
	static class ShimCommandTooManyOperationsTest {
		
		@MockitoBean Shim shimMock;
		@Autowired ExitCodeGenerator exitCodeGenerator;

		@Test
		void testWaitForLog_Startup() throws Exception {
			// Picocli should error out before calling shim
			verifyNoInteractions(shimMock);
			assertEquals(CommandLine.ExitCode.USAGE, exitCodeGenerator.getExitCode(), "Exit code should be USAGE when more than one operation is provided.");
		}
	}
}
