package hc.server.nio;

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
			} catch (EmptyStackException ex) {
			}

			if(s == null){
				try {
					s = Selector.open();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return s;
		}
	}

	public final static void returnSelector(Selector s) {
		synchronized (selectors) {
			selectors.push(s);
		}
	}

}