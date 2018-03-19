package hc.core;

import hc.core.util.Stack;

public class Ack {
	int waitStart, waitEnd;
	Ack next;

	final private static Stack freeStack = new Stack();
	private static short stackSize = 0;

	public final static Ack getFree() {
		Ack packate = null;
		synchronized (freeStack) {
			if (stackSize == 0) {
				// LogManager.log("------MEM ALLOCATE [Message]------");
				packate = new Ack();
			} else {
				packate = (Ack) freeStack.pop();
				stackSize--;
			}
			return packate;
		}
	}

	public final static void cycle(Ack dp) {
		synchronized (freeStack) {
			freeStack.push(dp);
			stackSize++;
		}
	}

}
