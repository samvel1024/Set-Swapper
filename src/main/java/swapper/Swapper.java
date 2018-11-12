package swapper;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

public class Swapper<T> {

	private final Collection<WaitingThread> queue = new LinkedList<>();
	private final Collection<T> set = new HashSet<>();
	private final Semaphore mutex = new Semaphore(1, true);

	private void remove(Collection<T> c) {
		set.removeAll(c);
		Log.debug("Removed elements %s", c);
	}

	private void add(Collection<T> add) {
		set.addAll(add);
		Log.debug("Added elements %s", add);
	}

	private void releaseWaiting(Collection<T> add) {
		Log.debug("Releasing waiting threads");
		add(add);
		for (Iterator<WaitingThread> it = queue.iterator(); it.hasNext(); ) {
			WaitingThread r = it.next();
			if (set.containsAll(r.remove)) {
				remove(r.remove);
				Log.debug("Releasing %s", r.waitingThread);
				r.semaphore.release();
				it.remove();
			}
		}
	}

	public void swap(Collection<T> rem, Collection<T> add) throws InterruptedException {
		mutex.acquire();
		if (!set.containsAll(rem)) {
			Log.debug("Set does not contain %s", rem);

			Semaphore s = new Semaphore(0);
			queue.add(new WaitingThread(Thread.currentThread().getName(), s, rem));
			mutex.release();
			Log.debug("Waiting for %s", rem);
			s.acquire();
			Log.debug("Woke up from waiting for to be removed elements");
			mutex.acquire();
			try {
				releaseWaiting(add);
			} finally {
				mutex.release();
			}

		} else {
			try {
				remove(rem);
				releaseWaiting(add);
			} finally {
				mutex.release();
			}
		}

	}

	/**
	 * Not thread safe
	 */
	public void clear() {
		this.set.clear();
		this.queue.clear();
	}

	private class WaitingThread {

		final String waitingThread;
		final Semaphore semaphore;
		final Collection<T> remove;

		public WaitingThread(String waitingThread, Semaphore semaphore, Collection<T> remove) {
			this.waitingThread = waitingThread;
			this.semaphore = semaphore;
			this.remove = new HashSet<>(remove);
		}

	}

}
