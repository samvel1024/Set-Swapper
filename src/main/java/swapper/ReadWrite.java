package swapper;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static java.util.Collections.*;

/**
 * Readers and writers problem for given maximum amount of N readers
 * Swappper initially equals to {0, 1, 2 ... N}
 * Reader owns elements 0 and i-th reader owns element i
 * Writer thread acquires all elements from swapper to prevent anyone accessing the guarded object
 * i-th reader thread acquires {0, i} to prevent the writer from writing
 * ThreadLocal is used to store the reader id per thread
 *
 * Example usage {@link swapper.Test.ReadWriteTest)}
 *
 */
public class ReadWrite<T> {

	private final AtomicInteger readerIdSequence = new AtomicInteger(1);
	private final ThreadLocal<Integer> readerId = new ThreadLocal<>();
	private final T guardedObject;
	private final Swapper<Integer> swp = new Swapper<>();
	private final Collection<Integer> all;
	private final int maxReader;

	public ReadWrite(T guardedObject, int maxReader) {
		this.guardedObject = guardedObject;
		this.maxReader = maxReader;
		this.all = IntStream.range(0, maxReader + 1).boxed().collect(Collectors.toList());
		try {
			this.swp.swap(emptySet(), all);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			e.printStackTrace();
		}
	}


	public int getMaxReader() {
		return maxReader;
	}

	public void mutate(Consumer<T> cons) throws InterruptedException {
		swp.swap(all, emptySet());
		Log.debug("Mutating state");
		try {
			cons.accept(guardedObject);
		} finally {
			swp.swap(emptyList(), all);
		}
	}

	public void read(Consumer<T> cons) throws InterruptedException {
		if (this.readerId.get() == null) {
			this.readerId.set(readerIdSequence.getAndIncrement());
			if (this.readerId.get() > maxReader)
				throw new IllegalStateException();
		}
		int mId = this.readerId.get();
		swp.swap(asList(0, mId), singleton(0));
		Log.debug("Reading from reader %d", mId);
		try {
			cons.accept(guardedObject);
		} finally {
			swp.swap(emptySet(), singleton(mId));
		}
	}


}
