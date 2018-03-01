package hc.util;

import hc.core.util.Stack;

public class StringBuilderCacher {
	private static final Stack free = new Stack(8);

	public static StringBuilder getFree() {
		synchronized (free) {
			if (free.size() == 0) {
				return new StringBuilder(1024 * 1024);
			} else {
				return (StringBuilder) free.pop();
			}
		}
	}

	public static void cycle(final StringBuilder sb) {
		if (sb == null) {
			return;
		}

		sb.setLength(0);

		synchronized (free) {
			free.push(sb);
		}
	}
}
