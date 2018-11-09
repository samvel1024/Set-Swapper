package swapper;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

/**
 * Consumer Producer problem for given buffer size of N
 * The solution uses a swapper which is a subset of {0, 1, ..., 2 * N - 1}
 * If a belongs to swp and a < N then buffer[a] is ready for being read by a consumer
 * If a belongs to swp and a >= N then buffer[a % N] is ready for being written by a producer
 * Initially swp is initialized as {N, N + 1, ... , 2 * N - 1}
 * Atomic integer instances are used for assigning indices to each consumer and producer iteration
 * <p>
 * Example usage {@link swapper.Test.ConsumeProduceTest)}
 */
public class ConsumeProduce<T> {

	private volatile boolean running = true;

	private final int consumers;
	private final int producers;

	private final Swapper<Integer> swp = new Swapper<>();
	private final Consumer<T> consumer;
	private final Supplier<T> supplier;
	private final T[] buffer;

	private final AtomicInteger write = new AtomicInteger(0);
	private final AtomicInteger read = new AtomicInteger(0);

	private class ProducerThread implements Runnable {


		@Override
		public void run() {
			while (running) {
				try {
					int writeIdx = write.getAndUpdate(ConsumeProduce.this::nextIndex);
					Log.debug("Got index %d", writeIdx);

					swp.swap(singleton(buffer.length + writeIdx), emptySet());

					if (buffer[writeIdx] != null) {
						throw new IllegalStateException();
					}

					try {
						buffer[writeIdx] = supplier.get();
						Log.debug("Writing at position %d %s", writeIdx, buffer[writeIdx]);
					} finally {
						swp.swap(emptySet(), singleton(writeIdx));
					}
				} catch (InterruptedException e) {
					e.printStackTrace(System.out);
					Thread.currentThread().interrupt();
				}
			}

		}
	}

	private class ConsumerThread implements Runnable {

		@Override
		public void run() {
			while (running) {
				try {
					int readIdx = read.getAndUpdate(ConsumeProduce.this::nextIndex);

					Log.debug("Got index %d", readIdx);
					swp.swap(singleton(readIdx), emptySet());

					if (buffer[readIdx] == null) {
						throw new IllegalStateException();
					}

					try {
						Log.debug("Reading from position %d %s", readIdx, buffer[readIdx]);
						consumer.accept(buffer[readIdx]);
						buffer[readIdx] = null;
					} finally {
						swp.swap(emptySet(), singleton(buffer.length + readIdx));
					}

				} catch (InterruptedException e) {
					e.printStackTrace(System.out);
					Thread.currentThread().interrupt();
				}
			}

		}
	}


	@SuppressWarnings("unchecked")
	public ConsumeProduce(
		Class<T> type,
		Consumer<T> cons,
		Supplier<T> supplier,
		int consumers,
		int producers,
		int size
	) {
		this.consumers = consumers;
		this.producers = producers;
		this.buffer = (T[]) Array.newInstance(type, size);
		this.supplier = supplier;
		this.consumer = cons;
	}

	public void start() throws InterruptedException {
		//Initialize swapper with { N, N + 1, ... , 2 * N - 1}
		this.read.set(0);
		this.write.set(0);
		Arrays.fill(buffer, null);
		swp.clear();
		swp.swap(emptySet(), IntStream.range(buffer.length, 2 * buffer.length).boxed().collect(Collectors.toList()));
		running = true;
		IntStream.range(0, producers + consumers)
			.mapToObj(i -> i < producers ?
				new Thread(new ProducerThread(), "Producer-" + i) :
				new Thread(new ConsumerThread(), "Consumer-" + (i - producers)))
			.forEach(Thread::start);
	}

	public void stop() {
		this.running = false;
	}

	private int nextIndex(int curr) {
		return (curr + 1) % buffer.length;
	}


}
