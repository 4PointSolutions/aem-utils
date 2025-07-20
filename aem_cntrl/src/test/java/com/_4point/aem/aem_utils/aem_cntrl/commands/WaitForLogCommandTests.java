package com._4point.aem.aem_utils.aem_cntrl.commands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.WaitForLog;

import picocli.CommandLine;

class WaitForLogCommandTests {

	@SpringBootTest(args = {"wflog"})
	static class WaitForLogCommandNoArgsTest {
		
		@MockitoBean WaitForLog waitForLogMock;
		@Autowired ExitCodeGenerator exitCodeGenerator;
		
		@Test
		void testWaitForLog_NoArgs() throws Exception {
			// Picocli should error out before calling waitForLog
			verifyNoInteractions(waitForLogMock);
			assertEquals(CommandLine.ExitCode.USAGE, exitCodeGenerator.getExitCode(), "Exit code should be USAGE when no arguments are provided.");
		}
	}

	@SpringBootTest(args = {"wflog", "--startup"})
	static class WaitForLogCommandStartupTest {
		
		@MockitoBean WaitForLog waitForLogMock;
		@Autowired ExitCodeGenerator exitCodeGenerator;

		@Test
		void testWaitForLog_Startup() throws Exception {
			doNothing().when(waitForLogMock).waitForLog(WaitForLog.RegexArgument.startup(), WaitForLog.DEFAULT_DURATION,
					WaitForLog.DEFAULT_FROM_OPTION, null);
			assertEquals(CommandLine.ExitCode.OK, exitCodeGenerator.getExitCode(), "Exit code should be 0 for successful installation with --startup argument.");
		}
	}

	@SpringBootTest(args = {"wflog", "--shutdown"})
	static class WaitForLogCommandShutdownTest {
		
		@MockitoBean WaitForLog waitForLogMock;
		@Autowired ExitCodeGenerator exitCodeGenerator;

		@Test
		void testWaitForLog_Shutdown() throws Exception {
			doNothing().when(waitForLogMock).waitForLog(WaitForLog.RegexArgument.shutdown(), WaitForLog.DEFAULT_DURATION,
					WaitForLog.DEFAULT_FROM_OPTION, null);
			assertEquals(CommandLine.ExitCode.OK, exitCodeGenerator.getExitCode(), "Exit code should be 0 for successful installation with --startup argument.");
		}
	}

	@SpringBootTest(args = {"wflog", "--regex", ".*Error.*"})
	static class WaitForLogCommandCustomRegexTest {
		
		@MockitoBean WaitForLog waitForLogMock;
		@Autowired ExitCodeGenerator exitCodeGenerator;

		@Test
		void testWaitForLog_CustomRegex() throws Exception {
			Pattern regex = Pattern.compile(".*Error.*");
			doNothing().when(waitForLogMock).waitForLog(WaitForLog.RegexArgument.custom(regex), WaitForLog.DEFAULT_DURATION,
					WaitForLog.DEFAULT_FROM_OPTION, null);
			assertEquals(CommandLine.ExitCode.OK, exitCodeGenerator.getExitCode(), "Exit code should be 0 for successful installation with --startup argument.");
		}
	}

	@SpringBootTest(args = {"wflog", "--regex", ".*\\qError.*"})
	static class WaitForLogCommandInvalidRegexTest {
		
		@MockitoBean WaitForLog waitForLogMock;
		@Autowired ExitCodeGenerator exitCodeGenerator;

		@Test
		void testWaitForLog_InvaidRegex() throws Exception {
			verifyNoInteractions(waitForLogMock);
			assertEquals(CommandLine.ExitCode.USAGE, exitCodeGenerator.getExitCode(), "Exit code should be USAGE when an invalid regex is provided.");
		}
	}

	@SpringBootTest(args = {"wflog", "--startup", "--fromStart"})
	static class WaitForLogCommandFromStartTest {
		
		@MockitoBean WaitForLog waitForLogMock;
		@Autowired ExitCodeGenerator exitCodeGenerator;

		@Test
		void testWaitForLog_FromStart() throws Exception {
			doNothing().when(waitForLogMock).waitForLog(WaitForLog.RegexArgument.startup(), WaitForLog.DEFAULT_DURATION,
					WaitForLog.FromOption.START, null);
			assertEquals(CommandLine.ExitCode.OK, exitCodeGenerator.getExitCode(), "Exit code should be 0 for successful installation with --startup argument.");
		}
	}

	@SpringBootTest(args = {"wflog", "--startup", "--fromEnd"})
	static class WaitForLogCommandFromEndTest {
		
		@MockitoBean WaitForLog waitForLogMock;
		@Autowired ExitCodeGenerator exitCodeGenerator;

		@Test
		void testWaitForLog_FromEnd() throws Exception {
			doNothing().when(waitForLogMock).waitForLog(WaitForLog.RegexArgument.startup(), WaitForLog.DEFAULT_DURATION,
					WaitForLog.FromOption.END, null);
			assertEquals(CommandLine.ExitCode.OK, exitCodeGenerator.getExitCode(), "Exit code should be 0 for successful installation with --startup argument.");
		}
	}

	@SpringBootTest(args = {"wflog", "--startup", "--timeout", "PT10M"})
	static class WaitForLogCommandTimeoutTest {
		
		@MockitoBean WaitForLog waitForLogMock;
		@Autowired ExitCodeGenerator exitCodeGenerator;

		@Test
		void testWaitForLog_Timeout() throws Exception {
			doNothing().when(waitForLogMock).waitForLog(WaitForLog.RegexArgument.startup(), Duration.ofMinutes(10),
					WaitForLog.DEFAULT_FROM_OPTION, null);
			assertEquals(CommandLine.ExitCode.OK, exitCodeGenerator.getExitCode(), "Exit code should be 0 for successful installation with valid --timeout argument.");
		}
	}

	@SpringBootTest(args = {"wflog", "--startup", "--timeout"})
	static class WaitForLogCommandTimeoutNoValueTest {
		
		@MockitoBean WaitForLog waitForLogMock;
		@Autowired ExitCodeGenerator exitCodeGenerator;

		@Test
		void testWaitForLog_TimeoutNoValue() throws Exception {
			verifyNoInteractions(waitForLogMock);
			assertEquals(CommandLine.ExitCode.USAGE, exitCodeGenerator.getExitCode(), "Exit code should be USAGE when --timeout is specified without a value." );
		}
	}

	@SpringBootTest(args = {"wflog", "--startup", "--timeout", "Foo10M"})
	static class WaitForLogCommandTimeoutInvalidValueTest {
		
		@MockitoBean WaitForLog waitForLogMock;
		@Autowired ExitCodeGenerator exitCodeGenerator;

		@Test
		void testWaitForLog_TimeoutInvalidValue() throws Exception {
			verifyNoInteractions(waitForLogMock);
			assertEquals(CommandLine.ExitCode.USAGE, exitCodeGenerator.getExitCode(), "Exit code should be USAGE when --timeout is specified without a value." );
		}
	}

	@SpringBootTest(args = {"wflog", "--startup", "--aemdir", "AemDir"})
	static class WaitForLogCommandAemDirectoryTest {
		
		@MockitoBean WaitForLog waitForLogMock;
		@Autowired ExitCodeGenerator exitCodeGenerator;

		@Test
		void testWaitForLog_AemDirectory() throws Exception {
			doNothing().when(waitForLogMock).waitForLog(WaitForLog.RegexArgument.startup(), WaitForLog.DEFAULT_DURATION,
					WaitForLog.DEFAULT_FROM_OPTION, Path.of("AemDir"));
			assertEquals(CommandLine.ExitCode.OK, exitCodeGenerator.getExitCode(), "Exit code should be 0 for successful installation with --aemdir argument." );
		}
	}

	@SpringBootTest(args = {"wflog", "--startup", "--aemdir", "\\\\\\\\AemDir"})
	static class WaitForLogCommandInvalidAemDirectoryTest {
		
		@MockitoBean WaitForLog waitForLogMock;
		@Autowired ExitCodeGenerator exitCodeGenerator;

		@Test
		void testWaitForLog_InvalidAemDirectory() throws Exception {
			verifyNoInteractions(waitForLogMock);
			assertEquals(CommandLine.ExitCode.USAGE, exitCodeGenerator.getExitCode(), "Exit code should be USAGE when --aemdir is specified with an invalid value." );
		}
	}

}
