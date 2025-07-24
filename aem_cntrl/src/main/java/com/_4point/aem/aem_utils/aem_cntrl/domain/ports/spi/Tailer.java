package com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface Tailer extends AutoCloseable {
	
	public Stream<String> stream();
	
	@FunctionalInterface
	public interface TailerFactory {
		enum FromOption {
			BEGINNING, END;
		};
		
		Tailer from(Path path, FromOption fromOption);
		
		/**
		 * Returns a {@link CommonsIoTailerTailer} that will stream lines from the specified
		 * file, starting from the beginning of the file.
		 * 
		 * @param path The path to the file to tail.
		 * @return A {@link CommonsIoTailerTailer} for streaming lines from the file.
		 */
		default Tailer fromBeginning(Path path) { return from(path, FromOption.BEGINNING); };
		
		/**
		 * Returns a {@link CommonsIoTailerTailer} that will stream lines from the specified
		 * file, starting from the end of the file.
		 * 
		 * @param path The path to the file to tail.
		 * @return A {@link CommonsIoTailerTailer} for streaming lines from the file.
		 */
		default Tailer fromEnd(Path path) { return from(path, FromOption.END); };
	}
}
