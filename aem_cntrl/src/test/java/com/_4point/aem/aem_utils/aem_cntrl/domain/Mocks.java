package com._4point.aem.aem_utils.aem_cntrl.domain;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.stream.Stream;

import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer;
import com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer.TailerFactory;

public class Mocks {
	
	public static class TailerMocker {
		// This mock AEN log has all the strings that are ever searched for
		static final String MOCK_AEM_LOG = """
				18.06.2025 09:26:04.989 *INFO* [FelixDispatchQueue] com.adobe.granite.workflow.core.launcher.WorkflowLauncherListener StartupListener.startupFinished called
				18.06.2025 09:28:04.288 *INFO* [FelixFrameworkWiring] org.apache.sling.installer.core.impl.OsgiInstallerImpl Apache Sling OSGi Installer Service stopped.
				18.06.2025 09:32:42.198 *INFO* [Thread-2944] com.adobe.granite.installer.Updater Content Package AEM-6.5-Service-Pack-23 Installed successfully
				18.06.2025 09:38:01.557 *INFO* [OsgiInstallerImpl] org.apache.sling.audit.osgi.installer Installed BMC XMLFormService of type BMC_NATIVE
				""";
		
		private final TailerFactory tailerFactoryMock;
		private final Tailer tailerMock;

		public TailerMocker() {
			this(mock(TailerFactory.class));
		}

		public TailerMocker(TailerFactory tailerFactoryMock) {
			this.tailerFactoryMock = tailerFactoryMock;
			this.tailerMock = mock(Tailer.class);
		}

		void programMocksToEmulateAem() {
			programMocksToEmulateAem(tailerFactoryMock, tailerMock);
		}

		void programMocksToEmulateAemAfterWait(Duration wait) {
			when(tailerFactoryMock.from(any(), any())).thenReturn(tailerMock);
			when(tailerMock.stream()).thenAnswer(i->emulateAemLog(wait));			
		}

		private static Stream<String> emulateAemLog(Duration wait) {
			// TODO: Implement a delay if needed
			return MOCK_AEM_LOG.lines().filter(s->{
				try {
					Thread.sleep(wait);
				} catch (InterruptedException e) {
				}
				return true;
			});
		}

		static void programMocksToEmulateAem(TailerFactory tailerFactoryMock, Tailer tailerMock) {
			when(tailerFactoryMock.from(any(), any())).thenReturn(tailerMock);
			when(tailerMock.stream()).thenAnswer(i->emulateAemLog(Duration.ZERO));
		}

		
		public TailerFactory tailerFactoryMock() {
			return tailerFactoryMock;
		}

		public Tailer TgetTailerMock() {
			return tailerMock;
		}
	}
}
