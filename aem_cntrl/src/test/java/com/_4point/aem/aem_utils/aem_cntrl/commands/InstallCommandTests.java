package com._4point.aem.aem_utils.aem_cntrl.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.AemInstaller;

import picocli.CommandLine;

class InstallCommandTests {
	@SpringBootTest(args = {"install"})
	static class InstallCommandNoArgsTest {
	
		@MockitoBean AemInstaller aemInstallerMock;
		@Autowired ExitCodeGenerator exitCodeGenerator;
	
		@EnabledOnOs(OS.WINDOWS)
		@Test
		void testInstall_NoArgs_Windows() throws Exception {
			doNothing().when(aemInstallerMock).installAem(any(Path.class), any(Path.class));
			verify(aemInstallerMock, times(1)).installAem(eq(Path.of("\\Adobe")), eq(Path.of("")));
			assertEquals(CommandLine.ExitCode.OK, exitCodeGenerator.getExitCode(), "Exit code should be 0 for successful installation without arguments.");
		}
	
		@EnabledOnOs(OS.LINUX)
		@Test
		void testInstall_NoArgs_Linux() throws Exception {
			doNothing().when(aemInstallerMock).installAem(any(Path.class), any(Path.class));
			verify(aemInstallerMock, times(1)).installAem(eq(Path.of("/opt", "adobe")), eq(Path.of("")));
			assertEquals(CommandLine.ExitCode.OK, exitCodeGenerator.getExitCode(), "Exit code should be 0 for successful installation without arguments.");
		}
	}	

	@SpringBootTest(args = {"install", "-d", "DestDir" })
	static class InstallCommandDestArgTest {

		@MockitoBean AemInstaller aemInstaller;
		@Autowired ExitCodeGenerator exitCodeGenerator;

		@Test
		void testInstall_DestArg() throws Exception {
			doNothing().when(aemInstaller).installAem(any(Path.class), any(Path.class));
			verify(aemInstaller, times(1)).installAem(eq(Path.of("DestDir")), eq(Path.of("")));
			assertEquals(CommandLine.ExitCode.OK, exitCodeGenerator.getExitCode(), "Exit code should be 0 for successful installation without source directory.");
		}
	}

	@SpringBootTest(args = {"install", "-s", "SrcDir" })
	static class InstallCommandSourceArgTest {

		@MockitoBean AemInstaller aemInstaller;
		@Autowired ExitCodeGenerator exitCodeGenerator;

		@Test
		void testInstall_SourceArg() throws Exception {
			doNothing().when(aemInstaller).installAem(any(Path.class), any(Path.class));
			verify(aemInstaller, times(1)).installAem(eq(Path.of("\\Adobe")), eq(Path.of("SrcDir")));
			assertEquals(CommandLine.ExitCode.OK, exitCodeGenerator.getExitCode(), "Exit code should be 0 for successful installation without destination directory.");
		}
	}
	
	@SpringBootTest(args = {"install", "-s", "SrcDir", "-d", "DestDir" })
	static class InstallCommandAllArgsTest {

		@MockitoBean AemInstaller aemInstaller;
		@Autowired ExitCodeGenerator exitCodeGenerator;

		@Test
		void testInstall_AllArgs() throws Exception {
			doNothing().when(aemInstaller).installAem(any(Path.class), any(Path.class));
			verify(aemInstaller, times(1)).installAem(eq(Path.of("DestDir")), eq(Path.of("SrcDir")));
			assertEquals(CommandLine.ExitCode.OK, exitCodeGenerator.getExitCode(), "Exit code should be 0 for successful installation with both source and destination directories.");
		}
	}

	@SpringBootTest(args = {"install", "-s" })
	static class InstallCommandMissingSrcDirTest {

		@MockitoBean AemInstaller aemInstaller;
		@Autowired ExitCodeGenerator exitCodeGenerator;

		@Test
		void testInstall_MissingSrcDir() throws Exception {
			verifyNoInteractions(aemInstaller);
			assertEquals(CommandLine.ExitCode.USAGE, exitCodeGenerator.getExitCode(), "Exit code should be 1 for missing source directory argument.");
		}
	}

	@SpringBootTest(args = {"install", "-d" })
	static class InstallCommandMissingDestDirTest {

		@MockitoBean AemInstaller aemInstaller;
		@Autowired ExitCodeGenerator exitCodeGenerator;

		@Test
		void testInstall_MissingDestDir() throws Exception {
			verifyNoInteractions(aemInstaller);
			assertEquals(CommandLine.ExitCode.USAGE, exitCodeGenerator.getExitCode(), "Exit code should be 1 for missing destination directory argument.");
		}
	}

	@SpringBootTest(args = {"install", "DestDir" })
	static class InstallCommandBadArgumentTest {

		@MockitoBean AemInstaller aemInstaller;
		@Autowired ExitCodeGenerator exitCodeGenerator;

		@Test
		void testInstall_BadArguments() throws Exception {
			verifyNoInteractions(aemInstaller);
			assertEquals(CommandLine.ExitCode.USAGE, exitCodeGenerator.getExitCode(), "Exit code should be 1 for bad arguments.");
		}
	}
}
