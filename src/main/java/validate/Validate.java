package validate;

import swapper.Swapper;

import java.util.Collection;
import java.util.Collections;

public class Validate {

	public static void main(String[] args) {
		Swapper<Integer> swapper = new Swapper<>();
		try {
			Collection<Integer> empty = Collections.emptySet();
			Collection<Integer> singletonOne = Collections.singleton(1);
			swapper.swap(empty, singletonOne);
			swapper.swap(singletonOne, empty);
			System.out.println("OK");
		} catch (InterruptedException e) {
			System.out.println("ERROR");
			System.exit(1);
		}
	}

}