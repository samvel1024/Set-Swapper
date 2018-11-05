package swapper;

import java.util.Collection;

import static java.util.Arrays.asList;

public class Test implements Runnable {

	public static void main(String[] args) {
		new Test().run();
	}


	int tCount = 0;

	void spawn(Runnable r) {
		new Thread(r, "T" + tCount++).start();
	}


	private <T> void swp(Swapper<T> swp, Collection<T> a, Collection<T> b) {
		try {
			swp.swap(a, b);
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public void run() {
		Swapper<Integer> swp = new Swapper<>();
		spawn(() -> swp(swp, asList(1), asList(2)));
		spawn(() -> swp(swp, asList(2), asList(3)));
		spawn(() -> swp(swp, asList(3), asList(4)));
		spawn(() -> swp(swp, asList(4), asList(5)));
		spawn(() -> swp(swp, asList(5), asList(6)));
		spawn(() -> swp(swp, asList(), asList(1)));

	}
}
