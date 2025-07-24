package com._4point.aem.aem_utils.aem_cntrl.adapters.spi;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterators.AbstractSpliterator;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonsIoTailerTailer implements com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer {
	private static final Logger log = LoggerFactory.getLogger(CommonsIoTailerTailer.class);
	private static final Duration TRAILER_DELAY = Duration.ofMillis(100);
	private static final int NUM_EXIST_RETRIES = 60;
	private final Path path;
	private final Tailer tailer;
	private final BlockingDeque<String> queue = new LinkedBlockingDeque<>();
	private final AtomicReference<Exception> exceptionThrown = new AtomicReference<>();
	
	private CommonsIoTailerTailer(Path path, boolean fromEnd) {
		this.path = path;
		this.tailer = Tailer.builder()
				   .setPath(path)
				   .setCharset(StandardCharsets.UTF_8)
				   .setDelayDuration(TRAILER_DELAY)
//				   .setExecutorService(Executors.newSingleThreadExecutor(Builder::newDaemonThread))
				   .setReOpen(false)
				   .setStartThread(true)
//				   .setTailable(tailable)
				   .setTailerListener(new MyListener())
				   .setTailFromEnd(fromEnd)
				   .get();
	}

	@Override
	public Stream<String> stream() {
		return StreamSupport.stream(new MySpliterator(), false);
	}
	
	private static void waitTillItExists(Path path) {
		Duration waitPeriod = Duration.ofSeconds(1);
		for (int i = 0; i < NUM_EXIST_RETRIES; i++) {
			if (Files.exists(path)) {
				return;
			}
			try {
				log.atTrace().addArgument(path.toString()).log("Waiting for log file ({}) to exist.");
				Thread.sleep(waitPeriod);
			} catch (InterruptedException e) {
				log.atWarn().setCause(e).log("Unexpected interruption during waiting for file to exist.");
			}
		}
	}
	
	@Override
	public void close() throws Exception {
//		System.out.println("Closing Tailer");
		tailer.close();
		Thread.sleep(TRAILER_DELAY.multipliedBy(2));	// Give the Tailer time to shut down and close file.
	}
	
	private class MySpliterator extends AbstractSpliterator<String> {
		
		protected MySpliterator() {
			super(Long.MAX_VALUE, 0);	// Estimated size is Long.MAX_VALUE, and no characteristics.
		}

		@Override
		public boolean tryAdvance(Consumer<? super String> action) {
			if (exceptionThrown.get() != null) {
				return false;
			}
			try {
				String line = queue.take();
				log.atTrace().log(()->"Pushing line '%s' to stream.".formatted(line));
				action.accept(line);
				return true;
			} catch (InterruptedException e) {
				throw new NoSuchElementException("Interrupted while waiting for next line to be read.", e);
			}
		}
		
	}
	
	private class MyListener implements TailerListener {

		@Override
		public void fileNotFound() {
			String msg = "Unable to open file for tailing (" + path.toAbsolutePath().toString() + ")";
			log.atError()
			   .log(msg);
			exceptionThrown.set(new FileNotFoundException());
		}

		@Override
		public void fileRotated() {
			// Do nothing, we don't care if it has been rotated
		}

		@Override
		public void handle(Exception ex) {
			log.atError()
			   .addArgument(()->Thread.currentThread().getName())
			   .addArgument(()->ex.getClass().getName())
			   .addArgument(()->Objects.requireNonNull(ex.getMessage(), "~No Message~"))
			   .setCause(ex)
			   .log("[{}] Caught exception '{}' rethrowing [{}].");
			exceptionThrown.set(ex);
		}

		@Override
		public void handle(String line) {
//			System.out.println("[%s] Adding line '%s' to queue.".formatted(Thread.currentThread().getName(), line));
			queue.add(line);
		}

		@Override
		public void init(Tailer tailer) {
			// Nothing to be done here
		}
	}
	
	public static class TailerFactory implements com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer.TailerFactory {

		@Override
		public com._4point.aem.aem_utils.aem_cntrl.domain.ports.spi.Tailer from(Path path, FromOption fromOption) {
			waitTillItExists(path);
			return new CommonsIoTailerTailer(path, toBoolean(fromOption));
		}

		private boolean toBoolean(FromOption fromOption) {
			return switch (fromOption) {
                case BEGINNING -> false;
                case END -> true;
            };
		}
	}
}
