package hc.server.nio;

import hc.core.util.LinkedSet;
import hc.core.util.Stack;

import java.nio.ByteBuffer;

public abstract class ByteBufferCacher {
	private final Stack set;

	public ByteBufferCacher() {
		set = new Stack();
	}

	public ByteBuffer getFree() {
		// synchronized (set) {
		if (set.isEmpty()) {
			return buildOne();
		} else {
			return (ByteBuffer) set.pop();
		}
		// }
	}

	public abstract ByteBuffer buildOne();

	public void cycle(ByteBuffer dp) {
		// synchronized (set) {
		set.push(dp);
		// }
	}

	public void cycleSet(final LinkedSet linkedSet) {
		ByteBuffer temp;
		while ((temp = (ByteBuffer) linkedSet.getFirst()) != null) {
			temp.clear();
			cycle(temp);
		}
	}
}
