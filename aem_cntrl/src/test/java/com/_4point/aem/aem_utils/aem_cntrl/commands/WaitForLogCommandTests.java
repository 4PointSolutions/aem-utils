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

import com._4point.aem.aem_utils.aem_cntrl.domain.DefaultsImpl;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.WaitForLog;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.WaitForLog.RegexArgument;

import picocli.CommandLine;

class WaitForLogCommandTests {
	private static final Path AEM_DIR = DefaultsImpl.aemDir();

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
			verify(waitForLogMock).waitForLog(eq(WaitForLog.RegexArgument.startup()), eq(WaitForLog.DEFAULT_DURATION),
					eq(WaitForLog.DEFAULT_FROM_OPTION), eq(AEM_DIR));
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
			verify(waitForLogMock).waitForLog(eq(WaitForLog.RegexArgument.shutdown()), eq(WaitForLog.DEFAULT_DURATION),
					eq(WaitForLog.DEFAULT_FROM_OPTION), eq(AEM_DIR));
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
			RegexArgument expectedRegexArg = WaitForLog.RegexArgument.custom(regex);
			doNothing().when(waitForLogMock).waitForLog(expectedRegexArg, WaitForLog.DEFAULT_DURATION,
					WaitForLog.DEFAULT_FROM_OPTION, null);
			verify(waitForLogMock, times(1)).waitForLog(any(WaitForLog.RegexArgument.RegexCustom.class)/* For some reason, can't use eq() here */, 
					eq(WaitForLog.DEFAULT_DURATION), eq(WaitForLog.DEFAULT_FROM_OPTION), eq(AEM_DIR));
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
			verify(waitForLogMock).waitForLog(eq(WaitForLog.RegexArgument.startup()), eq(WaitForLog.DEFAULT_DURATION),
					eq(WaitForLog.FromOption.START), eq(AEM_DIR));
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
			verify(waitForLogMock).waitForLog(eq(WaitForLog.RegexArgument.startup()), eq(WaitForLog.DEFAULT_DURATION),
					eq(WaitForLog.FromOption.END), eq(AEM_DIR));
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
			verify(waitForLogMock).waitForLog(eq(WaitForLog.RegexArgument.startup()), eq(Duration.ofMinutes(10)),
					eq(WaitForLog.DEFAULT_FROM_OPTION), eq(AEM_DIR));
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
			verify(waitForLogMock).waitForLog(eq(WaitForLog.RegexArgument.startup()), eq(WaitForLog.DEFAULT_DURATION),
					eq(WaitForLog.DEFAULT_FROM_OPTION), eq(Path.of("AemDir")));
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

	@SpringBootTest(args = {"wflog", "--startup", "--fromEnd", "--fromStart"})
	static class WaitForLogCommand_BothFromEndAndFromStartTest {
		
		@MockitoBean WaitForLog waitForLogMock;
		@Autowired ExitCodeGenerator exitCodeGenerator;

		@Test
		void testWaitForLog_BothFromEndANdFromStart() throws Exception {
			verifyNoInteractions(waitForLogMock);
			assertEquals(CommandLine.ExitCode.USAGE, exitCodeGenerator.getExitCode(), "Exit code should be USAGE when both --fromEnd and --fromStart are specified.");
		}
	}

}
