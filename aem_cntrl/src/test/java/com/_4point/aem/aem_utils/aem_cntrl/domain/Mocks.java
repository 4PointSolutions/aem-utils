package com._4point.aem.aem_utils.aem_cntrl.domain;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
		
		final TailerFactory tailerFactoryMock;
		final Tailer tailerMock;

		public TailerMocker() {
			this(mock(TailerFactory.class));
		}

		public TailerMocker(TailerFactory tailerFactoryMock) {
			this.tailerFactoryMock = tailerFactoryMock;
			this.tailerMock = mock(Tailer.class);
			programMocks(tailerFactoryMock, tailerMock);
		}

		static void programMocks(TailerFactory tailerFactoryMock, Tailer tailerMock) {
			when(tailerFactoryMock.from(any(), any())).thenReturn(tailerMock);
			when(tailerMock.stream()).thenAnswer(i->MOCK_AEM_LOG.lines());
		}

		public TailerFactory tailerFactoryMock() {
			return tailerFactoryMock;
		}

		public Tailer TgetTailerMock() {
			return tailerMock;
		}
	}
}
