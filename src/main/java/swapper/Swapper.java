package swapper;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Swapper<T> {

	private final Collection<WaitingThread> queue = new LinkedList<>();
	private final Collection<T> set = new HashSet<>();
	private final Lock mutex = new ReentrantLock();

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
				r.processed = true;
				r.condition.signal();
				it.remove();
			}
		}
	}

	public void swap(Collection<T> rem, Collection<T> add) throws InterruptedException {
		mutex.lock();
		if (!set.containsAll(rem)) {
			Log.debug("Set does not contain %s", rem);

			Condition cond = mutex.newCondition();
			WaitingThread request = new WaitingThread(Thread.currentThread().getName(), cond, rem);
			queue.add(request);
			Log.debug("Waiting for %s", rem);
			while (!request.processed) {
				cond.await();
			}
			Log.debug("Woke up from waiting for to be removed elements");
			try {
				releaseWaiting(add);
			} finally {
				mutex.unlock();
			}

		} else {
			try {
				remove(rem);
				releaseWaiting(add);
			} finally {
				mutex.unlock();
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
		final Condition condition;
		final Collection<T> remove;
		/**
		 * processed is true only after the requested
		 * items in remove are removed from the set
		 */
		volatile boolean processed = false;

		public WaitingThread(String waitingThread, Condition condition, Collection<T> remove) {
			this.waitingThread = waitingThread;
			this.condition = condition;
			this.remove = new HashSet<>(remove);
		}

		void finishedProcessing() {
			this.processed = true;
		}
	}

}
