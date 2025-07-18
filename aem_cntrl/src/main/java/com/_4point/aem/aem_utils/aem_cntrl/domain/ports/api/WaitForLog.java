package com._4point.aem.aem_utils.aem_cntrl.domain.ports.api;

import java.nio.file.Path;
import java.time.Duration;

public interface WaitForLog {
	public static final FromOption DEFAULT_FROM_OPTION = FromOption.END;
	public static final Duration DEFAULT_DURATION = Duration.ofMinutes(10);

	enum FromOption {
		START, END;
	}
	
	sealed interface RegexArgument {
		enum StandardRegexOption {
			STARTUP, SHUTDOWN;
		}
		
		record RegexNonCustom(StandardRegexOption option) implements RegexArgument {};
		record RegexCustom(String regex) implements RegexArgument {};
		
		static RegexArgument startup() {
			return new RegexNonCustom(StandardRegexOption.STARTUP);
		}

		static RegexArgument shutdown() {
			return new RegexNonCustom(StandardRegexOption.SHUTDOWN);
		}

		static RegexArgument custom(String regex) {
			return new RegexCustom(regex);
		}
	};
	
	void waitForLog(RegexArgument regexArgument, Duration timeout, FromOption from, Path aemDir);
}
