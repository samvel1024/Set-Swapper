package swapper;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class Swapper<T> {

	private final AtomicInteger waiterCount = new AtomicInteger(0);

	private class WaitingRequest {

		final String waitingThread;
		final Semaphore semaphore;
		final Set<T> remove;
		final long id;

		public WaitingRequest(String waitingThread, Semaphore semaphore, Collection<T> remove) {
			this.waitingThread = waitingThread;
			this.semaphore = semaphore;
			this.remove = new HashSet<>(remove);
			this.id = waiterCount.getAndIncrement();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			return id == ((WaitingRequest) o).id;
		}

		@Override
		public int hashCode() {
			return (int) id;
		}
	}


	final boolean debug;
	Set<WaitingRequest> requests = new HashSet<>();
	Set<T> set = new HashSet<>();
	Semaphore mutex = new Semaphore(1, true);

	public Swapper() {
		this(false);
	}

	public Swapper(boolean debug) {
		this.debug = debug;
	}



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
		Collection<WaitingRequest> tbr = new LinkedList<>();
		for (WaitingRequest r : requests) {
			if (set.containsAll(r.remove)) {
				remove(r.remove);
				tbr.add(r);
				Log.debug("Releasing %s", r.waitingThread);
				r.semaphore.release();
			}
		}
		requests.removeAll(tbr);
	}

	public void swap(Collection<T> rem, Collection<T> add) throws InterruptedException {
		mutex.acquire();
		if (!set.containsAll(rem)) {
			Log.debug("Set does not contain %s", rem);

			Semaphore s = new Semaphore(0);
			requests.add(new WaitingRequest(Thread.currentThread().getName(), s, rem));
			mutex.release();
			Log.debug("Waiting for %s", rem);
			s.acquire();


			Log.debug("Woke up from waiting");
			mutex.acquire();
			releaseWaiting(add);
			mutex.release();

		} else {
			remove(rem);
			releaseWaiting(add);
			mutex.release();
		}

	}

	/**
	 * Not thread safe
	 */
	public void clear(){
		this.set.clear();
		this.requests.clear();
	}

}
