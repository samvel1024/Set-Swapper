package swapper;

import java.util.Collection;
import java.util.Collections;


/**
 * Example usage {@link swapper.Test.SwpSemaphoreTest)}
 */
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



}
