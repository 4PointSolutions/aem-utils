package com._4point.aem.aem_utils.aem_cntrl.domain.ports.api;

import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Pattern;

public interface WaitForLog {
	public static final FromOption DEFAULT_FROM_OPTION = FromOption.END;
	public static final Duration DEFAULT_DURATION = Duration.ofMinutes(10);

	enum FromOption {
		START, END;
	}
	
	sealed interface RegexArgument {
		record RegexStartup() implements RegexArgument {};
		record RegexShutdown() implements RegexArgument {};
		record RegexCustom(Pattern regex) implements RegexArgument {};
		
		static RegexArgument startup() {
			return new RegexStartup();
		}

		static RegexArgument shutdown() {
			return new RegexShutdown();
		}

		static RegexArgument custom(Pattern regex) {
			return new RegexCustom(regex);
		}
	};
	
	void waitForLog(RegexArgument regexArgument, Duration timeout, FromOption from, Path aemDir);
}
