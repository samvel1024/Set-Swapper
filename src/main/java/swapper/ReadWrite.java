package swapper;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static java.util.Collections.*;

public class ReadWrite<T> {

	private final AtomicInteger readerIdSequence = new AtomicInteger(1);
	private final ThreadLocal<Integer> readerId = new ThreadLocal<>();
	private final T guardedObject;
	private final Swapper<Integer> swp = new Swapper<>();

	private int maxReader;
	private final Collection<Integer> all;

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
		cons.accept(guardedObject);
		swp.swap(emptyList(), all);
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
		cons.accept(guardedObject);
		swp.swap(emptySet(), singleton(mId));
	}


	private static class TestClass {
		int val = 0;

	}

	private static volatile boolean running = true;


	public static void main(String[] args) throws InterruptedException {

		ReadWrite<TestClass> readWrite = new ReadWrite<>(new TestClass(), 3);

		Runnable reader = () -> {
			while (running) {
				try {
					readWrite.read(obj -> Log.debug("Reading value %d", obj.val));
				} catch (InterruptedException e) {
					e.printStackTrace();
					Thread.currentThread().interrupt();
				}
			}
		};

		for (int i = 0; i < readWrite.getMaxReader(); ++i) {
			Thread thread = new Thread(reader, "Reader-" + i);
			thread.start();
		}

		Thread writer = new Thread(() -> {
			while (running) {
				try {
					readWrite.mutate(obj -> {
						obj.val++;
						Log.debug("Set value %d", obj.val);
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
							Thread.currentThread().interrupt();
						}
					});
				} catch (InterruptedException e) {
					e.printStackTrace();
					Thread.currentThread().interrupt();
				}
			}
		}, "Writer");

		writer.start();


		Thread.sleep(5000);
		running = false;


	}

}
