package hc.server.nio;

import hc.core.util.ExceptionReporter;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.EmptyStackException;
import java.util.Stack;

public class SelectorFactory {
	private final static Stack<Selector> selectors = new Stack<Selector>();

	public final static Selector getSelector() {
		synchronized (selectors) {
			Selector s = null;
			try {
				if (selectors.size() != 0)
					s = selectors.pop();
			} catch (final EmptyStackException ex) {
			}

			if (s == null) {
				try {
					s = Selector.open();
				} catch (final IOException e) {
					ExceptionReporter.printStackTrace(e);
				}
			}
			return s;
		}
	}

	public final static void returnSelector(final Selector s) {
		synchronized (selectors) {
			selectors.push(s);
		}
	}

}