package swapper;

public class Log {


	private static boolean debug = false;

	static {
		String prop = System.getProperty("debug.enabled");
		if ("true".equals(prop))
			debug = true;
	}

	private Log() {
	}

	public static void debug(String format, Object... args) {
		if (!debug) return;
		String prefix = String.format("[Thread: %s]\t", Thread.currentThread().getName());
		System.out.println(String.format(prefix + format, args));
	}


}
