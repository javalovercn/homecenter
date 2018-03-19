package hc.core;

public class SenderSlowCounter {
	private int counter;

	public final int addOne() {
		synchronized (this) {
			return ++counter;
		}
	}

	public final void minusOne() {
		synchronized (this) {
			counter--;
		}
	}
}
