package com._4point.aem.aem_utils.aem_cntrl.commands;

import static org.mockito.Mockito.*;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com._4point.aem.aem_utils.aem_cntrl.domain.ports.api.AemInstaller;

class InstallCommandTests {
	@SpringBootTest(args = {"install"})
	static class InstallCommandNoArgsTest {
	
		@MockitoBean AemInstaller aemInstaller;
	
		@EnabledOnOs(OS.WINDOWS)
		@Test
		void testInstall_NoArgs_Windows() throws Exception {
			doNothing().when(aemInstaller).installAem(any(Path.class), any(Path.class));
			verify(aemInstaller, times(1)).installAem(eq(Path.of("\\Adobe")), eq(Path.of("")));
		}
	
		@EnabledOnOs(OS.LINUX)
		@Test
		void testInstall_NoArgs_Linux() throws Exception {
			doNothing().when(aemInstaller).installAem(any(Path.class), any(Path.class));
			verify(aemInstaller, times(1)).installAem(eq(Path.of("/opt", "adobe")), eq(Path.of("")));
		}
	}	

	@SpringBootTest(args = {"install", "-d", "DestDir" })
	static class InstallCommandDestArgTest {

		@MockitoBean AemInstaller aemInstaller;

		@Test
		void testInstall_NoArgs_Windows() throws Exception {
			doNothing().when(aemInstaller).installAem(any(Path.class), any(Path.class));
			verify(aemInstaller, times(1)).installAem(eq(Path.of("DestDir")), eq(Path.of("")));
		}
	}

	@SpringBootTest(args = {"install", "-s", "SrcDir" })
	static class InstallCommandSourceArgTest {

		@MockitoBean AemInstaller aemInstaller;

		@Test
		void testInstall_NoArgs_Windows() throws Exception {
			doNothing().when(aemInstaller).installAem(any(Path.class), any(Path.class));
			verify(aemInstaller, times(1)).installAem(eq(Path.of("\\Adobe")), eq(Path.of("SrcDir")));
		}
	}
	
	@SpringBootTest(args = {"install", "-s", "SrcDir", "-d", "DestDir" })
	static class InstallCommandAllArgsTest {

		@MockitoBean AemInstaller aemInstaller;

		@Test
		void testInstall_NoArgs_Windows() throws Exception {
			doNothing().when(aemInstaller).installAem(any(Path.class), any(Path.class));
			verify(aemInstaller, times(1)).installAem(eq(Path.of("DestDir")), eq(Path.of("SrcDir")));
		}
	}
}
