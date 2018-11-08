package swapper;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class SwpSemaphore {

	private static final Collection<Integer> singleton = Collections.singleton(1);
	private static final Collection<Integer> emptySet = Collections.emptySet();

	private final Swapper<Integer> swp = new Swapper<>();
	private boolean open;

	public SwpSemaphore(boolean open) {
		this.open = open;
		if (open) {
			try {
				swp.swap(emptySet, singleton);
			} catch (InterruptedException e) {
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
		}
	}

	public void acquire() throws InterruptedException {
		swp.swap(singleton, emptySet);
		open = false;
	}

	public void release() throws InterruptedException {
		if (open)
			throw new IllegalStateException();
		swp.swap(emptySet, singleton);
		open = true;
	}

	/******* TEST ********/

	private static int testVal = 0;

	public static void main(String[] args) throws InterruptedException {
		SwpSemaphore sem = new SwpSemaphore(true);
		final int tCount = 20;
		final int perThread = 10000;
		Callable<?> t = () -> {
			for (int i = 0; i < perThread; ++i) {
				try {
					sem.acquire();
					++testVal;
				} catch (InterruptedException e) {
					e.printStackTrace();
					Thread.currentThread().interrupt();
				} finally {
					sem.release();
				}
			}
			return null;
		};

		ExecutorService threadPool = Executors.newFixedThreadPool(tCount);
		threadPool.invokeAll(IntStream.range(0, tCount).mapToObj(i -> t).collect(Collectors.toList()));
		assert tCount * perThread == testVal;
		threadPool.shutdown();
	}

}
