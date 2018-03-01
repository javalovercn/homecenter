package hc.server.ui.design.code;

import hc.server.ui.design.hpj.HPNode;

import java.util.Vector;

public class NodeLibClassesAndResMap {
	private final Vector<HPNode> vectorKey = new Vector<HPNode>();
	private final Vector<String[]> vectorValue = new Vector<String[]>();

	public final String[] searchLibClassesAndRes(final String key) {
		final int size = vectorKey.size();
		for (int i = 0; i < size; i++) {
			if (vectorKey.get(i).name.equals(key)) {
				return vectorValue.get(i);
			}
		}
		return null;
	}

	public final void put(final HPNode node, final String[] res) {
		vectorKey.add(node);
		vectorValue.add(res);
	}

	public final void remove(final HPNode node) {
		final int size = vectorKey.size();
		for (int i = 0; i < size; i++) {
			if (vectorKey.get(i) == node) {
				vectorKey.remove(i);
				vectorValue.remove(i);
				return;
			}
		}
	}

	public final String[] getNodeNames() {
		final int size = vectorKey.size();
		final String[] keyStrs = new String[size];

		for (int i = 0; i < size; i++) {
			keyStrs[i] = vectorKey.get(i).name;
		}

		return keyStrs;
	}
}
