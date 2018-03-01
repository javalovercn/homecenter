package hc.util;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.LinkedList;

public class SelectorCacher {
	private final LinkedList<Selector> free;

	public SelectorCacher() {
		free = new LinkedList<Selector>();
	}

	public Selector getFree() {
		synchronized (free) {
			if (free.size() == 0) {
				try {
					return Selector.open();
				} catch (IOException e) {
					return null;
				}
			} else {
				return free.removeFirst();
			}
		}
	}

	public void cycle(Selector dp) {
		synchronized (free) {
			free.addLast(dp);
		}
	}
}
