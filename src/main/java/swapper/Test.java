package swapper;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;

public class Test {


	public static void main(String[] args) {
		Runnable[] all = new Runnable[]{
			new ReadWriteTest()
		};
		for (Runnable r : all) {
			System.out.printf("********************* Running test %s ************************\n", r.getClass().getCanonicalName());
			r.run();
		}
	}


	public static class SmallSwapperTest implements Runnable {

		int tCount = 0;
		CountDownLatch latch = new CountDownLatch(5);



		Thread spawn(Runnable r) {
			return new Thread(() -> {
				r.run();
				latch.countDown();
			}, "T" + tCount++);
		}

		private <T> void swp(Swapper<T> swp, Collection<T> a, Collection<T> b) {
			try {
				swp.swap(a, b);
			} catch (InterruptedException e) {
				e.printStackTrace(System.out);
				Thread.currentThread().interrupt();
			}
		}

		@Override
		public void run() {
			Swapper<Integer> swp = new Swapper<>();
			List<Thread> threads = Arrays.asList(spawn(() -> swp(swp, singletonList(1), singletonList(2))),
				spawn(() -> swp(swp, singletonList(2), singletonList(3))),
				spawn(() -> swp(swp, singletonList(3), singletonList(4))),
				spawn(() -> swp(swp, singletonList(4), singletonList(5))),
				spawn(() -> swp(swp, singletonList(5), singletonList(6))),
				spawn(() -> swp(swp, emptySet(), singletonList(1))));
			for (Thread thread : threads) {
				thread.start();
			}
			try {
				latch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}


		}
	}

	public static class ConsumeProduceTest implements Runnable {
		@Override
		public void run() {
			ConsumeProduce<Integer> p = new ConsumeProduce<>(
				Integer.class,
				(a) -> Log.debug("Consuming message %s", a),
				new Supplier<Integer>() {
					int a = 0;

					@Override
					public Integer get() {
						return a++;
					}
				},
				10,
				10,
				10
			);
			try {
				p.start();
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
			p.stop();
		}
	}

	public static class SwpSemaphoreTest implements Runnable {

		private static int testVal = 0;

		@Override
		public void run() {
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
			try {
				threadPool.invokeAll(IntStream.range(0, tCount).mapToObj(i -> t).collect(Collectors.toList()));
			} catch (InterruptedException e) {
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
			assert tCount * perThread == testVal;
			threadPool.shutdown();
		}
	}


	public static class ReadWriteTest implements Runnable {

		private volatile boolean running = true;

		private static class TestClass {
			int val = 0;
		}

		@Override
		public void run() {

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
						readWrite.write(obj -> {
							obj.val++;
							Log.debug("Set value %d", obj.val);
						});
					} catch (InterruptedException e) {
						e.printStackTrace();
						Thread.currentThread().interrupt();
					}
				}
			}, "Writer");

			writer.start();


			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
			running = false;
		}
	}


}
