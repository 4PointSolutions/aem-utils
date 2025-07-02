package com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface Tailer extends AutoCloseable {
	
	public Stream<String> stream();
	
	public interface TailerFactory {
		/**
		 * Returns a {@link CommonsIoTailerTailer} that will stream lines from the specified
		 * file, starting from the beginning of the file.
		 * 
		 * @param path The path to the file to tail.
		 * @return A {@link CommonsIoTailerTailer} for streaming lines from the file.
		 */
		Tailer fromBeginning(Path path);
		
		/**
		 * Returns a {@link CommonsIoTailerTailer} that will stream lines from the specified
		 * file, starting from the end of the file.
		 * 
		 * @param path The path to the file to tail.
		 * @return A {@link CommonsIoTailerTailer} for streaming lines from the file.
		 */
		Tailer fromEnd(Path path);
	}
}
